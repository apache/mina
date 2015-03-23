/**
 * 
 */
package org.apache.mina.http2.impl;

import org.apache.mina.http2.api.Http2Frame;
import org.apache.mina.http2.api.Http2Frame.AbstractHttp2FrameBuilder;
import org.apache.mina.http2.api.Http2FrameHeadePartialDecoder.Http2FrameHeader;
import org.apache.mina.http2.api.PartialDecoder;

/**
 * @author jeffmaury
 *
 */
public abstract class Http2FrameDecoder implements PartialDecoder<Http2Frame> {
    private Http2FrameHeader header;
    
    private Http2Frame frame;

    public Http2FrameDecoder(Http2FrameHeader header) {
        this.header = header;
    }
    
    protected boolean isFlagSet(short mask) {
        return (header.getFlags() & mask) == mask;
    }
    
    protected void initBuilder(AbstractHttp2FrameBuilder builder) {
        builder.length(header.getLength());
        builder.type(header.getType());
        builder.flags(header.getFlags());
        builder.streamID(header.getStreamID());
    }
    
    protected Http2FrameHeader getHeader() {
        return header;
    }
    
    @Override
    public Http2Frame getValue() {
        return frame;
    }
    
    protected void setValue(Http2Frame frame) {
        this.frame = frame;
    }

    @Override
    public void reset() {
    }
    
}
