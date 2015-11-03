package com.github.mortimersmith.jason;

import static com.github.mortimersmith.jason.Util.forEachE;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Compiler
{
    public static final Set<String> BUILTIN_TYPES = Sets.of
        ( "Boolean"
        , "Integer"
        , "Long"
        , "String"
        , "Optional"
        , "List"
        , "Map"
        );

    public static void compile(Path specs, Path target) throws Error
    {
        JsonParser parser = new JsonParser();
        for (File file : specs.toFile().listFiles()) {
            if (!file.isFile()) continue;
            if (!file.getName().endsWith(".json")) continue;
            StringBuilder out = new StringBuilder();
            JsonObject json = Error.get(() -> parser.parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)).getAsJsonObject());
            compileJson(json, target);
        }
    }

    public static void compileJson(JsonObject spec, Path target) throws Error
    {
        StringBuilder out = new StringBuilder();
        Instances instances = new Instances();
        instances.pkg = spec.get("package").getAsString();
        instances.name = spec.get("name").getAsString();
        forEachE(spec.get("types").getAsJsonObject(), (name, e) -> {
            Instance instance = new Instance();
            instance.name = name;
            forEachE(e.getAsJsonObject(), (field, type) -> {
                Member member = new Member();
                member.name = field;
                resolveType(type.getAsJsonObject(), member);
                instance.members.add(member);
            });
            instances.map.put(name, instance);
        });
        validate(instances);
        Emit.instances(instances, out);
        output(instances, out.toString(), target);
    }

    private static void output(Instances instances, String data, Path target) throws Error
    {
        Path pkg = target.resolve(instances.pkg.replace(".", File.separator));
        pkg.toFile().mkdirs();
        Path file = pkg.resolve(instances.name + ".java");
        Error.run(() -> Files.write(file, data.getBytes(StandardCharsets.UTF_8)));
    }

    private static void resolveType(JsonObject json, Member m) throws Error
    {
        String type = json.get("type").getAsString();
        m.type = Symbol.of(type);
        if ("optional".equals(type) || "list".equals(type) || "map".equals(type)) {
            String of = json.get("of").getAsString();
            m.template = Symbol.of(of);
        }
    }

    private static void validate(Instances instances) throws Error
    {
        Set<String> types = new HashSet<>();
        for (Instance i : instances.map.values())
            types.add(i.name);
        for (Instance i : instances.map.values()) {
            for (Member m : i.members) {
                if (!BUILTIN_TYPES.contains(m.type.name) && !types.contains(m.type.name))
                    throw Error.because(String.format("unknown type: %s", m.type));
                if (m.template != null && !BUILTIN_TYPES.contains(m.template.name) && !types.contains(m.template.name))
                    throw Error.because(String.format("unknown type parameter: %s: %s", m.type, m.template));
            }
        }
    }

    private interface Emit
    {
        static void instances(Instances instances, StringBuilder out) throws Error
        {
            out.append("package ").append(instances.pkg).append(";\n");
            out.append("import com.github.mortimersmith.jason.JasonLib.Serializer;\n");
            out.append("import com.github.mortimersmith.jason.JasonLib.Serializable;\n");
            out.append("import java.io.IOException;\n");
            out.append("import java.util.List;\n");
            out.append("import java.util.Map;\n");
            out.append("public interface ").append(instances.name).append(" {\n");
            for (Instance i : instances.map.values())
                instance(i, out);
            out.append("}\n");
        }

        static void instance(Instance instance, StringBuilder out) throws Error
        {
            out.append("public static class ").append(instance.name).append(" implements Serializable {\n");
            fields(instance, out);
            constructor(instance, out);
            factoryMethod(instance, out);
            getters(instance, out);
            setters(instance, out);
            serializableReader(instance, out);
            serializableWriter(instance, out);
            out.append("}\n");
        }

        static void fields(Instance instance, StringBuilder out) throws Error
        {
            for (Member m : instance.members) {
                out.append("private final ");
                m.emitType(out);
                out.append(" _").append(m.name).append(";\n");
            }
        }

        static void constructor(Instance instance, StringBuilder out) throws Error
        {
            out.append("private ").append(instance.name).append("(");
            boolean first = true;
            for (Member m : instance.members) {
                if (first) first = false; else out.append(", ");
                m.emitType(out);
                out.append(" ").append(m.name);
            }
            out.append(") {\n");
            for (Member m : instance.members) {
                out.append("_").append(m.name).append(" = ").append(m.name).append(";\n");
            }
            out.append("}\n");
        }

        static void factoryMethod(Instance instance, StringBuilder out) throws Error
        {
            out.append("public static ").append(instance.name).append(" of(");
            boolean first = true;
            for (Member m : instance.members) {
                if (first) first = false; else out.append(", ");
                m.emitType(out);
                out.append(" ").append(m.name);
            }
            out.append(") {\n");
            out.append("return new ").append(instance.name).append("(");
            first = true;
            for (Member m : instance.members) {
                if (first) first = false; else out.append(", ");
                out.append(m.name);
            }
            out.append(");\n}\n");
        }

        static void getters(Instance instance, StringBuilder out) throws Error
        {
            for (Member m : instance.members) {
                out.append("public ");
                m.emitType(out);
                out.append(" ");
                out.append(m.name);
                out.append("() { return _");
                out.append(m.name);
                out.append("; }\n");
            }
        }

        static void setters(Instance instance, StringBuilder out) throws Error
        {
            for (Member m : instance.members) {
                out.append("public ");
                out.append(instance.name);
                out.append(" ");
                out.append(m.name);
                out.append("(");
                m.emitType(out);
                out.append(" ");
                out.append(m.name);
                out.append(") { return ");
                out.append(instance.name);
                out.append(".of(");
                boolean first = true;
                for (Member n : instance.members) {
                    if (first) first = false; else out.append(", ");
                    if (m != n) out.append("_");
                    out.append(n.name);
                }
                out.append("); }\n");
            }
        }

        static void serializableReader(Instance instance, StringBuilder out) throws Error
        {

            out.append("public static <ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite> ");
            out.append(instance.name);
            out.append(" from(Serializer<ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite> s, PrimitiveRead o) throws IOException {\n");
            out.append("return serializableReader(s).read(o);\n");
            out.append("}\n");

            out.append("public static <ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite> Serializer.Reader<PrimitiveRead, ");
            out.append(instance.name);
            out.append("> serializableReader(Serializer<ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite> s) throws IOException {\n");
            out.append("return (context) -> {\n");
            out.append("ObjectRead child = s.childContext(context);\n");
            out.append("return ");
            out.append(instance.name);
            out.append(".of\n");
            boolean first = true;
            for (Member m : instance.members) {
                if (first) { first = false; out.append("( "); } else out.append(", ");
                out.append("s.read");
                typeClassification(m, out);
                out.append("(child, \"");
                out.append(m.name);
                out.append("\", ");
                serializableReaderPrimitive(m.type.templatized() ? m.template : m.type, out);
                out.append(")\n");
            }
            out.append(");\n};\n}\n");
        }

        static void serializableWriter(Instance instance, StringBuilder out) throws Error
        {
            out.append("public <ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite> ObjectWrite serialize(Serializer<ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite> s, ObjectWrite context) throws IOException {\n");
            for (Member m : instance.members) {
                out.append("s.write");
                typeClassification(m, out);
                out.append("(context, \"");
                out.append(m.name);
                out.append("\", ");
                serializableWriterPrimitive(m.type.templatized() ? m.template : m.type, out);
                out.append(", _");
                out.append(m.name);
                out.append(");\n");
            }
            out.append("return context;\n");
            out.append("}\n");
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
            else if (s.isString()) out.append("s.stringReader()");
            else out.append(s.name).append(".serializableReader(s)");
        }

        static void serializableWriterPrimitive(Symbol s, StringBuilder out) throws Error
        {
            if (s.isBoolean()) out.append("s.booleanWriter()");
            else if (s.isInteger()) out.append("s.integerWriter()");
            else if (s.isLong()) out.append("s.longWriter()");
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
        public String name;

        private static Symbol of(String type) throws Error {
            Symbol s = new Symbol();
            if ("boolean".equals(type)) {
                s.name = "Boolean";
            } else if ("integer".equals(type)) {
                s.name = "Integer";
            } else if ("long".equals(type)) {
                s.name = "Long";
            } else if ("string".equals(type)) {
                s.name = "String";
            } else if ("optional".equals(type)) {
                s.name = "Optional";
            } else if ("list".equals(type)) {
                s.name = "List";
            } else if ("map".equals(type)) {
                s.name = "Map";
            } else {
                s.name = type;
            }
            return s;
        }

        // FIXME: properly model types
        public boolean isBoolean() { return "Boolean".equals(name); }
        public boolean isInteger() { return "Integer".equals(name); }
        public boolean isLong() { return "Long".equals(name); }
        public boolean isString() { return "String".equals(name); }
        public boolean isOptional() { return "Optional".equals(name); }
        public boolean isList() { return "List".equals(name); }
        public boolean isMap() { return "Map".equals(name); }
        public boolean templatized() { return "Optional".equals(name) || "List".equals(name) || "Map".equals(name); }

        public String toString() { return name; }

        public boolean equals(Object o) {
            if (o == null) return false;
            if (!(o instanceof Symbol)) return false;
            return Objects.equals(name, ((Symbol)o).name);
        }

        public int hashCode() { return Objects.hash(name); }
    }

    public static class Instances
    {
        public String pkg;
        public String name;
        public final Map<String, Instance> map = new HashMap<>();
    }

    public static class Instance
    {
        public String name;
        public final List<Member> members = new LinkedList<>();
    }

    public static class Member
    {
        public String name;
        public Symbol type;
        public Symbol template;

        public void emitType(StringBuilder out) throws Error
        {
            out.append(type);
            if (type.isList()) {
                out.append("<").append(template).append(">");
            } else if (type.isMap()) {
                out.append("<String, ").append(template).append(">");
            }
        }

        public Symbol leafType()
        {
            return type.templatized() ? template : type;
        }
    }
}
