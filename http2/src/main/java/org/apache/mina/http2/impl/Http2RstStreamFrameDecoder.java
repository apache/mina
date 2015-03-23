/**
 * 
 */
package org.apache.mina.http2.impl;

import java.nio.ByteBuffer;

import org.apache.mina.http2.api.BytePartialDecoder;
import org.apache.mina.http2.api.Http2FrameHeadePartialDecoder.Http2FrameHeader;
import org.apache.mina.http2.api.Http2RstStreamFrame.Builder;
import org.apache.mina.http2.api.IntPartialDecoder;
import org.apache.mina.http2.api.LongPartialDecoder;
import org.apache.mina.http2.api.PartialDecoder;

import static org.apache.mina.http2.api.Http2Constants.HTTP2_31BITS_MASK;

/**
 * @author jeffmaury
 *
 */
public class Http2RstStreamFrameDecoder extends Http2FrameDecoder {

    private enum State {
        ERROR_CODE,
        EXTRA
    }
    
    private State state;
    
    private PartialDecoder<?> decoder;
    
    private Builder builder = new Builder();
    
    public Http2RstStreamFrameDecoder(Http2FrameHeader header) {
        super(header);
        state = State.ERROR_CODE;
        decoder = new LongPartialDecoder(4);
        initBuilder(builder);
    }
    
    /* (non-Javadoc)
     * @see org.apache.mina.http2.api.PartialDecoder#consume(java.nio.ByteBuffer)
     */
    @Override
    public boolean consume(ByteBuffer buffer) {
        while ((getValue() == null) && buffer.remaining() > 0) {
            switch (state) {
            case ERROR_CODE:
                if (decoder.consume(buffer)) {
                    builder.errorCode(((LongPartialDecoder)decoder).getValue());
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
