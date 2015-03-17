/**
 * 
 */
package org.apache.mina.http2.impl;

import java.nio.ByteBuffer;

import org.apache.mina.http2.api.BytePartialDecoder;
import org.apache.mina.http2.api.Http2Frame;
import org.apache.mina.http2.api.LongPartialDecoder;
import org.apache.mina.http2.api.PartialDecoder;

import static org.apache.mina.http2.api.Http2Constants.HTTP2_31BITS_MASK;

/**
 * @author jeffmaury
 *
 */
public class Http2FrameDecoder implements PartialDecoder<Http2Frame> {

    private static enum State {
        LENGTH_TYPE_FLAGS,
        STREAMID,
        PAYLOAD
    }
    
    private State state;
    
    private PartialDecoder<?> decoder;
    
    private Http2Frame frame;
    
    private boolean frameComplete;

    public Http2FrameDecoder() {
        reset();
    }
    
    @Override
    public boolean consume(ByteBuffer buffer) {
        while (!frameComplete && buffer.remaining() > 0) {
            switch (state) {
            case LENGTH_TYPE_FLAGS:
                if (decoder.consume(buffer)) {
                    long val = ((LongPartialDecoder)decoder).getValue();
                    frame.setLength((int) ((val >> 16) & 0xFFFFFFL));
                    frame.setType((short) ((val >> 8) & 0xFF));
                    frame.setFlags((short) (val & 0xFF));
                    state = State.STREAMID;
                    decoder = new LongPartialDecoder(4);
                }
                break;
            case STREAMID:
                if (decoder.consume(buffer)) {
                    frame.setStreamID((int) (((LongPartialDecoder)decoder).getValue() & HTTP2_31BITS_MASK));
                    if (frame.getLength() > 0) {
                        decoder = new BytePartialDecoder(frame.getLength());
                        state = State.PAYLOAD;
                    } else {
                       frameComplete = true;
                    }
                }
                break;
            case PAYLOAD:
                if (decoder.consume(buffer)) {
                    frame.setPayload(((BytePartialDecoder)decoder).getValue());
                    frameComplete = true;
                }
                break;
            }
        }
        return frameComplete;
    }

    @Override
    public Http2Frame getValue() {
        return frame;
    }

    @Override
    public void reset() {
        state = State.LENGTH_TYPE_FLAGS;
        decoder = new LongPartialDecoder(5);
        frame = new Http2Frame();
        frameComplete = false;
    }
    
}
