package com.github.mortimersmith.jason;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;

public class JasonLib
{
    public interface From<T extends JasonLib.Serializable>
    {
        <ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite> T
            get(JasonLib.Serializer<ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite> s, PrimitiveRead o)
            throws IOException;
    }

    public interface Serializable
    {
        <ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite>
            ObjectWrite
            serialize(Serializer<ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite> s, ObjectWrite context)
            throws IOException;
    }

    public interface Serializer<ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite>
    {
        public interface Reader<C, T> {
            T read(C p) throws IOException;
        }

        public interface Writer<C, T> {
            C write(C p, T t) throws IOException;
        }

        ObjectRead childContext(PrimitiveRead context);

        Reader<PrimitiveRead, Boolean> booleanReader() throws IOException;
        Reader<PrimitiveRead, Integer> integerReader() throws IOException;
        Reader<PrimitiveRead, Long> longReader() throws IOException;
        Reader<PrimitiveRead, String> stringReader() throws IOException;

        <T> T readPrimitive(ObjectRead context, String field, Reader<PrimitiveRead, T> as) throws IOException;
        <T> List<T> readList(ObjectRead context, String field, Reader<PrimitiveRead, T> of) throws IOException;
        <T> Map<String, T> readMap(ObjectRead context, String field, Reader<PrimitiveRead, T> of) throws IOException;

        Writer<PrimitiveWrite, Boolean> booleanWriter() throws IOException;
        Writer<PrimitiveWrite, Integer> integerWriter() throws IOException;
        Writer<PrimitiveWrite, Long> longWriter() throws IOException;
        Writer<PrimitiveWrite, String> stringWriter() throws IOException;
        <T extends Serializable> Writer<PrimitiveWrite, T> serializableWriter() throws IOException;

        <T> ObjectWrite writePrimitive(ObjectWrite context, String field, Writer<PrimitiveWrite, T> as, T value) throws IOException;
        <T> ObjectWrite writeList(ObjectWrite context, String field, Writer<PrimitiveWrite, T> of, List<T> value) throws IOException;
        <T> ObjectWrite writeMap(ObjectWrite context, String field, Writer<PrimitiveWrite, T> of, Map<String, T> value) throws IOException;
    }
}