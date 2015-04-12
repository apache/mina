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

import org.apache.mina.http2.api.Http2DataFrame.Http2DataFrameBuilder;
import org.apache.mina.http2.impl.Http2FrameHeadePartialDecoder.Http2FrameHeader;

import static org.apache.mina.http2.api.Http2Constants.FLAGS_PADDING;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
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
    
    private Http2DataFrameBuilder builder = new Http2DataFrameBuilder();
    
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
