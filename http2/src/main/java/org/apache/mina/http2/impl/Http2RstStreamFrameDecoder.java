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

import org.apache.mina.http2.api.Http2RstStreamFrame.Http2RstStreamFrameBuilder;
import org.apache.mina.http2.impl.Http2FrameHeadePartialDecoder.Http2FrameHeader;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Http2RstStreamFrameDecoder extends Http2FrameDecoder {

    private enum State {
        ERROR_CODE,
        EXTRA
    }
    
    private State state;
    
    private PartialDecoder<?> decoder;
    
    private Http2RstStreamFrameBuilder builder = new Http2RstStreamFrameBuilder();
    
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
