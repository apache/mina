package org.apache.mina.http2.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.apache.mina.http2.impl.Http2Connection;
import org.junit.Test;

public class Htp2PriorityFrameDecoderTest {

    @Test
    public void checkPriorityFrameNoExclusive() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x05, /*length*/
                                                        0x02, /*type*/
                                                        0x00, /*flags*/
                                                        0x00, 0x00, 0x00, 0x20, /*streamID*/
                                                        0x00, 0x00, 0x01, 0x00, /*streamDependency*/
                                                        0x01});
        Http2PriorityFrame frame = (Http2PriorityFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(5, frame.getLength());
        assertEquals(2, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(256, frame.getStreamDependencyID());
        assertFalse(frame.getExclusiveMode());
    }

    @Test
    public void checkPriorityFrameExclusive() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x05, /*length*/
                                                        0x02, /*type*/
                                                        0x00, /*flags*/
                                                        0x00, 0x00, 0x00, 0x20, /*streamID*/
                                                        (byte) 0x0080, 0x00, 0x01, 0x00, /*streamDependency*/
                                                        0x01});
        Http2PriorityFrame frame = (Http2PriorityFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(5, frame.getLength());
        assertEquals(2, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(256, frame.getStreamDependencyID());
        assertTrue(frame.getExclusiveMode());
    }
}
