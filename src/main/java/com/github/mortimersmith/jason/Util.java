package com.github.mortimersmith.jason;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Util
{
    public interface ThrowingRunnable<E extends Exception> {
        void run() throws E;
    }

    public interface ThrowingConsumer<T, E extends Exception> {
        void accept(T t) throws E;
    }

    public interface ThrowingBiConsumer<T, U, E extends Exception> {
        void accept(T t, U u) throws E;
    }

    public static <T, U> Consumer<Map.Entry<T, U>> split(BiConsumer<T, U> c) {
        return (e) -> c.accept(e.getKey(), e.getValue());
    }

    public static <T, U, E extends Exception> ThrowingConsumer<Map.Entry<T, U>, E> splitE(ThrowingBiConsumer<T, U, E> c) throws E {
        return (e) -> c.accept(e.getKey(), e.getValue());
    }

    public static <T, E extends Exception> void forEachE(Iterable<T> i, ThrowingConsumer<T, E> c) throws E
    {
        for (T t : i) c.accept(t);
    }

    public static <T, U, E extends Exception> void forEachE(Map<T, U> m, ThrowingBiConsumer<T, U, E> c) throws E
    {
        for (Map.Entry<T, U> e : m.entrySet()) splitE(c).accept(e);
    }

    public static <E extends Exception> void forEachE(JsonObject json, ThrowingBiConsumer<String, JsonElement, E> c) throws E
    {
        for (Map.Entry<String, JsonElement> e : json.entrySet()) splitE(c).accept(e);
    }

    public interface OtherwiseE<E extends Exception>
    {
        public void otherwise(ThrowingRunnable<E> r) throws E;
    }

    public static <T, E extends Exception> OtherwiseE<E> ifPresentE(Optional<T> o, ThrowingConsumer<T, E> c) throws E
    {
        if (o.isPresent()) { c.accept(o.get()); return ifPresentOtherwiseE(true); }
        else return ifPresentOtherwiseE(false);
    }

    private static <E extends Exception> OtherwiseE<E> ifPresentOtherwiseE(boolean wasPresent)
    {
        return (r) -> { if (!wasPresent) r.run(); };
    }
}
