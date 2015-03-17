package org.apache.mina.http2.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.nio.ByteBuffer;

import org.apache.mina.http2.impl.Http2Connection;
import org.junit.Test;

public class Htp2SettingsFrameDecoderTest {

    @Test
    public void checkRstStream() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x06, /*length*/
                                                        0x04, /*type*/
                                                        0x00, /*flags*/
                                                        0x00, 0x00, 0x00, 0x20, /*streamID*/
                                                        0x00, 0x01, /*ID*/
                                                        0x01, 0x02, 0x03, 0x04, /*value*/});
        Http2SettingsFrame frame = (Http2SettingsFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(6, frame.getLength());
        assertEquals(4, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(1, frame.getSettings().size());
        Http2Setting setting = frame.getSettings().iterator().next();
        assertEquals(1, setting.getID());
        assertEquals(0x01020304L, setting.getValue());
    }

    @Test
    public void checkRstStreamHighestID() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x06, /*length*/
                                                        0x04, /*type*/
                                                        0x00, /*flags*/
                                                        0x00, 0x00, 0x00, 0x20, /*streamID*/
                                                        (byte) 0xFF, (byte) 0xFF, /*ID*/
                                                        0x01, 0x02, 0x03, 0x04, /*value*/});
        Http2SettingsFrame frame = (Http2SettingsFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(6, frame.getLength());
        assertEquals(4, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(1, frame.getSettings().size());
        Http2Setting setting = frame.getSettings().iterator().next();
        assertEquals(0x00FFFF, setting.getID());
        assertEquals(0x01020304L, setting.getValue());
    }
 
    @Test
    public void checkRstStreamHighestValue() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x06, /*length*/
                                                        0x04, /*type*/
                                                        0x00, /*flags*/
                                                        0x00, 0x00, 0x00, 0x20, /*streamID*/
                                                        0x00, 0x01, /*ID*/
                                                        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, /*value*/});
        Http2SettingsFrame frame = (Http2SettingsFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(6, frame.getLength());
        assertEquals(4, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(1, frame.getSettings().size());
        Http2Setting setting = frame.getSettings().iterator().next();
        assertEquals(1, setting.getID());
        assertEquals(0xFFFFFFFFL, setting.getValue());
    }
}
