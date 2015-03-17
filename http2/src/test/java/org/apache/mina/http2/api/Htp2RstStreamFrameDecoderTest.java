package org.apache.mina.http2.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.apache.mina.http2.impl.Http2Connection;
import org.junit.Test;

public class Htp2RstStreamFrameDecoderTest {

    @Test
    public void checkRstStreamNoExtraPayload() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x04, /*length*/
                                                        0x03, /*type*/
                                                        0x00, /*flags*/
                                                        0x00, 0x00, 0x00, 0x20, /*streamID*/
                                                        0x00, 0x00, 0x01, 0x00, /*errorCode*/});
        Http2RstStreamFrame frame = (Http2RstStreamFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(4, frame.getLength());
        assertEquals(3, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(256, frame.getErrorCode());
    }

    @Test
    public void checkRstStreamHighestValueNoExtraPayload() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x04, /*length*/
                                                        0x03, /*type*/
                                                        0x00, /*flags*/
                                                        0x00, 0x00, 0x00, 0x20, /*streamID*/
                                                        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, /*errorCode*/});
        Http2RstStreamFrame frame = (Http2RstStreamFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(4, frame.getLength());
        assertEquals(3, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(0x00FFFFFFFFL, frame.getErrorCode());
    }

    @Test
    public void checkRstStreamWithExtraPayload() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x06, /*length*/
                                                        0x03, /*type*/
                                                        0x00, /*flags*/
                                                        0x00, 0x00, 0x00, 0x20, /*streamID*/
                                                        0x00, 0x00, 0x01, 0x00, /*errorCode*/
                                                        0x0E, 0x28});
        Http2RstStreamFrame frame = (Http2RstStreamFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(6, frame.getLength());
        assertEquals(3, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(256, frame.getErrorCode());
    }

    @Test
    public void checkRstStreamHighestValueWithExtraPayload() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x06, /*length*/
                                                        0x03, /*type*/
                                                        0x00, /*flags*/
                                                        0x00, 0x00, 0x00, 0x20, /*streamID*/
                                                        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, /*errorCode*/
                                                        0x0E, 0x28});
        Http2RstStreamFrame frame = (Http2RstStreamFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(6, frame.getLength());
        assertEquals(3, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(0x00FFFFFFFFL, frame.getErrorCode());
    }
}
