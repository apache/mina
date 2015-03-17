package org.apache.mina.http2.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.junit.Test;

public class IntPartialDecoderTest {

    @Test
    public void checkSimpleValue() {
        IntPartialDecoder decoder = new IntPartialDecoder();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x00, 0x00});
        assertTrue(decoder.consume(buffer));
        assertEquals(0, decoder.getValue().intValue());
    }
    
    @Test
    public void checkNotenoughData() {
        IntPartialDecoder decoder = new IntPartialDecoder();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00});
        assertFalse(decoder.consume(buffer));
    }

    @Test
    public void checkTooMuchData() {
        IntPartialDecoder decoder = new IntPartialDecoder();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x00, 0x00, 0x00});
        assertTrue(decoder.consume(buffer));
        assertEquals(0, decoder.getValue().intValue());
        assertEquals(1, buffer.remaining());
    }

    @Test
    public void checkDecodingIn2Steps() {
        IntPartialDecoder decoder = new IntPartialDecoder();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00});
        assertFalse(decoder.consume(buffer));
        buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x00});
        assertTrue(decoder.consume(buffer));
        assertEquals(0, decoder.getValue().intValue());
        assertEquals(1, buffer.remaining());
    }
}
