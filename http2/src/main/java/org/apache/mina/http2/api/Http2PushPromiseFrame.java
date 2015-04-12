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

import static org.apache.mina.http2.api.Http2Constants.EMPTY_BYTE_ARRAY;
import static org.apache.mina.http2.api.Http2Constants.FLAGS_PADDING;
import static org.apache.mina.http2.api.Http2Constants.FRAME_TYPE_PUSH_PROMISE;

import java.nio.ByteBuffer;

/**
 * An SPY data frame
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public class Http2PushPromiseFrame extends Http2Frame {

    private final byte[] padding;
    
    private final int promisedStreamID;
    
    private final byte[] headerBlockFragment;
    
    
    public byte[] getPadding() {
        return padding;
    }

    public int getPromisedStreamID() {
        return promisedStreamID;
    }

    public byte[] getHeaderBlockFragment() {
        return headerBlockFragment;
    }

    /* (non-Javadoc)
     * @see org.apache.mina.http2.api.Http2Frame#writePayload(java.nio.ByteBuffer)
     */
    @Override
    public void writePayload(ByteBuffer buffer) {
        if (isFlagSet(FLAGS_PADDING)) {
            buffer.put((byte) getPadding().length);
        }
        buffer.putInt(getPromisedStreamID());
        buffer.put(getHeaderBlockFragment());
        if (isFlagSet(FLAGS_PADDING)) {
            buffer.put(getPadding());
        }
    }

    protected Http2PushPromiseFrame(Http2PushPromiseFrameBuilder builder) {
        super(builder);
        this.padding = builder.getPadding();
        this.promisedStreamID = builder.getPromisedStreamID();
        this.headerBlockFragment = builder.getHeaderBlockFragment();
    }

    public static class Http2PushPromiseFrameBuilder extends AbstractHttp2FrameBuilder<Http2PushPromiseFrameBuilder,Http2PushPromiseFrame> {
        private byte[] padding = EMPTY_BYTE_ARRAY;
        
        private int promisedStreamID;
        
        private byte[] headerBlockFragment = EMPTY_BYTE_ARRAY;
        
        public Http2PushPromiseFrameBuilder padding(byte[] padding) {
            this.padding = padding;
            addFlag(FLAGS_PADDING);
            return this;
        }
        
        public byte[] getPadding() {
            return padding;
        }

        public Http2PushPromiseFrameBuilder promisedStreamID(int promisedStreamID) {
            this.promisedStreamID = promisedStreamID;
            return this;
        }
        
        public int getPromisedStreamID() {
            return promisedStreamID;
        }

        public Http2PushPromiseFrameBuilder headerBlockFragment(byte[] headerBlockFragment) {
            this.headerBlockFragment = headerBlockFragment;
            return this;
        }
        
        public byte[] getHeaderBlockFragment() {
            return headerBlockFragment;
        }

        @Override
        public Http2PushPromiseFrame build() {
            if (getLength() == (-1)) {
                int length = getHeaderBlockFragment().length + 4;
                if (isFlagSet(FLAGS_PADDING)) {
                    length += getPadding().length + 1;
                }
                setLength(length);
            }
            return new Http2PushPromiseFrame(type(FRAME_TYPE_PUSH_PROMISE));
        }
        
        public static Http2PushPromiseFrameBuilder builder() {
            return new Http2PushPromiseFrameBuilder();
        }
    }
}
