package org.apache.mina.http2.api;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.nio.ByteBuffer;

import org.apache.mina.http2.impl.Http2Connection;
import org.junit.Test;

public class Htp2PushPromiseFrameDecoderTest {

    @Test
    public void checkWithNoPadding() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x05, /*length*/
                                                        0x05, /*type*/
                                                        0x00, /*flags*/
                                                        0x00, 0x00, 0x00, 0x01, /*streamID*/
                                                        0x00, 0x00, 0x01, 0x00, /*promisedStreamID*/
                                                        (byte) 0x0082 /*headerFragment*/});
        Http2PushPromiseFrame frame = (Http2PushPromiseFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(5, frame.getLength());
        assertEquals(5, frame.getType());
        assertEquals(0, frame.getFlags());
        assertEquals(1, frame.getStreamID());
        assertEquals(256, frame.getPromisedStreamID());
        assertEquals(1, frame.getHeaderBlockFragment().length);
        assertEquals(0x0082, frame.getHeaderBlockFragment()[0] & 0x00FF);
    }
    
    
    @Test
    public void checkWithPadding() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x16, /*length*/
                                                        0x05, /*type*/
                                                        0x08, /*flags*/
                                                        0x00, 0x00, 0x00, 0x03, /*streamID*/
                                                        0x10, /*padding length*/
                                                        0x00, 0x00, 0x00, 0x14, /*promisedStreamID*/
                                                        (byte) 0x0082, /*headerFragment*/
                                                        0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x70, 0x61, 0x64, 0x64, 0x69, 0x6E, 0x67, 0x2E /*padding*/});
        Http2PushPromiseFrame frame = (Http2PushPromiseFrame) connection.decode(buffer);
        assertEquals(22, frame.getLength());
        assertEquals(5, frame.getType());
        assertEquals(0x08, frame.getFlags());
        assertEquals(3, frame.getStreamID());
        assertEquals(20, frame.getPromisedStreamID());
        assertEquals(1, frame.getHeaderBlockFragment().length);
        assertEquals(0x0082, frame.getHeaderBlockFragment()[0] & 0x00FF);
        assertEquals(16,  frame.getPadding().length);
        assertArrayEquals(new byte[] {0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x70, 0x61, 0x64, 0x64, 0x69, 0x6E, 0x67, 0x2E}, frame.getPadding());
    }
    
}
