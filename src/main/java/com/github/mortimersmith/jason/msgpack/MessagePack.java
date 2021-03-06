package com.github.mortimersmith.jason.msgpack;

import com.github.mortimersmith.jason.JasonLib;
import static com.github.mortimersmith.utils.Utils.ifPresentE;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;

public class MessagePack implements JasonLib.Serializer<Unpacker, Packer, Unpacker, Packer>
{
    public static final MessagePack INSTANCE = new MessagePack();

    private static final org.msgpack.MessagePack _mp = new org.msgpack.MessagePack();

    public static <T extends JasonLib.Serializable> void wrap(T t, OutputStream out) throws IOException
    {
        t.serialize(INSTANCE, _mp.createPacker(out));
    }

    public static <T extends JasonLib.Serializable> T unwrap(InputStream in, JasonLib.From<T> from) throws IOException
    {
        return from.get(INSTANCE, _mp.createUnpacker(in));
    }

    @Override
    public Unpacker childContext(Unpacker context) {
        return context;
    }

    @Override
    public JasonLib.Serializer.Reader<Unpacker, Boolean> booleanReader() throws IOException {
        return (u) -> u.readBoolean();
    }

    @Override
    public JasonLib.Serializer.Reader<Unpacker, Integer> integerReader() throws IOException {
        return (u) -> u.readInt();
    }

    @Override
    public JasonLib.Serializer.Reader<Unpacker, Long> longReader() throws IOException {
        return (u) -> u.readLong();
    }

    @Override
    public JasonLib.Serializer.Reader<Unpacker, Float> floatReader() throws IOException {
        return (u) -> u.readFloat();
    }

    @Override
    public JasonLib.Serializer.Reader<Unpacker, Double> doubleReader() throws IOException {
        return (u) -> u.readDouble();
    }

    @Override
    public JasonLib.Serializer.Reader<Unpacker, String> stringReader() throws IOException {
        return (u) -> u.readString();
    }

    @Override
    public <T> T readPrimitive(Unpacker context, String field, JasonLib.Serializer.Reader<Unpacker, T> as) throws IOException {
        return as.read(context);
    }

    @Override
    public <T> Optional<T> readOptional(Unpacker context, String field, JasonLib.Serializer.Reader<Unpacker, T> of) throws IOException {
        Boolean present = booleanReader().read(context);
        return present != null && present
            ? Optional.of(of.read(context))
            : Optional.empty()
            ;
    }

    @Override
    public <T> List<T> readList(Unpacker context, String field, JasonLib.Serializer.Reader<Unpacker, T> of) throws IOException {
        int len = context.readInt();
        List<T> l = new LinkedList<>();
        for (int i = 0; i < len; ++i) l.add(of.read(context));
        return l;
    }

    @Override
    public <T, U> Map<T, U> readMap(Unpacker context, String field, JasonLib.Serializer.Reader<Unpacker, T> rkey, JasonLib.Serializer.Reader<Unpacker, U> rvalue) throws IOException {
        int len = context.readInt();
        Map<T, U> m = new HashMap<>();
        for (int i = 0; i < len; ++i) m.put(rkey.read(context), rvalue.read(context));
        return m;
    }

    @Override
    public JasonLib.Serializer.Writer<Packer, Boolean> booleanWriter() throws IOException {
        return (p, value) -> p.write(value);
    }

    @Override
    public JasonLib.Serializer.Writer<Packer, Integer> integerWriter() throws IOException {
        return (p, value) -> p.write(value);
    }

    @Override
    public JasonLib.Serializer.Writer<Packer, Long> longWriter() throws IOException {
        return (p, value) -> p.write(value);
    }

    @Override
    public JasonLib.Serializer.Writer<Packer, Float> floatWriter() throws IOException {
        return (p, value) -> p.write(value);
    }

    @Override
    public JasonLib.Serializer.Writer<Packer, Double> doubleWriter() throws IOException {
        return (p, value) -> p.write(value);
    }

    @Override
    public JasonLib.Serializer.Writer<Packer, String> stringWriter() throws IOException {
        return (p, value) -> p.write(value);
    }

    @Override
    public <T extends JasonLib.Serializable> JasonLib.Serializer.Writer<Packer, T> serializableWriter() throws IOException {
        return (p, value) -> value.serialize(MessagePack.this, p);
    }

    @Override
    public <T> Packer writePrimitive(Packer context, String field, JasonLib.Serializer.Writer<Packer, T> as, T value) throws IOException {
        as.write(context, value);
        return context;
    }

    @Override
    public <T> Packer writeOptional(Packer context, String field, JasonLib.Serializer.Writer<Packer, T> of, Optional<T> value) throws IOException {
        ifPresentE(value, (t) -> { booleanWriter().write(context, true); of.write(context, t); })
            .otherwise(() -> booleanWriter().write(context, false));
        return context;
    }

    @Override
    public <T> Packer writeList(Packer context, String field, JasonLib.Serializer.Writer<Packer, T> of, List<T> value) throws IOException {
        context.write(value.size());
        for (T t : value) of.write(context, t);
        return context;
    }

    @Override
    public <T, U> Packer writeMap(Packer context, String field, JasonLib.Serializer.Writer<Packer, T> wkey, JasonLib.Serializer.Writer<Packer, U> wvalue, Map<T, U> value) throws IOException {
        context.write(value.size());
        for (Map.Entry<T, U> e : value.entrySet()) {
            wkey.write(context, e.getKey());
            wvalue.write(context, e.getValue());
        }
        return context;
    }
}
