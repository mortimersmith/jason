package com.github.mortimersmith.jason;

import com.github.mortimersmith.jason.JasonLib.Serializable;
import com.github.mortimersmith.jason.JasonLib.Serializer;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Example
{
    static class Foo implements Serializable
    {
        private final boolean _primitive;
        private final List<Integer> _list;
        private final Map<String, Bar> _map;

        private Foo(boolean primitive, List<Integer> list, Map<String, Bar> map) {
            _primitive = primitive;
            _list = list;
            _map = map;
        }

        public static Foo of(boolean primitive, List<Integer> list, Map<String, Bar> map) {
            return new Foo(primitive, list, map);
        }

        public boolean primitive() { return _primitive; }
        public Foo primitive(boolean primitive) { return Foo.of(primitive, _list, _map); }

        public List<Integer> list() { return _list; }
        public Foo list(List<Integer> list) { return Foo.of(_primitive, list, _map); }

        public Map<String, Bar> map() { return _map; }
        public Foo map(Map<String, Bar> map) { return Foo.of(_primitive, _list, map); }

        public static
            <ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite> Foo
            from(Serializer<ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite> s, PrimitiveRead o)
            throws IOException
        {
            return serializableReader(s).read(o);
        }

        public static
            <ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite> Serializer.Reader<PrimitiveRead, Foo>
            serializableReader(Serializer<ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite> s)
            throws IOException
        {
            return (context) -> {
                ObjectRead child = s.childContext(context);
                return Foo.of
                    ( s.readPrimitive(child, "primitive", s.booleanReader())
                    , s.readList(child, "list", s.integerReader())
                    , s.readMap(child, "map", Bar.serializableReader(s))
                    );
            };
        }

        public <ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite>
            ObjectWrite
            serialize(Serializer<ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite> s, ObjectWrite context)
            throws IOException
        {
            s.writePrimitive(context, "primitive", s.booleanWriter(), _primitive);
            s.writeList(context, "list", s.integerWriter(), _list);
            s.writeMap(context, "map", s.<Bar>serializableWriter(), _map);
            return context;
        }
    }

    static class Bar implements Serializable
    {
        private Bar() {}

        public static Bar of() {
            return new Bar();
        }

        public static
            <ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite> Bar
            from(Serializer<ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite> s, PrimitiveRead o)
            throws IOException
        {
            return serializableReader(s).read(o);
        }

        public static
            <ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite> Serializer.Reader<PrimitiveRead, Bar>
            serializableReader(Serializer<ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite> s)
            throws IOException
        {
            return (c) -> Bar.of();
        }

        public <ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite>
            ObjectWrite
            serialize(Serializer<ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite> s, ObjectWrite context)
        {
            return context;
        }
    }

    public interface ImmutableList<T>
    {
        int size();
        boolean empty();
        boolean contains(T t);
        T[] toArray();
        T[] toArray(T[] a);
        T get(int index);
        int indexOf(Object o);
        int lastIndexOf(Object o);
        Stream<T> stream();
    }

    public interface ImmutableSet<T>
    {
        int size();
        boolean empty();
        boolean contains(T t);
        T[] toArray();
        T[] toArray(T[] a);
        Stream<T> stream();
    }

    public interface ImmutableMap<K, V>
    {
        int size();
        boolean empty();
        boolean containsKey(K key);
        boolean containsValue(V value);
        V get(K key);
        ImmutableSet<K> keys();
        ImmutableList<V> values();
        ImmutableSet<Map.Entry<K, V>> entries();
    }
}
