package org.apache.mina.http2.api;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.nio.ByteBuffer;

import org.apache.mina.http2.impl.Http2Connection;
import org.junit.Test;

public class Htp2DataFrameDecoderTest {

    @Test
    public void checkDataNoPayloadNoPadding() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x00, /*length*/
                                                        0x00, /*type*/
                                                        0x00, /*flags*/
                                                        0x00, 0x00, 0x00, 0x32 /*streamID*/});
        Http2DataFrame frame = (Http2DataFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(0, frame.getLength());
        assertEquals(0, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(50, frame.getStreamID());
        assertEquals(0, frame.getData().length);
        assertEquals(0, frame.getPadding().length);
    }
    
    @Test
    public void checkDataWithPayloadNoPadding() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x0A, /*length*/
                                                        0x00, /*type*/
                                                        0x00, /*flags*/
                                                        0x00, 0x00, 0x00, 0x32, /*streamID*/
                                                        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A /*headerFragment*/});
        Http2DataFrame frame = (Http2DataFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(10, frame.getLength());
        assertEquals(0, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(50, frame.getStreamID());
        assertEquals(10, frame.getData().length);
        assertArrayEquals(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A}, frame.getData());
        assertEquals(0, frame.getPadding().length);
    }

    @Test
    public void checkDataNoPayloadPadding() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x03, /*length*/
                                                        0x00, /*type*/
                                                        0x08, /*flags*/
                                                        0x00, 0x00, 0x00, 0x32, /*streamID*/
                                                        0x02, 0x0E, 0x28 /*padding*/});
        Http2DataFrame frame = (Http2DataFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(3, frame.getLength());
        assertEquals(0, frame.getType());
        assertEquals(0x08, frame.getFlags());
        assertEquals(50, frame.getStreamID());
        assertEquals(0,frame.getData().length);
        assertEquals(2, frame.getPadding().length);
        assertArrayEquals(new byte[] {0x0E,  0x28}, frame.getPadding());
    }
    
    @Test
    public void checkDataWithPayloadPadding() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x0D, /*length*/
                                                        0x00, /*type*/
                                                        0x08, /*flags*/
                                                        0x00, 0x00, 0x00, 0x32, /*streamID*/
                                                        0x02, /*padLength*/
                                                        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, /*data*/
                                                        0x0E, 0x28 /*padding*/});
        Http2DataFrame frame = (Http2DataFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(13, frame.getLength());
        assertEquals(0, frame.getType());
        assertEquals(0x08, frame.getFlags());
        assertEquals(50, frame.getStreamID());
        assertEquals(10, frame.getData().length);
        assertArrayEquals(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A}, frame.getData());
        assertEquals(2, frame.getPadding().length);
        assertArrayEquals(new byte[] {0x0E, 0x28}, frame.getPadding());
    }
}
