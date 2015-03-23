package org.apache.mina.http2.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.apache.mina.http2.api.Http2FrameHeadePartialDecoder.Http2FrameHeader;
import org.junit.Test;

public class Http2FrameHeaderPartialDecoderTest {

    @Test
    public void checkStandardValue() {
        Http2FrameHeadePartialDecoder decoder = new Http2FrameHeadePartialDecoder();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x00, /*length*/
                                                        0x00, /*type*/
                                                        0x00, /*flags*/
                                                        0x00, 0x00, 0x00, 0x01 /*streamID*/});
        assertTrue(decoder.consume(buffer));
        Http2FrameHeader header = decoder.getValue();
        assertNotNull(header);
        assertEquals(0, header.getLength());
        assertEquals(0, header.getType());
        assertEquals(0, header.getFlags());
        assertEquals(1, header.getStreamID());
    }

    @Test
    public void checkReservedBitIsNotTransmitted() {
        Http2FrameHeadePartialDecoder decoder = new Http2FrameHeadePartialDecoder();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x00, /*length*/
                                                        0x00, /*type*/
                                                        0x00, /*flags*/
                                                        (byte)0x80, 0x00, 0x00, 0x01 /*streamID*/});
        assertTrue(decoder.consume(buffer));
        Http2FrameHeader header = decoder.getValue();
        assertNotNull(header);
        assertEquals(0, header.getLength());
        assertEquals(0, header.getType());
        assertEquals(0, header.getFlags());
        assertEquals(1, header.getStreamID());
    }
    
    @Test
    public void checkPayLoadIsTransmitted() {
        Http2FrameHeadePartialDecoder decoder = new Http2FrameHeadePartialDecoder();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x01, /*length*/
                                                        0x00, /*type*/
                                                        0x00, /*flags*/
                                                        (byte)0x80, 0x00, 0x00, 0x01, /*streamID*/
                                                        0x40});
        assertTrue(decoder.consume(buffer));
        Http2FrameHeader header = decoder.getValue();
        assertNotNull(header);
        assertEquals(1, header.getLength());
        assertEquals(0, header.getType());
        assertEquals(0, header.getFlags());
        assertEquals(1, header.getStreamID());
    }

}
