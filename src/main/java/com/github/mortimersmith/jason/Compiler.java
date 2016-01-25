package com.github.mortimersmith.jason;

import com.github.mortimersmith.utils.Utils.ThrowingConsumer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.lang.model.element.Modifier;

public class Compiler
{
    public static void main(String[] args) throws Error
    {
        if (args.length != 2) throw Error.because("invalid arguments");
        compile(Paths.get(args[0]), Paths.get(args[1]));
    }

    public static void compile(Path specs, Path target) throws Error
    {
        JsonParser parser = new JsonParser();
        for (File file : specs.toFile().listFiles()) {
            if (!file.isFile()) continue;
            if (!file.getName().endsWith(".json")) continue;
            JsonObject json = Error.get(() -> parser.parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)).getAsJsonObject());
            compileJson(json, target);
        }
    }

    public static void compileJson(JsonObject spec, Path target) throws Error
    {
        Package pkg = new Package();
        pkg.name = spec.get("package").getAsString();
        for (Instances i : compileNamespaces(spec, pkg))
            pkg.namespaces.put(i.name, i);
        Emit.pkg(pkg, target);
    }

    private static Iterable<Instances> compileNamespaces(JsonObject spec, Package pkg) throws Error
    {
        List<Instances> iss = new LinkedList<>();
        for (JsonElement nsElement : spec.get("namespaces").getAsJsonArray()) {
            JsonObject nsObject = nsElement.getAsJsonObject();
            Instances is = new Instances();
            is.pkg = pkg;
            is.name = nsObject.get("name").getAsString();
            if (nsObject.has("types")) {
                for (JsonElement typeElement : nsObject.get("types").getAsJsonArray()) {
                    JsonObject typeObject = typeElement.getAsJsonObject();
                    Instance i = new Instance();
                    i.instances = is;
                    i.name = typeObject.get("name").getAsString();
                    if (typeObject.has("implements")) {
                        for (JsonElement ifaceElement : typeObject.get("implements").getAsJsonArray())
                            i.ifaces.add(ifaceElement.getAsString());
                    }
                    if (typeObject.has("fields")) {
                        for (JsonElement fieldElement : typeObject.get("fields").getAsJsonArray()) {
                            JsonObject fieldObject = fieldElement.getAsJsonObject();
                            Member member = new Member();
                            member.name = fieldObject.get("name").getAsString();
                            resolveType(fieldObject, member);
                            i.members.add(member);
                        }
                    }
                    is.types.put(i.name, i);
                }
            }
            if (nsObject.has("namespaces")) {
                for (Instances child : compileNamespaces(nsObject, pkg)) {
                    child.parent = is;
                    is.namespaces.put(child.name, child);
                }
            }
            iss.add(is);
        }
        return iss;
    }

    private static void resolveType(JsonObject json, Member m) throws Error
    {
        String type = json.get("type").getAsString();
        m.type = Symbol.of(type);
        if ("optional".equals(type) || "list".equals(type)) {
            String of = json.get("of").getAsString();
            m.templates = new Symbol[] { Symbol.of(of) };
        } else if ("map".equals(type)) {
            String key = json.get("key").getAsString();
            String value = json.get("value").getAsString();
            m.templates = new Symbol[] { Symbol.of(key), Symbol.of(value) };
        } else {
            m.templates = new Symbol[] {};
        }
    }

    private static <T, E extends Exception> void emitEach(T[] ts, String sep, StringBuilder out, ThrowingConsumer<T, E> c) throws E
    {
        boolean first = true;
        for (T t : ts) {
            if (first) first = false; else out.append(sep);
            c.accept(t);
        }
    }

    private static <T, E extends Exception> void emitEach(Iterable<T> ts, String sep, StringBuilder out, ThrowingConsumer<T, E> c) throws E
    {
        boolean first = true;
        for (T t : ts) {
            if (first) first = false; else out.append(sep);
            c.accept(t);
        }
    }

    private interface Emit
    {
        static void pkg(Package pkg, Path target) throws Error
        {
            for (Instances is : pkg.namespaces.values()) {
                TypeSpec type = instances(is);
                JavaFile file = JavaFile.builder(is.pkg.name, type).build();
                Error.run(() -> file.writeTo(target));
            }
        }

        static TypeSpec instances(Instances is) throws Error
        {
            Modifier[] mods = is.parent == null
                ? new Modifier[] { Modifier.PUBLIC, Modifier.FINAL }
                : new Modifier[] { Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC };
            TypeSpec.Builder wrapper = TypeSpec.classBuilder(is.name)
                .addModifiers(mods)
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
            for (Instance i : is.types.values())
                instance(i, wrapper);
            for (Instances nested : is.namespaces.values())
                wrapper.addType(instances(nested));
            return wrapper.build();
        }

        static void instance(Instance instance, TypeSpec.Builder wrapper) throws Error
        {
            TypeSpec.Builder nested = TypeSpec.classBuilder(instance.name)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addSuperinterface(JasonLib.Serializable.class);
            ifaces(instance, nested);
            fields(instance, true, nested);
            constructor(instance, nested);
            factoryMethod(instance, nested);
            getters(instance, nested);
            setters(instance, nested);
            builder(instance, nested);
            toBuilder(instance, nested);
            serializableReader(instance, nested);
            serializableWriter(instance, nested);
            wrapper.addType(nested.build());
        }

        static void ifaces(Instance instance, TypeSpec.Builder type) throws Error
        {
            for (String iface : instance.ifaces)
                type.addSuperinterface(ClassName.get("", iface));
        }

        static void fields(Instance instance, boolean final_, TypeSpec.Builder type) throws Error
        {
            for (Member m : instance.members) {
                Modifier[] mods = final_
                    ? new Modifier[] { Modifier.PRIVATE, Modifier.FINAL }
                    : new Modifier[] { Modifier.PRIVATE };
                type.addField(FieldSpec.builder(m.type.type(), "_" + m.name, mods).build());
            }
        }

        static void constructor(Instance instance, TypeSpec.Builder type) throws Error
        {
            MethodSpec.Builder ctor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE);
            for (Member m : instance.members)
                ctor.addParameter(m.type.type(), m.name);
            for (Member m : instance.members)
                ctor.addStatement("_$N = $N", m.name, m.name);
            type.addMethod(ctor.build());
        }

        static void factoryMethod(Instance instance, TypeSpec.Builder type) throws Error
        {
            MethodSpec.Builder method = MethodSpec.methodBuilder("of")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get("", instance.name));
            for (Member m : instance.members)
                method.addParameter(m.type.type(), m.name);
            StringBuilder stmt = new StringBuilder();
            stmt.append("return new ").append(instance.name).append("(");
            emitEach(instance.members, ", ", stmt, (m) -> stmt.append(m.name));
            stmt.append(")");
            method.addStatement(stmt.toString());
            type.addMethod(method.build());
        }

        static void getters(Instance instance, TypeSpec.Builder type) throws Error
        {
            for (Member m : instance.members)
                type.addMethod(
                    MethodSpec.methodBuilder(m.name)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(m.type.type())
                        .addStatement("return _$N", m.name)
                        .build());
        }

        static void setters(Instance instance, TypeSpec.Builder type) throws Error
        {
            for (Member m : instance.members) {
                StringBuilder stmt = new StringBuilder();
                stmt.append("return ").append(instance.name).append(".of(");
                emitEach(instance.members, ", ", stmt, (n) -> {
                    if (m != n) stmt.append("_");
                    stmt.append(n.name);
                });
                stmt.append(")");
                type.addMethod(
                    MethodSpec.methodBuilder(m.name)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(ClassName.get("", instance.name))
                        .addParameter(m.type.type(), m.name)
                        .addStatement(stmt.toString())
                        .build());
            }
        }

        static void builder(Instance instance, TypeSpec.Builder type) throws Error
        {
            TypeSpec.Builder builder = TypeSpec.classBuilder("Builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addSuperinterface(
                    ParameterizedTypeName.get(
                        ClassName.get("com.github.mortimersmith.jason.JasonLib", "IBuilder"),
                        instance.fullName()));
            fields(instance, false, builder);
            getters(instance, builder);
            builderSetters(instance, builder);

            StringBuilder stmt = new StringBuilder();
            stmt.append("return ").append(instance.name).append(".of(");
            emitEach(instance.members, ", ", stmt, (m) -> stmt.append("_" + m.name));
            stmt.append(")");
            builder.addMethod(
                MethodSpec.methodBuilder("build")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(instance.fullName())
                    .addStatement(stmt.toString())
                    .build());

            type.addType(builder.build());
        }

        static void toBuilder(Instance instance, TypeSpec.Builder type) throws Error
        {
            StringBuilder stmt = new StringBuilder();
            stmt.append("return new Builder()");
            emitEach(instance.members, "", stmt, (m) -> stmt.append(".").append(m.name).append("(_").append(m.name).append(")"));
            type.addMethod(
                MethodSpec.methodBuilder("builder")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(ClassName.get("", "Builder"))
                    .addStatement(stmt.toString())
                    .build());
        }

        static void builderSetters(Instance instance, TypeSpec.Builder type) throws Error
        {
            for (Member m : instance.members) {
                type.addMethod(
                    MethodSpec.methodBuilder(m.name)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(ClassName.get("", "Builder"))
                        .addParameter(m.type.type(), m.name)
                        .addStatement("_$N = $N", m.name, m.name)
                        .addStatement("return this")
                        .build());
            }
        }

        static void serializableReader(Instance instance, TypeSpec.Builder type) throws Error
        {
            type.addMethod(
                MethodSpec.methodBuilder("from")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariable(TypeVariableName.get("ObjectRead"))
                    .addTypeVariable(TypeVariableName.get("ObjectWrite"))
                    .addTypeVariable(TypeVariableName.get("PrimitiveRead"))
                    .addTypeVariable(TypeVariableName.get("PrimitiveWrite"))
                    .returns(instance.fullName())
                    .addParameter(
                        ParameterizedTypeName.get(
                            ClassName.get("com.github.mortimersmith.jason.JasonLib", "Serializer"),
                            TypeVariableName.get("ObjectRead"),
                            TypeVariableName.get("ObjectWrite"),
                            TypeVariableName.get("PrimitiveRead"),
                            TypeVariableName.get("PrimitiveWrite")),
                        "s")
                    .addParameter(ClassName.get("", "PrimitiveRead"), "o")
                    .addException(IOException.class)
                    .addStatement("return serializableReader(s).read(o)")
                    .build());

            StringBuilder stmt = new StringBuilder();
            stmt.append("return (context) -> { ");
            stmt.append("ObjectRead child = s.childContext(context); ");
            stmt.append("return ");
            stmt.append(instance.name);
            stmt.append(".of(");
            emitEach(instance.members, ", ", stmt, (m) -> {
                stmt.append("s.read");
                typeClassification(m, stmt);
                stmt.append("(child, \"");
                stmt.append(m.name);
                stmt.append("\", ");
                if (!m.type.templatized()) {
                    serializableReaderPrimitive(m.type, stmt);
                } else {
                    emitEach(m.templates, ", ", stmt, (t) -> serializableReaderPrimitive(t, stmt));
                }
                stmt.append(")");
            });
            stmt.append("); }");

            type.addMethod(
                MethodSpec.methodBuilder("serializableReader")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariable(TypeVariableName.get("ObjectRead"))
                    .addTypeVariable(TypeVariableName.get("ObjectWrite"))
                    .addTypeVariable(TypeVariableName.get("PrimitiveRead"))
                    .addTypeVariable(TypeVariableName.get("PrimitiveWrite"))
                    .returns(
                        ParameterizedTypeName.get(
                            ClassName.get("com.github.mortimersmith.jason.JasonLib.Serializer", "Reader"),
                            TypeVariableName.get("PrimitiveRead"),
                            TypeVariableName.get(instance.name)))
                    .addParameter(
                        ParameterizedTypeName.get(
                            ClassName.get("com.github.mortimersmith.jason.JasonLib", "Serializer"),
                            TypeVariableName.get("ObjectRead"),
                            TypeVariableName.get("ObjectWrite"),
                            TypeVariableName.get("PrimitiveRead"),
                            TypeVariableName.get("PrimitiveWrite")),
                        "s")
                    .addException(IOException.class)
                    .addStatement(stmt.toString())
                    .build());
        }

        static void serializableWriter(Instance instance, TypeSpec.Builder type) throws Error
        {
            MethodSpec.Builder method = MethodSpec.methodBuilder("serialize")
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(TypeVariableName.get("ObjectRead"))
                .addTypeVariable(TypeVariableName.get("ObjectWrite"))
                .addTypeVariable(TypeVariableName.get("PrimitiveRead"))
                .addTypeVariable(TypeVariableName.get("PrimitiveWrite"))
                .returns(TypeVariableName.get("ObjectWrite"))
                .addParameter(
                    ParameterizedTypeName.get(
                        ClassName.get("com.github.mortimersmith.jason.JasonLib", "Serializer"),
                        TypeVariableName.get("ObjectRead"),
                        TypeVariableName.get("ObjectWrite"),
                        TypeVariableName.get("PrimitiveRead"),
                        TypeVariableName.get("PrimitiveWrite")),
                    "s")
                .addParameter(TypeVariableName.get("ObjectWrite"), "context")
                .addException(IOException.class);

            for (Member m : instance.members) {
                StringBuilder stmt = new StringBuilder();
                stmt.append("s.write");
                typeClassification(m, stmt);
                stmt.append("(context, \"");
                stmt.append(m.name);
                stmt.append("\", ");
                if (!m.type.templatized()) {
                    serializableWriterPrimitive(m.type, stmt);
                } else {
                    emitEach(m.templates, ", ", stmt, (t) -> serializableWriterPrimitive(t, stmt));
                }
                stmt.append(", _");
                stmt.append(m.name);
                stmt.append(")");
                method.addStatement(stmt.toString());
            }
            method.addStatement("return context");

            type.addMethod(method.build());
        }

        static void typeClassification(Member m, StringBuilder out) throws Error
        {
            if (m.type.isOptional()) out.append("Optional");
            else if (m.type.isList()) out.append("List");
            else if (m.type.isMap()) out.append("Map");
            else out.append("Primitive");
        }

        static void serializableReaderPrimitive(Symbol s, StringBuilder out) throws Error
        {
            if (s.isBoolean()) out.append("s.booleanReader()");
            else if (s.isInteger()) out.append("s.integerReader()");
            else if (s.isLong()) out.append("s.longReader()");
            else if (s.isFloat()) out.append("s.floatReader()");
            else if (s.isDouble()) out.append("s.doubleReader()");
            else if (s.isString()) out.append("s.stringReader()");
            else out.append(s.name()).append(".serializableReader(s)");
        }

        static void serializableWriterPrimitive(Symbol s, StringBuilder out) throws Error
        {
            if (s.isBoolean()) out.append("s.booleanWriter()");
            else if (s.isInteger()) out.append("s.integerWriter()");
            else if (s.isLong()) out.append("s.longWriter()");
            else if (s.isFloat()) out.append("s.floatWriter()");
            else if (s.isDouble()) out.append("s.doubleWriter()");
            else if (s.isString()) out.append("s.stringWriter()");
            else out.append("s.serializableWriter()");
        }
    }

    public static class Error extends Exception
    {
        public Error(String msg) { super(msg); }
        public Error(Exception e) { super(e); }
        public Error(String msg, Exception e) { super(msg, e); }
        public static Error because(String msg) { return new Error(msg); }
        public interface ThrowingRunnable<E extends Exception> { void run() throws E; }
        public interface ThrowingSupplier<T, E extends Exception> { T get() throws E; }
        public static <E extends Exception> void run(ThrowingRunnable<E> r) throws Error { wrap(r); }
        public static <T, E extends Exception> T get(ThrowingSupplier<T, E> s) throws Error { return wrap(s); }
        public static <E extends Exception> void wrap(ThrowingRunnable<E> r) throws Error {
            try { r.run(); } catch (Exception e) { throw new Error(e); }
        }
        public static <T, E extends Exception> T wrap(ThrowingSupplier<T, E> s) throws Error {
            try { return s.get(); } catch (Exception e) { throw new Error(e); }
        }
    }

    public static class Symbol
    {
        private String _type;

        private static Symbol of(String type) throws Error {
            return new Symbol(type);
        }

        private Symbol(String type) {
            _type = type;
        }

        // FIXME: properly model types
        public boolean isBoolean() { return "boolean".equals(_type); }
        public boolean isInteger() { return "int".equals(_type); }
        public boolean isLong() { return "long".equals(_type); }
        public boolean isFloat() { return "float".equals(_type); }
        public boolean isDouble() { return "double".equals(_type); }
        public boolean isString() { return "string".equals(_type); }
        public boolean isOptional() { return "optional".equals(_type); }
        public boolean isList() { return "list".equals(_type); }
        public boolean isMap() { return "map".equals(_type); }
        public boolean templatized() { return "optional".equals(_type) || "list".equals(_type) || "map".equals(_type); }

        public String toString() { return name(); }

        public boolean equals(Object o) {
            if (o == null) return false;
            if (!(o instanceof Symbol)) return false;
            return Objects.equals(_type, ((Symbol)o)._type);
        }

        public int hashCode() { return Objects.hash(_type); }

        public String name() {
            if ("boolean".equals(_type)) {
                return "boolean";
            } else if ("int".equals(_type)) {
                return "int";
            } else if ("long".equals(_type)) {
                return "long";
            } else if ("float".equals(_type)) {
                return "float";
            } else if ("double".equals(_type)) {
                return "double";
            } else if ("string".equals(_type)) {
                return "String";
            } else if ("optional".equals(_type)) {
                return "java.util.Optional";
            } else if ("list".equals(_type)) {
                return "java.util.List";
            } else if ("map".equals(_type)) {
                return "java.util.Map";
            } else {
                return _type;
            }
        }

        public TypeName type() {
            if ("boolean".equals(_type)) {
                return TypeName.BOOLEAN;
            } else if ("int".equals(_type)) {
                return TypeName.INT;
            } else if ("long".equals(_type)) {
                return TypeName.LONG;
            } else if ("float".equals(_type)) {
                return TypeName.FLOAT;
            } else if ("double".equals(_type)) {
                return TypeName.DOUBLE;
            } else if ("string".equals(_type)) {
                return ClassName.get("java.lang", "String");
            } else if ("optional".equals(_type)) {
                return ClassName.get("java.util", "Optional");
            } else if ("list".equals(_type)) {
                return ClassName.get("java.util", "List");
            } else if ("map".equals(_type)) {
                return ClassName.get("java.util", "Map");
            } else {
                return ClassName.get("", _type);
            }
        }
    }

    public static class Package
    {
        public String name;
        public final Map<String, Instances> namespaces = new HashMap<>();
    }

    public static class Instances
    {
        public Package pkg;
        public Instances parent;
        public String name;
        public final Map<String, Instances> namespaces = new HashMap<>();
        public final Map<String, Instance> types = new HashMap<>();
    }

    public static class Instance
    {
        public Instances instances;
        public String name;
        public final List<String> ifaces = new LinkedList<>();
        public final List<Member> members = new LinkedList<>();

        public ClassName fullName() {
            return ClassName.get(instances.pkg.name, generateName());
        }

        private String generateName() {
            StringBuilder out = new StringBuilder();
            generateInstanceChain(instances, out);
            out.append(".").append(name);
            return out.toString();
        }

        private StringBuilder generateInstanceChain(Instances i, StringBuilder out) {
            if  (i.parent != null) {
                generateInstanceChain(i.parent, out);
                out.append(".");
            }
            out.append(i.name);
            return out;
        }
    }

    public static class Member
    {
        public String name;
        public Symbol type;
        public Symbol[] templates;

        public void emitType(StringBuilder out) throws Error
        {
            out.append(type);
            if (templates != null && templates.length > 0) {
                out.append("<");
                emitEach(templates, ", ", out, (t) -> out.append(t));
                out.append(">");
            }
        }
    }
}
