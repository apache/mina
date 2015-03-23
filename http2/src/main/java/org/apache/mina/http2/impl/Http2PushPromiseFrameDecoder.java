/**
 * 
 */
package org.apache.mina.http2.impl;

import java.nio.ByteBuffer;

import org.apache.mina.http2.api.BytePartialDecoder;
import org.apache.mina.http2.api.Http2FrameHeadePartialDecoder.Http2FrameHeader;
import org.apache.mina.http2.api.Http2PushPromiseFrame.Builder;
import org.apache.mina.http2.api.IntPartialDecoder;
import org.apache.mina.http2.api.PartialDecoder;

import static org.apache.mina.http2.api.Http2Constants.FLAGS_PADDING;
import static org.apache.mina.http2.api.Http2Constants.HTTP2_31BITS_MASK;

/**
 * @author jeffmaury
 *
 */
public class Http2PushPromiseFrameDecoder extends Http2FrameDecoder {

    private enum State {
        PAD_LENGTH,
        PROMISED_STREAM,
        HEADER,
        PADDING
    }
    
    private State state;
    
    private PartialDecoder<?> decoder;
    
    private int padLength;
    
    private Builder builder = new Builder();
    
    public Http2PushPromiseFrameDecoder(Http2FrameHeader header) {
        super(header);
        if (isFlagSet(FLAGS_PADDING)) {
            state = State.PAD_LENGTH;
        } else {
            state = State.PROMISED_STREAM;
            decoder = new IntPartialDecoder(4);
        }
        initBuilder(builder);
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
                state = State.PROMISED_STREAM;
                decoder = new IntPartialDecoder(4);
                break;
            case PROMISED_STREAM:
                if (decoder.consume(buffer)) {
                    builder.promisedStreamID(((IntPartialDecoder)decoder).getValue() & HTTP2_31BITS_MASK);
                    state = State.HEADER;
                    int headerLength = getHeader().getLength() - 4;
                    if (isFlagSet(FLAGS_PADDING)) {
                        headerLength -= (padLength + 1);
                    }
                    decoder = new BytePartialDecoder(headerLength);
                }
                break;
            case HEADER:
                if (decoder.consume(buffer)) {
                    builder.headerBlockFragment(((BytePartialDecoder)decoder).getValue());
                    if (isFlagSet(FLAGS_PADDING)) {
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
