/**
 * 
 */
package org.apache.mina.http2.impl;

import java.nio.ByteBuffer;

import org.apache.mina.http2.api.BytePartialDecoder;
import org.apache.mina.http2.api.Http2FrameHeadePartialDecoder.Http2FrameHeader;
import org.apache.mina.http2.api.Http2DataFrame.Builder;
import org.apache.mina.http2.api.PartialDecoder;

import static org.apache.mina.http2.api.Http2Constants.FLAGS_PADDING;

/**
 * @author jeffmaury
 *
 */
public class Http2DataFrameDecoder extends Http2FrameDecoder {

    private enum State {
        PAD_LENGTH,
        DATA,
        PADDING
    }
    
    private State state;
    
    private PartialDecoder<?> decoder;
    
    private int padLength;
    
    private Builder builder = new Builder();
    
    public Http2DataFrameDecoder(Http2FrameHeader header) {
        super(header);
        initBuilder(builder);
        if (isFlagSet(FLAGS_PADDING)) {
            state = State.PAD_LENGTH;
        } else if (header.getLength() > 0) {
            state = State.DATA;
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
            switch (state) {
            case PAD_LENGTH:
                padLength = buffer.get();
                if ((getHeader().getLength() - 1 - padLength) > 0) {
                    state = State.DATA;
                    decoder = new BytePartialDecoder(getHeader().getLength() - 1 - padLength);
                } else if (padLength > 0) {
                    state = State.PADDING;
                    decoder = new BytePartialDecoder(padLength);
                } else {
                    setValue(builder.build());
                }
                break;
            case DATA:
                if (decoder.consume(buffer)) {
                    builder.data(((BytePartialDecoder)decoder).getValue());
                    if (isFlagSet(FLAGS_PADDING) && (padLength > 0)) {
                      state = State.PADDING;
                      decoder = new BytePartialDecoder(padLength);
                    } else {
                        setValue(builder.build());
                    }
                }
                break;
            case PADDING:
                if (decoder.consume(buffer)) {
                    builder.padding(((BytePartialDecoder)decoder).getValue());
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
