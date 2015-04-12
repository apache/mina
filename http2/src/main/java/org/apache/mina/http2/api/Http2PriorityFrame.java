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

import static org.apache.mina.http2.api.Http2Constants.FRAME_TYPE_PRIORITY;
import static org.apache.mina.http2.api.Http2Constants.HTTP2_EXCLUSIVE_MASK;

import java.nio.ByteBuffer;

/**
 * An HTTP2 PRIORITY frame.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public class Http2PriorityFrame extends Http2Frame {
    private final int streamDependencyID;
    
    private boolean exclusiveMode;
    
    private final short weight;
    
    public int getStreamDependencyID() {
        return streamDependencyID;
    }
    
    public boolean getExclusiveMode() {
        return exclusiveMode;
    }

    public short getWeight() {
        return weight;
    }

    /* (non-Javadoc)
     * @see org.apache.mina.http2.api.Http2Frame#writePayload(java.nio.ByteBuffer)
     */
    @Override
    public void writePayload(ByteBuffer buffer) {
        buffer.putInt(getExclusiveMode()?HTTP2_EXCLUSIVE_MASK | getStreamDependencyID():getStreamDependencyID());
        buffer.put((byte) (getWeight() - 1));
    }

    protected Http2PriorityFrame(Http2PriorityFrameBuilder builder) {
        super(builder);
        this.streamDependencyID = builder.getStreamDependencyID();
        this.exclusiveMode = builder.exclusiveMode;
        this.weight = builder.getWeight();
    }

    
    public static class Http2PriorityFrameBuilder extends AbstractHttp2FrameBuilder<Http2PriorityFrameBuilder,Http2PriorityFrame> {
        private int streamDependencyID;
        
        private boolean exclusiveMode;
        
        private short weight;
        
        public Http2PriorityFrameBuilder streamDependencyID(int streamDependencyID) {
            this.streamDependencyID = streamDependencyID;
            return this;
        }
        
        public int getStreamDependencyID() {
            return streamDependencyID;
        }
        
        public Http2PriorityFrameBuilder exclusiveMode(boolean exclusiveMode) {
            this.exclusiveMode = exclusiveMode;
            return this;
        }
        
        public boolean getExclusiveMode() {
            return exclusiveMode;
        }

        public Http2PriorityFrameBuilder weight(short weight) {
            this.weight = weight;
            return this;
        }
        
        public short getWeight() {
            return weight;
        }

        @Override
        public Http2PriorityFrame build() {
            if (getLength() == (-1)) {
                setLength(5);
            }
            return new Http2PriorityFrame(type(FRAME_TYPE_PRIORITY));
        }
        
        public static Http2PriorityFrameBuilder builder() {
            return new Http2PriorityFrameBuilder();
        }
    }
 }
