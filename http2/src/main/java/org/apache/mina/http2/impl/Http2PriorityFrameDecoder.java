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

import org.apache.mina.http2.api.Http2PriorityFrame.Http2PriorityFrameBuilder;
import org.apache.mina.http2.impl.Http2FrameHeadePartialDecoder.Http2FrameHeader;

import static org.apache.mina.http2.api.Http2Constants.HTTP2_31BITS_MASK;
import static org.apache.mina.http2.api.Http2Constants.HTTP2_EXCLUSIVE_MASK;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Http2PriorityFrameDecoder extends Http2FrameDecoder {

    private enum State {
        STREAM_DEPENDENCY,
        WEIGHT,
        EXTRA
    }
    
    private State state;
    
    private PartialDecoder<?> decoder;
    
    private Http2PriorityFrameBuilder builder = new Http2PriorityFrameBuilder();
    
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
