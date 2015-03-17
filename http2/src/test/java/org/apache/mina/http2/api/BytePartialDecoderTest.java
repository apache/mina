package org.apache.mina.http2.api;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.junit.Test;

public class BytePartialDecoderTest {

    private static final byte[] SAMPLE_VALUE_1 = new byte[] {0x74, 0x18, 0x4F, 0x68};
    private static final byte[] SAMPLE_VALUE_2 = new byte[] {0x74, 0x18, 0x4F, 0x68, 0x0F};

    @Test
    public void checkSimpleValue() {
        BytePartialDecoder decoder = new BytePartialDecoder(4);
        ByteBuffer buffer = ByteBuffer.wrap(SAMPLE_VALUE_1);
        assertTrue(decoder.consume(buffer));
        assertArrayEquals(SAMPLE_VALUE_1, decoder.getValue());
    }
    
    @Test
    public void checkNotenoughData() {
        BytePartialDecoder decoder = new BytePartialDecoder(4);
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00});
        assertFalse(decoder.consume(buffer));
    }

    @Test
    public void checkTooMuchData() {
        BytePartialDecoder decoder = new BytePartialDecoder(4);
        ByteBuffer buffer = ByteBuffer.wrap(SAMPLE_VALUE_2);
        assertTrue(decoder.consume(buffer));
        assertArrayEquals(SAMPLE_VALUE_1, decoder.getValue());
        assertEquals(1, buffer.remaining());
    }

    @Test
    public void checkDecodingIn2Steps() {
        BytePartialDecoder decoder = new BytePartialDecoder(4);
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {SAMPLE_VALUE_2[0], SAMPLE_VALUE_2[1]});
        assertFalse(decoder.consume(buffer));
        buffer = ByteBuffer.wrap(new byte[] {SAMPLE_VALUE_2[2], SAMPLE_VALUE_2[3], SAMPLE_VALUE_2[4]});
        assertTrue(decoder.consume(buffer));
        assertArrayEquals(SAMPLE_VALUE_1, decoder.getValue());
        assertEquals(1, buffer.remaining());
    }
}
