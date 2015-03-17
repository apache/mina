package org.apache.mina.http2.api;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.nio.ByteBuffer;

import org.apache.mina.http2.impl.Http2Connection;
import org.junit.Test;

public class Htp2ContinuationFrameDecoderTest {

    @Test
    public void checkContinuationNoHeaderFragment() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x00, /*length*/
                                                        0x09, /*type*/
                                                        0x00, /*flags*/
                                                        0x00, 0x00, 0x00, 0x32 /*streamID*/});
        Http2ContinuationFrame frame = (Http2ContinuationFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(0, frame.getLength());
        assertEquals(9, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(50, frame.getStreamID());
        assertEquals(0, frame.getHeaderBlockFragment().length);
    }
    
    @Test
    public void checkContinuationWithHeaderFragment() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x0A, /*length*/
                                                        0x09, /*type*/
                                                        0x00, /*flags*/
                                                        0x00, 0x00, 0x00, 0x32, /*streamID*/
                                                        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A /*headerFragment*/});
        Http2ContinuationFrame frame = (Http2ContinuationFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(10, frame.getLength());
        assertEquals(9, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(50, frame.getStreamID());
        assertEquals(10, frame.getHeaderBlockFragment().length);
        assertArrayEquals(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A}, frame.getHeaderBlockFragment());
    }
}
