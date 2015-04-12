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

import org.apache.mina.http2.api.Http2GoAwayFrame.Http2GoAwayFrameBuilder;
import org.apache.mina.http2.impl.Http2FrameHeadePartialDecoder.Http2FrameHeader;

import static org.apache.mina.http2.api.Http2Constants.HTTP2_31BITS_MASK;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Http2GoAwayFrameDecoder extends Http2FrameDecoder {

    private enum State {
        LAST_STREAM_ID,
        CODE,
        EXTRA
    }
    
    private State state;
    
    private PartialDecoder<?> decoder;
    
    private Http2GoAwayFrameBuilder builder = new Http2GoAwayFrameBuilder();
    
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
                    decoder = new LongPartialDecoder(4);
                }
            case CODE:
                if (decoder.consume(buffer)) {
                    builder.errorCode(((LongPartialDecoder)decoder).getValue());
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
