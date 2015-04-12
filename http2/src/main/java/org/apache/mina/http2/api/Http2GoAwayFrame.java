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

import static org.apache.mina.http2.api.Http2Constants.FRAME_TYPE_GOAWAY;
import static org.apache.mina.http2.api.Http2Constants.EMPTY_BYTE_ARRAY;

import java.nio.ByteBuffer;

/**
 * An SPY data frame
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public class Http2GoAwayFrame extends Http2Frame {
    private final int lastStreamID;
    
    private final long errorCode;

    private byte[] data;
    
    public int getLastStreamID() {
        return lastStreamID;
    }
    
    public long getErrorCode() {
        return errorCode;
    }

    public byte[] getData() {
        return data;
    }
    
    /* (non-Javadoc)
     * @see org.apache.mina.http2.api.Http2Frame#writePayload(java.nio.ByteBuffer)
     */
    @Override
    public void writePayload(ByteBuffer buffer) {
        buffer.putInt(getLastStreamID());
        buffer.putInt((int) getErrorCode());
        buffer.put(getData());
    }

    protected Http2GoAwayFrame(Http2GoAwayFrameBuilder builder) {
        super(builder);
        this.lastStreamID = builder.getLastStreamID();
        this.errorCode = builder.getErrorCode();
        this.data = builder.getData();
    }

    
    public static class Http2GoAwayFrameBuilder extends AbstractHttp2FrameBuilder<Http2GoAwayFrameBuilder,Http2GoAwayFrame> {
        private int lastStreamID;
        
        private long errorCode;
        
        private byte[] data = EMPTY_BYTE_ARRAY;
        
        public Http2GoAwayFrameBuilder lastStreamID(int lastStreamID) {
            this.lastStreamID = lastStreamID;
            return this;
        }
        
        public int getLastStreamID() {
            return lastStreamID;
        }

        public Http2GoAwayFrameBuilder errorCode(long errorCode) {
            this.errorCode = errorCode;
            return this;
        }
        
        public long getErrorCode() {
            return errorCode;
        }
        
        public Http2GoAwayFrameBuilder data(byte[] data) {
            this.data = data;
            return this;
        }
        
        public byte[] getData() {
            return data;
        }

        @Override
        public Http2GoAwayFrame build() {
            if (getLength() == (-1)) {
                setLength(getData().length + 8);
            }
            return new Http2GoAwayFrame(type(FRAME_TYPE_GOAWAY));
        }
        
        public static Http2GoAwayFrameBuilder builder() {
            return new Http2GoAwayFrameBuilder();
        }
    }
}
