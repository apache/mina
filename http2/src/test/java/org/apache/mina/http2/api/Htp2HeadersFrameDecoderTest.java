package org.apache.mina.http2.api;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.nio.ByteBuffer;

import org.apache.mina.http2.impl.Http2Connection;
import org.junit.Test;

public class Htp2HeadersFrameDecoderTest {

    @Test
    public void checkHeadersFrameWithNotPadding() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x01, /*length*/
                                                        0x01, /*type*/
                                                        0x00, /*flags*/
                                                        0x00, 0x00, 0x00, 0x01, /*streamID*/
                                                        (byte) 0x0082 /*headerFragment*/});
        Http2HeadersFrame frame = (Http2HeadersFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(1, frame.getLength());
        assertEquals(1, frame.getType());
        assertEquals(0, frame.getFlags());
        assertEquals(1, frame.getStreamID());
        assertEquals(1, frame.getHeaderBlockFragment().length);
        assertEquals(0x0082, frame.getHeaderBlockFragment()[0] & 0x00FF);
    }
    
    
    @Test
    public void checkHeadersFramePaddingPriority() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x17, /*length*/
                                                        0x01, /*type*/
                                                        0x28, /*flags*/
                                                        0x00, 0x00, 0x00, 0x03, /*streamID*/
                                                        0x10, /*padding length*/
                                                        (byte)0x0080, 0x00, 0x00, 0x14, /*stream dependency*/
                                                        0x09, /*weight*/
                                                        (byte) 0x0082, /*headerFragment*/
                                                        0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x70, 0x61, 0x64, 0x64, 0x69, 0x6E, 0x67, 0x2E /*padding*/});
        Http2HeadersFrame frame = (Http2HeadersFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(23, frame.getLength());
        assertEquals(1, frame.getType());
        assertEquals(0x28, frame.getFlags());
        assertEquals(3, frame.getStreamID());
        assertEquals(10,  frame.getWeight());
        assertEquals(1, frame.getHeaderBlockFragment().length);
        assertEquals(0x0082, frame.getHeaderBlockFragment()[0] & 0x00FF);
        assertEquals(16,  frame.getPadding().length);
        assertArrayEquals(new byte[] {0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x70, 0x61, 0x64, 0x64, 0x69, 0x6E, 0x67, 0x2E}, frame.getPadding());
    }
    
    @Test
    public void checkHeadersFramePaddingNoPriority() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x12, /*length*/
                                                        0x01, /*type*/
                                                        0x08, /*flags*/
                                                        0x00, 0x00, 0x00, 0x03, /*streamID*/
                                                        0x10, /*padding length*/
                                                        (byte) 0x0082, /*headerFragment*/
                                                        0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x70, 0x61, 0x64, 0x64, 0x69, 0x6E, 0x67, 0x2E /*padding*/});
        Http2HeadersFrame frame = (Http2HeadersFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(18, frame.getLength());
        assertEquals(1, frame.getType());
        assertEquals(0x08, frame.getFlags());
        assertEquals(3, frame.getStreamID());
        assertEquals(1, frame.getHeaderBlockFragment().length);
        assertEquals(0x0082, frame.getHeaderBlockFragment()[0] & 0x00FF);
        assertEquals(16,  frame.getPadding().length);
        assertArrayEquals(new byte[] {0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x70, 0x61, 0x64, 0x64, 0x69, 0x6E, 0x67, 0x2E}, frame.getPadding());
    }
}
