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

import static org.apache.mina.http2.api.Http2Constants.FLAGS_PADDING;
import static org.apache.mina.http2.api.Http2Constants.FLAGS_PRIORITY;
import static org.apache.mina.http2.api.Http2Constants.FRAME_TYPE_HEADERS;
import static org.apache.mina.http2.api.Http2Constants.HTTP2_HEADER_LENGTH;
import static org.apache.mina.http2.api.Http2Constants.HTTP2_EXCLUSIVE_MASK;

import java.nio.ByteBuffer;

/**
 * An HTTP2 HEADERS frame
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public class Http2HeadersFrame extends Http2Frame {

    private final byte[] padding;
    
    private final int streamDependencyID;
    
    private final boolean exclusiveMode;
    
    private final short weight;
    
    private final byte[] headerBlockFragment;
    
    
    public byte[] getPadding() {
        return padding;
    }

    public int getStreamDependencyID() {
        return streamDependencyID;
    }
    
    public boolean getExclusiveMode() {
        return exclusiveMode;
    }

    public short getWeight() {
        return weight;
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
        if (isFlagSet(FLAGS_PRIORITY)) {
            buffer.putInt(getExclusiveMode()?HTTP2_EXCLUSIVE_MASK | getStreamDependencyID():getStreamDependencyID());
            buffer.put((byte) (getWeight() - 1));
        }
        buffer.put(getHeaderBlockFragment());
        if (isFlagSet(FLAGS_PADDING)) {
            buffer.put(getPadding());
        }
    }

    protected Http2HeadersFrame(Http2HeadersFrameBuilder builder) {
        super(builder);
        this.padding = builder.getPadding();
        this.streamDependencyID = builder.getStreamDependencyID();
        this.exclusiveMode = builder.getExclusiveMode();
        this.weight = builder.getWeight();
        this.headerBlockFragment = builder.getHeaderBlockFragment();
    }

    
    public static class Http2HeadersFrameBuilder extends AbstractHttp2FrameBuilder<Http2HeadersFrameBuilder,Http2HeadersFrame> {
        private byte[] padding;
        
        private int streamDependencyID;
        
        private short weight;
        
        private byte[] headerBlockFragment;
        
        private boolean exclusiveMode;
        
        public Http2HeadersFrameBuilder padding(byte[] padding) {
            this.padding = padding;
            addFlag(FLAGS_PADDING);
            return this;
        }
        
        public byte[] getPadding() {
            return padding;
        }

        public Http2HeadersFrameBuilder streamDependencyID(int streamDependencyID) {
            this.streamDependencyID = streamDependencyID;
            addFlag(FLAGS_PRIORITY);
            return this;
        }
        
        public int getStreamDependencyID() {
            return streamDependencyID;
        }

        public Http2HeadersFrameBuilder exclusiveMode(boolean exclusiveMode) {
            this.exclusiveMode = exclusiveMode;
            addFlag(FLAGS_PRIORITY);
            return this;
        }
        
        public boolean getExclusiveMode() {
            return exclusiveMode;
        }

        public Http2HeadersFrameBuilder weight(short weight) {
            this.weight = weight;
            addFlag(FLAGS_PRIORITY);
            return this;
        }
        
        public short getWeight() {
            return weight;
        }

        public Http2HeadersFrameBuilder headerBlockFragment(byte[] headerBlockFragment) {
            this.headerBlockFragment = headerBlockFragment;
            return this;
        }
        
        public byte[] getHeaderBlockFragment() {
            return headerBlockFragment;
        }

        @Override
        public Http2HeadersFrame build() {
            if (getLength() == (-1)) {
                int length = getHeaderBlockFragment().length;
                if (isFlagSet(FLAGS_PADDING)) {
                    length += getPadding().length + 1;
                }
                if (isFlagSet(FLAGS_PRIORITY)) {
                    length += 5;
                }
                length(length);
            }
            return new Http2HeadersFrame(type(FRAME_TYPE_HEADERS));
        }
        
        public static Http2HeadersFrameBuilder builder() {
            return new Http2HeadersFrameBuilder();
        }
    }
}
