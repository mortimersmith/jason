package com.github.mortimersmith.jason.json;

import com.github.mortimersmith.jason.JasonLib;
import static com.github.mortimersmith.utils.Utils.ifPresentE;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Json implements JasonLib.Serializer<JsonObject, JsonObject, JsonElement, JsonElement>
{
    public static final Json INSTANCE = new Json();

    public static <T extends JasonLib.Serializable> JsonObject wrap(T t) throws IOException
    {
        return t.serialize(INSTANCE, new JsonObject());
    }

    public static <T extends JasonLib.Serializable> T unwrap(JsonObject json, JasonLib.From<T> from) throws IOException
    {
        return from.get(INSTANCE, json);
    }

    @Override
    public JsonObject childContext(JsonElement context) {
        return context.getAsJsonObject();
    }

    @Override
    public JasonLib.Serializer.Reader<JsonElement, Boolean> booleanReader() throws IOException {
        return (json) -> json.getAsBoolean();
    }

    @Override
    public JasonLib.Serializer.Reader<JsonElement, Integer> integerReader() throws IOException {
        return (json) -> json.getAsInt();
    }

    @Override
    public JasonLib.Serializer.Reader<JsonElement, Long> longReader() throws IOException {
        return (json) -> json.getAsLong();
    }

    @Override
    public JasonLib.Serializer.Reader<JsonElement, Float> floatReader() throws IOException {
        return (json) -> json.getAsFloat();
    }

    @Override
    public JasonLib.Serializer.Reader<JsonElement, Double> doubleReader() throws IOException {
        return (json) -> json.getAsDouble();
    }

    @Override
    public JasonLib.Serializer.Reader<JsonElement, String> stringReader() throws IOException {
        return (json) -> json.getAsString();
    }

    @Override
    public <T> T readPrimitive(JsonObject context, String field, JasonLib.Serializer.Reader<JsonElement, T> as) throws IOException {
        return as.read(context.get(field));
    }

    @Override
    public <T> Optional<T> readOptional(JsonObject context, String field, Reader<JsonElement, T> of) throws IOException {
        JsonElement e = context.get(field);
        return e == null ? Optional.empty() : Optional.of(of.read(context.get(field)));
    }

    @Override
    public <T> List<T> readList(JsonObject context, String field, JasonLib.Serializer.Reader<JsonElement, T> of) throws IOException {
        JsonArray a = context.getAsJsonArray(field);
        List<T> l = new LinkedList<>();
        for (int i = 0; i < a.size(); ++i) l.add(of.read(a.get(i)));
        return l;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, U> Map<T, U> readMap(JsonObject context, String field, JasonLib.Serializer.Reader<JsonElement, T> rkey, JasonLib.Serializer.Reader<JsonElement, U> rvalue) throws IOException {
        JsonObject o = context.getAsJsonObject(field);
        Map<T, U> m = new HashMap<>();
        try {
            for (Map.Entry<String, JsonElement> e : o.entrySet()) m.put((T)e.getKey(), rvalue.read(e.getValue()));
        } catch (ClassCastException e) {
            throw new IOException("json maps must have string keys");
        }
        return m;
    }

    @Override
    public JasonLib.Serializer.Writer<JsonElement, Boolean> booleanWriter() throws IOException {
        return (json, value) -> new JsonPrimitive(value);
    }

    @Override
    public JasonLib.Serializer.Writer<JsonElement, Integer> integerWriter() throws IOException {
        return (json, value) -> new JsonPrimitive(value);
    }

    @Override
    public JasonLib.Serializer.Writer<JsonElement, Long> longWriter() throws IOException {
        return (json, value) -> new JsonPrimitive(value);
    }

    @Override
    public JasonLib.Serializer.Writer<JsonElement, Float> floatWriter() throws IOException {
        return (json, value) -> new JsonPrimitive(value);
    }

    @Override
    public JasonLib.Serializer.Writer<JsonElement, Double> doubleWriter() throws IOException {
        return (json, value) -> new JsonPrimitive(value);
    }

    @Override
    public JasonLib.Serializer.Writer<JsonElement, String> stringWriter() throws IOException {
        return (json, value) -> new JsonPrimitive(value);
    }

    @Override
    public <T extends JasonLib.Serializable> JasonLib.Serializer.Writer<JsonElement, T> serializableWriter() throws IOException {
        return (json, value) -> value.serialize(Json.this, new JsonObject());
    }

    @Override
    public <T> JsonObject writePrimitive(JsonObject context, String field, JasonLib.Serializer.Writer<JsonElement, T> as, T value) throws IOException {
        context.add(field, as.write(null, value));
        return context;
    }

    @Override
    public <T> JsonObject writeOptional(JsonObject context, String field, Writer<JsonElement, T> of, Optional<T> value) throws IOException {
        ifPresentE(value, (t) -> context.add(field, of.write(null, t)));
        return context;
    }

    @Override
    public <T> JsonObject writeList(JsonObject context, String field, JasonLib.Serializer.Writer<JsonElement, T> of, List<T> value) throws IOException {
        JsonArray a = new JsonArray();
        for (T t : value) a.add(of.write(null, t));
        context.add(field, a);
        return context;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, U> JsonObject writeMap(JsonObject context, String field, JasonLib.Serializer.Writer<JsonElement, T> wkey, JasonLib.Serializer.Writer<JsonElement, U> wvalue, Map<T, U> value) throws IOException {
        JsonObject o = new JsonObject();
        try {
            for (Map.Entry<T, U> e : value.entrySet())
                o.add((String)e.getKey(), wvalue.write(null, e.getValue()));
        } catch (ClassCastException e) {
            throw new IOException("json maps must have string keys");
        }
        context.add(field, o);
        return context;
    }
}
