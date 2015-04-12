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

import java.nio.ByteBuffer;

/**
 * An HTTP2 unknown frame.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public class Http2UnknownFrame extends Http2Frame {
    private final byte[] payload;
    
    public byte[] getPayload() {
        return payload;
    }

    /* (non-Javadoc)
     * @see org.apache.mina.http2.api.Http2Frame#writePayload(java.nio.ByteBuffer)
     */
    @Override
    public void writePayload(ByteBuffer buffer) {
        buffer.put(getPayload());
    }

    protected Http2UnknownFrame(Http2UnknownFrameBuilder builder) {
        super(builder);
        this.payload = builder.getPayload();
    }

    
    public static class Http2UnknownFrameBuilder extends AbstractHttp2FrameBuilder<Http2UnknownFrameBuilder,Http2UnknownFrame> {
        private byte[] payload = new byte[0];
        
        public Http2UnknownFrameBuilder payload(byte[] payload) {
            this.payload = payload;
            return this;
        }
        
        public byte[] getPayload() {
            return payload;
        }

        @Override
        public Http2UnknownFrame build() {
            if (getLength() == (-1)) {
                setLength(getPayload().length);
            }
            return new Http2UnknownFrame(this);
        }
        
        public static Http2UnknownFrameBuilder builder() {
            return new Http2UnknownFrameBuilder();
        }
    }
}
