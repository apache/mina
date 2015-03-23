/**
 * 
 */
package org.apache.mina.http2.impl;

import java.nio.ByteBuffer;
import org.apache.mina.http2.api.BytePartialDecoder;
import org.apache.mina.http2.api.Http2FrameHeadePartialDecoder.Http2FrameHeader;
import org.apache.mina.http2.api.Http2UnknownFrame.Builder;

/**
 * @author jeffmaury
 *
 */
public class Http2UnknownFrameDecoder extends Http2FrameDecoder {

    private BytePartialDecoder decoder;
    
    private Builder builder = new Builder();
    
    public Http2UnknownFrameDecoder(Http2FrameHeader header) {
        super(header);
        initBuilder(builder);
        if (header.getLength() > 0) {
            decoder = new BytePartialDecoder(header.getLength());
        } else {
            setValue(builder.build());
        }
    }
    
    /* (non-Javadoc)
     * @see org.apache.mina.http2.api.PartialDecoder#consume(java.nio.ByteBuffer)
     */
    @Override
    public boolean consume(ByteBuffer buffer) {
        while ((getValue() == null) && buffer.remaining() > 0) {
            if (decoder.consume(buffer)) {
                builder.payload(decoder.getValue());
                setValue(builder.build());
            }
        }
        return getValue() != null;
    }

    /* (non-Javadoc)
     * @see org.apache.mina.http2.api.PartialDecoder#reset()
     */
    @Override
    public void reset() {
    }

}
