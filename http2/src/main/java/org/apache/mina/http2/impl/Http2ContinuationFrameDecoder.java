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

import org.apache.mina.http2.api.Http2ContinuationFrame.Http2ContinuationFrameBuilder;
import org.apache.mina.http2.impl.Http2FrameHeadePartialDecoder.Http2FrameHeader;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Http2ContinuationFrameDecoder extends Http2FrameDecoder {

    private BytePartialDecoder decoder;
    
    private Http2ContinuationFrameBuilder builder = new Http2ContinuationFrameBuilder();
    
    public Http2ContinuationFrameDecoder(Http2FrameHeader header) {
        super(header);
        initBuilder(builder);
        if (header.getLength() > 0) {
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
        if ((decoder != null) && decoder.consume(buffer)) {
            builder.headerBlockFragment(decoder.getValue());
            setValue(builder.build());
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
