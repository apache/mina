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

import static org.apache.mina.http2.api.Http2Constants.FRAME_TYPE_PING;

import java.nio.ByteBuffer;

/**
 * An SPY data frame
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public class Http2PingFrame extends Http2Frame {
    private final byte[] data;
    
    public byte[] getData() {
        return data;
    }

    /* (non-Javadoc)
     * @see org.apache.mina.http2.api.Http2Frame#writePayload(java.nio.ByteBuffer)
     */
    @Override
    public void writePayload(ByteBuffer buffer) {
        buffer.put(getData());
    }

    protected Http2PingFrame(Http2PingFrameBuilder builder) {
        super(builder);
        this.data = builder.getData();
    }

    
    public static class Http2PingFrameBuilder extends AbstractHttp2FrameBuilder<Http2PingFrameBuilder,Http2PingFrame> {
        private byte[] data;
        
        public Http2PingFrameBuilder data(byte[] data) {
            this.data = data;
            return this;
        }
        
        public byte[] getData() {
            return data;
        }

        @Override
        public Http2PingFrame build() {
            if (getLength() == (-1)) {
                setLength(getData().length);
            }
            return new Http2PingFrame(type(FRAME_TYPE_PING));
        }
        
        public static Http2PingFrameBuilder builder() {
            return new Http2PingFrameBuilder();
        }
    }
}
