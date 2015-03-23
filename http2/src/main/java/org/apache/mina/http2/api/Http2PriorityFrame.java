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

/**
 * An SPY data frame
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

    protected <T extends AbstractHttp2PriorityFrameBuilder<T,V>, V extends Http2PriorityFrame> Http2PriorityFrame(AbstractHttp2PriorityFrameBuilder<T, V> builder) {
        super(builder);
        this.streamDependencyID = builder.getStreamDependencyID();
        this.exclusiveMode = builder.exclusiveMode;
        this.weight = builder.getWeight();
    }

    
    public static abstract class AbstractHttp2PriorityFrameBuilder<T extends AbstractHttp2PriorityFrameBuilder<T,V>, V extends Http2PriorityFrame> extends AbstractHttp2FrameBuilder<T,V> {
        private int streamDependencyID;
        
        private boolean exclusiveMode;
        
        private short weight;
        
        @SuppressWarnings("unchecked")
        public T streamDependencyID(int streamDependencyID) {
            this.streamDependencyID = streamDependencyID;
            return (T) this;
        }
        
        public int getStreamDependencyID() {
            return streamDependencyID;
        }
        
        @SuppressWarnings("unchecked")
        public T exclusiveMode(boolean exclusiveMode) {
            this.exclusiveMode = exclusiveMode;
            return (T) this;
        }
        
        public boolean getExclusiveMode() {
            return exclusiveMode;
        }


        @SuppressWarnings("unchecked")
        public T weight(short weight) {
            this.weight = weight;
            return (T) this;
        }
        
        public short getWeight() {
            return weight;
        }
    }
    
    public static class Builder extends AbstractHttp2PriorityFrameBuilder<Builder, Http2PriorityFrame> {

        @Override
        public Http2PriorityFrame build() {
            return new Http2PriorityFrame(this);
        }
        
        public static Builder builder() {
            return new Builder();
        }
    }
}
