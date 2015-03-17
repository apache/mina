package org.apache.mina.http2.impl;

import java.nio.ByteBuffer;

import org.apache.mina.http2.api.Http2Frame;

public class Http2Connection {

    private final Http2FrameDecoder decoder = new Http2FrameDecoder();

    /**
     * Decode the incoming message and if all frame data has been received,
     * then the decoded frame will be returned. Otherwise, null is returned.
     * 
     * @param input the input buffer to decode
     * @return the decoder HTTP2 frame or null if more data are required
     */
    public Http2Frame decode(ByteBuffer input) {
        Http2Frame frame = null;
        if (decoder.consume(input)) {
            frame = decoder.getValue();
            decoder.reset();
        }
        return frame;
    }
}
