package org.apache.mina.codec.delimited.serialization;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.mina.codec.delimited.Transcoder;
import org.junit.Test;

abstract public class TranscoderTest<T,U> {
    abstract public Transcoder<T,T> getTranscoderInstance() throws Exception;

    abstract public List<T> getObjects();

    @Test
    public void testSerialization() throws Exception {
        Transcoder<T,T> transcoder = getTranscoderInstance();
        for (T object : getObjects())
            assertEquals(object, transcoder.decode(transcoder.encode(object)));
    }

    @Test
    public void testEncodedSize() throws Exception {
        Transcoder<T,T> transcoder = getTranscoderInstance();
        for (T object : getObjects()) {
            int size = transcoder.getEncodedSize(object);
            ByteBuffer out = ByteBuffer.allocate(size);
            transcoder.writeTo(object, out);
            assertEquals(size, out.position());
            out.position(0);
            assertEquals(object, transcoder.decode(out));
        }
    }
}
