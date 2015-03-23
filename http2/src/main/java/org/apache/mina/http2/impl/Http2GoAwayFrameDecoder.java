/**
 * 
 */
package org.apache.mina.http2.impl;

import java.nio.ByteBuffer;

import org.apache.mina.http2.api.BytePartialDecoder;
import org.apache.mina.http2.api.Http2FrameHeadePartialDecoder.Http2FrameHeader;
import org.apache.mina.http2.api.Http2GoAwayFrame.Builder;
import org.apache.mina.http2.api.IntPartialDecoder;
import org.apache.mina.http2.api.PartialDecoder;

import static org.apache.mina.http2.api.Http2Constants.HTTP2_31BITS_MASK;
/**
 * @author jeffmaury
 *
 */
public class Http2GoAwayFrameDecoder extends Http2FrameDecoder {

    private enum State {
        LAST_STREAM_ID,
        CODE,
        EXTRA
    }
    
    private State state;
    
    private PartialDecoder<?> decoder;
    
    private Builder builder = new Builder();
    
    public Http2GoAwayFrameDecoder(Http2FrameHeader header) {
        super(header);
        state = State.LAST_STREAM_ID;
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
            case LAST_STREAM_ID:
                if (decoder.consume(buffer)) {
                    builder.lastStreamID(((IntPartialDecoder)decoder).getValue() & HTTP2_31BITS_MASK);
                    state = State.CODE;
                    decoder.reset();
                }
            case CODE:
                if (decoder.consume(buffer)) {
                    builder.errorCode(((IntPartialDecoder)decoder).getValue());
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
                    builder.data(((BytePartialDecoder)decoder).getValue());
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
