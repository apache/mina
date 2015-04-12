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

import static org.apache.mina.http2.api.Http2Constants.FRAME_TYPE_RST_STREAM;

import java.nio.ByteBuffer;
/**
 * An HTTP2 RST_STREAM frame.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public class Http2RstStreamFrame extends Http2Frame {
    private final long errorCode;
    
    public long getErrorCode() {
        return errorCode;
    }

    /* (non-Javadoc)
     * @see org.apache.mina.http2.api.Http2Frame#writePayload(java.nio.ByteBuffer)
     */
    @Override
    public void writePayload(ByteBuffer buffer) {
        buffer.putInt((int) getErrorCode());
    }

    protected Http2RstStreamFrame(Http2RstStreamFrameBuilder builder) {
        super(builder);
        this.errorCode = builder.getErrorCode();
    }

    
    public static class Http2RstStreamFrameBuilder extends AbstractHttp2FrameBuilder<Http2RstStreamFrameBuilder,Http2RstStreamFrame> {
        private long errorCode;
        
        public Http2RstStreamFrameBuilder errorCode(long errorCode) {
            this.errorCode = errorCode;
            return this;
        }
        
        public long getErrorCode() {
            return errorCode;
        }

        @Override
        public Http2RstStreamFrame build() {
            if (getLength() == (-1)) {
                setLength(4);
            }
            return new Http2RstStreamFrame(type(FRAME_TYPE_RST_STREAM));
        }
        
        public static Http2RstStreamFrameBuilder builder() {
            return new Http2RstStreamFrameBuilder();
        }
    }
}
