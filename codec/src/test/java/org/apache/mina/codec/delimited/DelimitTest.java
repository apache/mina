package org.apache.mina.codec.delimited;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

abstract public class DelimitTest<T> {

    abstract public List<T> getObjects();

    abstract protected ByteBuffer delimitWithOriginal() throws Exception;

    abstract public SizePrefixedEncoder<T> getSerializer() throws Exception;

    final protected ByteBuffer delimitWithMina() throws Exception {
        SizePrefixedEncoder<T> pe = getSerializer();

        List<ByteBuffer> buffers = new LinkedList<ByteBuffer>();
        for (T p : getObjects())
            buffers.add(pe.encode(p, null));

        int size = 0;
        for (ByteBuffer b : buffers)
            size += b.remaining();

        ByteBuffer buffer = ByteBuffer.allocate(size);
        for (ByteBuffer b : buffers)
            buffer.put(b);
        buffer.flip();
        return buffer;
    }

    @Test
    public void testDelimit() throws Exception {
        assertEquals(delimitWithOriginal(), delimitWithMina());
    }

}
