/**
 * 
 */
package org.apache.mina.http2.impl;

import java.nio.ByteBuffer;

import org.apache.mina.http2.api.BytePartialDecoder;
import org.apache.mina.http2.api.Http2FrameHeadePartialDecoder.Http2FrameHeader;
import org.apache.mina.http2.api.Http2WindowUpdateFrame.Builder;
import org.apache.mina.http2.api.IntPartialDecoder;
import org.apache.mina.http2.api.PartialDecoder;

/**
 * @author jeffmaury
 *
 */
public class Http2WindowUpdateFrameDecoder extends Http2FrameDecoder {

    private enum State {
        INCREMENT,
        EXTRA
    }
    
    private State state;
    
    private PartialDecoder<?> decoder;
    
    private Builder builder = new Builder();
    
    public Http2WindowUpdateFrameDecoder(Http2FrameHeader header) {
        super(header);
        state = State.INCREMENT;
        decoder = new IntPartialDecoder(4);
        initBuilder(builder);
    }
    
    /* (non-Javadoc)
     * @see org.apache.mina.http2.api.PartialDecoder#consume(java.nio.ByteBuffer)
     */
    @Override
    public boolean consume(ByteBuffer buffer) {
        while ((getValue() == null) && buffer.remaining() > 0) {
            switch (state) {
            case INCREMENT:
                if (decoder.consume(buffer)) {
                    builder.windowUpdateIncrement(((IntPartialDecoder)decoder).getValue());
                    if (getHeader().getLength() > 4) {
                        state = State.EXTRA;
                        decoder = new BytePartialDecoder(getHeader().getLength() - 4);
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
