package org.apache.mina.codec.delimited.serialization;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.mina.codec.delimited.ByteBufferDecoder;
import org.apache.mina.codec.delimited.ByteBufferEncoder;
import org.junit.Test;

abstract public class GenericSerializerTest<T> {

    abstract public ByteBufferDecoder<T> getDecoder() throws Exception;

    abstract public ByteBufferEncoder<T> getEncoder() throws Exception;

    abstract public List<T> getObjects();

    @Test
    public void testSerialization() throws Exception {
        ByteBufferDecoder<T> decoder = getDecoder();
        ByteBufferEncoder<T> encoder = getEncoder();
        for (T object : getObjects())
            assertEquals(object, decoder.decode(encoder.encode(object)));
    }

    @Test
    public void testEncodedSize() throws Exception {
        ByteBufferDecoder<T> decoder = getDecoder();
        ByteBufferEncoder<T> encoder = getEncoder();
        for (T object : getObjects()) {
            int size = encoder.getEncodedSize(object);
            ByteBuffer out = ByteBuffer.allocate(size);
            encoder.writeTo(object, out);
            assertEquals(size, out.position());
            out.position(0);
            assertEquals(object, decoder.decode(out));
        }
    }
}
