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
package org.apache.mina.http2.api;

import static org.apache.mina.http2.api.Http2Constants.FRAME_TYPE_CONTINUATION;
import static org.apache.mina.http2.api.Http2Constants.EMPTY_BYTE_ARRAY;

import java.nio.ByteBuffer;

/**
 * An HTTP2 continuation frame.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Http2ContinuationFrame extends Http2Frame {

    private final byte[] headerBlockFragment;
    
    
    public byte[] getHeaderBlockFragment() {
        return headerBlockFragment;
    }

    /* (non-Javadoc)
     * @see org.apache.mina.http2.api.Http2Frame#writePayload(java.nio.ByteBuffer)
     */
    @Override
    public void writePayload(ByteBuffer buffer) {
        buffer.put(getHeaderBlockFragment());
    }

    protected Http2ContinuationFrame(Http2ContinuationFrameBuilder builder) {
        super(builder);
        this.headerBlockFragment = builder.getHeaderBlockFragment();
    }

    
    public static class Http2ContinuationFrameBuilder extends AbstractHttp2FrameBuilder<Http2ContinuationFrameBuilder,Http2ContinuationFrame> {
        private byte[] headerBlockFragment = EMPTY_BYTE_ARRAY;
        
        public Http2ContinuationFrameBuilder headerBlockFragment(byte[] headerBlockFragment) {
            this.headerBlockFragment = headerBlockFragment;
            return this;
        }
        
        public byte[] getHeaderBlockFragment() {
            return headerBlockFragment;
        }

        @Override
        public Http2ContinuationFrame build() {
            if (getLength() == (-1)) {
                setLength(getHeaderBlockFragment().length);
            }
            return new Http2ContinuationFrame(type(FRAME_TYPE_CONTINUATION));
        }
        
        public static Http2ContinuationFrameBuilder builder() {
            return new Http2ContinuationFrameBuilder();
        }
    }
}
