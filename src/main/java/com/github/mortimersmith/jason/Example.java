package com.github.mortimersmith.jason;

import com.github.mortimersmith.jason.JasonLib.IBuilder;
import com.github.mortimersmith.jason.JasonLib.Serializable;
import com.github.mortimersmith.jason.JasonLib.Serializer;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class Example
{
    static class Foo implements Serializable
    {
        private final Boolean _primitive;
        private final Optional<Long> _optional;
        private final List<Integer> _list;
        private final Map<String, Bar> _map;

        private Foo(Boolean primitive, Optional<Long> optional, List<Integer> list, Map<String, Bar> map) {
            _primitive = primitive;
            _optional = optional;
            _list = list;
            _map = map;
        }

        public static Foo of(Boolean primitive, Optional<Long> optional, List<Integer> list, Map<String, Bar> map) {
            return new Foo(primitive, optional, list, map);
        }

        public static class Builder implements IBuilder<Foo>
        {
            private Boolean _primitive;
            private Optional<Long> _optional;
            private List<Integer> _list;
            private Map<String, Bar> _map;

            public Boolean primitive() { return _primitive; }
            public Builder primitive(Boolean primitive) { _primitive = primitive; return this; }

            public Optional<Long> optional() { return _optional; }
            public Builder optional(Optional<Long> optional) { _optional = optional; return this; }

            public List<Integer> list() { return _list; }
            public Builder list(List<Integer> list) { _list = list; return this; }

            public Map<String, Bar> map() { return _map; }
            public Builder map(Map<String, Bar> map) { _map = map; return this; }

            public Foo build() { return Foo.of(_primitive, _optional, _list, _map); }
        }

        public Builder builder() { return new Builder().primitive(_primitive).optional(_optional).list(_list).map(_map); }

        public Boolean primitive() { return _primitive; }
        public Foo primitive(Boolean primitive) { return Foo.of(primitive, _optional, _list, _map); }

        public Optional<Long> optional() { return _optional; }
        public Foo optional(Optional<Long> optional) { return Foo.of(_primitive, optional, _list, _map); }

        public List<Integer> list() { return _list; }
        public Foo list(List<Integer> list) { return Foo.of(_primitive, _optional, list, _map); }

        public Map<String, Bar> map() { return _map; }
        public Foo map(Map<String, Bar> map) { return Foo.of(_primitive, _optional, _list, map); }

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
                    , s.readOptional(child, "optional", s.longReader())
                    , s.readList(child, "list", s.integerReader())
                    , s.readMap(child, "map", s.stringReader(), Bar.serializableReader(s))
                    );
            };
        }

        public <ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite>
            ObjectWrite
            serialize(Serializer<ObjectRead, ObjectWrite, PrimitiveRead, PrimitiveWrite> s, ObjectWrite context)
            throws IOException
        {
            s.writePrimitive(context, "primitive", s.booleanWriter(), _primitive);
            s.writeOptional(context, "optional", s.longWriter(), _optional);
            s.writeList(context, "list", s.integerWriter(), _list);
            s.writeMap(context, "map", s.stringWriter(), s.<Bar>serializableWriter(), _map);
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
