package org.apache.mina.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * A {@link ByteBufferInputStream} test.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ByteBufferInputStreamTest {
    @Test
    public void testEmpty() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(0);
        InputStream is = new ByteBufferInputStream(bb);
        assertEquals(-1, is.read());
        is.close();
    }

    @Test
    public void testReadArray() throws IOException {
        byte[] src = "HELLO MINA".getBytes();
        byte[] dst = new byte[src.length];
        ByteBuffer bb = ByteBuffer.wrap(src);
        InputStream is = new ByteBufferInputStream(bb);

        assertEquals(true, is.markSupported());
        is.mark(src.length);
        assertEquals(dst.length, is.read(dst));
        assertArrayEquals(src, dst);
        assertEquals(-1, is.read());
        is.close();

        is.reset();
        byte[] dstTooBig = new byte[src.length + 1];
        assertEquals(src.length, is.read(dstTooBig));

        assertEquals(-1, is.read(dstTooBig));
    }

    @Test
    public void testSkip() throws IOException {
        byte[] src = "HELLO MINA!".getBytes();
        ByteBuffer bb = ByteBuffer.wrap(src);
        InputStream is = new ByteBufferInputStream(bb);
        is.skip(6);

        assertEquals(5, is.available());
        assertEquals('M', is.read());
        assertEquals('I', is.read());
        assertEquals('N', is.read());
        assertEquals('A', is.read());

        is.skip((long) Integer.MAX_VALUE + 1);
        assertEquals(-1, is.read());
        is.close();
    }

}
