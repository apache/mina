/**
 * 
 */
package org.apache.mina.http2.impl;

import java.nio.ByteBuffer;

import org.apache.mina.codec.ProtocolDecoder;
import org.apache.mina.http2.api.Http2Frame;

/**
 * @author jeffmaury
 *
 */
public class Http2ProtocolDecoder implements ProtocolDecoder<ByteBuffer, Http2Frame, Http2Connection> {

    @Override
    public Http2Connection createDecoderState() {
        return new Http2Connection();
    }

    @Override
    public Http2Frame decode(ByteBuffer input, Http2Connection context) {
        return context.decode(input);
    }

    @Override
    public void finishDecode(Http2Connection context) {
    }
}
