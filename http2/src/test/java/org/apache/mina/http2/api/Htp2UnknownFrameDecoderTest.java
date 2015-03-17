package org.apache.mina.http2.api;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.nio.ByteBuffer;

import org.apache.mina.http2.impl.Http2Connection;
import org.junit.Test;

public class Htp2UnknownFrameDecoderTest {

    @Test
    public void checkUnknownFrame() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x02, /*length*/
                                                        (byte) 0x00FF, /*type*/
                                                        0x00, /*flags*/
                                                        0x00, 0x00, 0x00, 0x20, /*streamID*/
                                                        0x0E, 0x18});
        Http2UnknownFrame frame = (Http2UnknownFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(2, frame.getLength());
        assertEquals(255, frame.getType() & 0x00FF);
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(2, frame.getPayload().length);
        assertArrayEquals(new byte[] {0x0E,  0x18}, frame.getPayload());
    }

    @Test
    public void checkUnknownFrameWithoutPayload() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x00, /*length*/
                                                        (byte) 0x00FF, /*type*/
                                                        0x00, /*flags*/
                                                        0x00, 0x00, 0x00, 0x20 /*streamID*/});
        Http2UnknownFrame frame = (Http2UnknownFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(0, frame.getLength());
        assertEquals(255, frame.getType() & 0x00FF);
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(0, frame.getPayload().length);
    }

}
