/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.mina.http2.impl;

import java.nio.ByteBuffer;

import org.apache.mina.http2.api.Http2HeadersFrame.Http2HeadersFrameBuilder;
import org.apache.mina.http2.impl.Http2FrameHeadePartialDecoder.Http2FrameHeader;

import static org.apache.mina.http2.api.Http2Constants.FLAGS_PADDING;
import static org.apache.mina.http2.api.Http2Constants.FLAGS_PRIORITY;
import static org.apache.mina.http2.api.Http2Constants.HTTP2_31BITS_MASK;
import static org.apache.mina.http2.api.Http2Constants.HTTP2_EXCLUSIVE_MASK;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Http2HeadersFrameDecoder extends Http2FrameDecoder {

    private enum State {
        PAD_LENGTH,
        STREAM_DEPENDENCY,
        WEIGHT,
        HEADER,
        PADDING
    }
    
    private State state;
    
    private PartialDecoder<?> decoder;
    
    private int padLength;
    
    private Http2HeadersFrameBuilder builder = new Http2HeadersFrameBuilder();
    
    public Http2HeadersFrameDecoder(Http2FrameHeader header) {
        super(header);
        if (isFlagSet(FLAGS_PADDING)) {
            state = State.PAD_LENGTH;
        } else if (isFlagSet(FLAGS_PRIORITY)) {
            state = State.STREAM_DEPENDENCY;
            decoder = new IntPartialDecoder(4);
        } else {
            state = State.HEADER;
            decoder = new BytePartialDecoder(header.getLength());
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
                if (isFlagSet(FLAGS_PRIORITY)) {
                    decoder = new IntPartialDecoder(4);
                    state = State.STREAM_DEPENDENCY;
                } else {
                    state = State.HEADER;
                    decoder = new BytePartialDecoder(getHeader().getLength() - 1 - padLength);
                }
                break;
            case STREAM_DEPENDENCY:
                if (decoder.consume(buffer)) {
                    builder.streamDependencyID(((IntPartialDecoder)decoder).getValue() & HTTP2_31BITS_MASK);
                    builder.exclusiveMode((((IntPartialDecoder)decoder).getValue() & HTTP2_EXCLUSIVE_MASK) == HTTP2_EXCLUSIVE_MASK);
                    state = State.WEIGHT;
                }
                break;
            case WEIGHT:
                builder.weight((byte) (buffer.get()+1));
                state = State.HEADER;
                int headerLength = getHeader().getLength() - 5;
                if (isFlagSet(FLAGS_PADDING)) {
                    headerLength -= (padLength + 1);
                }
                decoder = new BytePartialDecoder(headerLength);
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
