/**
 * 
 */
package org.apache.mina.http2.impl;

import java.nio.ByteBuffer;

import org.apache.mina.http2.api.BytePartialDecoder;
import org.apache.mina.http2.api.Http2FrameHeadePartialDecoder.Http2FrameHeader;
import org.apache.mina.http2.api.Http2PriorityFrame.Builder;
import org.apache.mina.http2.api.IntPartialDecoder;
import org.apache.mina.http2.api.PartialDecoder;

import static org.apache.mina.http2.api.Http2Constants.HTTP2_31BITS_MASK;
import static org.apache.mina.http2.api.Http2Constants.HTTP2_EXCLUSIVE_MASK;

/**
 * @author jeffmaury
 *
 */
public class Http2PriorityFrameDecoder extends Http2FrameDecoder {

    private enum State {
        STREAM_DEPENDENCY,
        WEIGHT,
        EXTRA
    }
    
    private State state;
    
    private PartialDecoder<?> decoder;
    
    private Builder builder = new Builder();
    
    public Http2PriorityFrameDecoder(Http2FrameHeader header) {
        super(header);
        state = State.STREAM_DEPENDENCY;
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
            case STREAM_DEPENDENCY:
                if (decoder.consume(buffer)) {
                    builder.streamDependencyID(((IntPartialDecoder)decoder).getValue() & HTTP2_31BITS_MASK);
                    builder.exclusiveMode((((IntPartialDecoder)decoder).getValue() & HTTP2_EXCLUSIVE_MASK) == HTTP2_EXCLUSIVE_MASK);
                    state = State.WEIGHT;
                }
                break;
            case WEIGHT:
                builder.weight((short) ((buffer.get() & 0x00FF) + 1));
                int extraLength = getHeader().getLength() - 5;
                if (extraLength > 0) {
                    decoder = new BytePartialDecoder(extraLength);
                state = State.EXTRA;
                } else {
                    setValue(builder.build());
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
