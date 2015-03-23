/**
 * 
 */
package org.apache.mina.http2.impl;

import java.nio.ByteBuffer;
import org.apache.mina.http2.api.BytePartialDecoder;
import org.apache.mina.http2.api.Http2FrameHeadePartialDecoder.Http2FrameHeader;
import org.apache.mina.http2.api.Http2PingFrame.Builder;

/**
 * @author jeffmaury
 *
 */
public class Http2PingFrameDecoder extends Http2FrameDecoder {

    private static enum State {
        DATA,
        EXTRA
    }
    
    private State state;
    
    private BytePartialDecoder decoder;
    
    private Builder builder = new Builder();
    
    public Http2PingFrameDecoder(Http2FrameHeader header) {
        super(header);
        decoder = new BytePartialDecoder(8);
        state = State.DATA;
        initBuilder(builder);
    }
    
    /* (non-Javadoc)
     * @see org.apache.mina.http2.api.PartialDecoder#consume(java.nio.ByteBuffer)
     */
    @Override
    public boolean consume(ByteBuffer buffer) {
        while ((getValue() == null) && buffer.remaining() > 0) {
            switch (state) {
            case DATA:
                if (decoder.consume(buffer)) {
                    builder.data(decoder.getValue());
                    if (getHeader().getLength() > 8) {
                        state = State.EXTRA;
                        decoder = new BytePartialDecoder(getHeader().getLength() - 8);
                    } else {
                        setValue(builder.build());
                    }
                }
                break;
            case EXTRA:
                if (decoder.consume(buffer)) {
                    setValue(builder.build());
                }
                break;
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
