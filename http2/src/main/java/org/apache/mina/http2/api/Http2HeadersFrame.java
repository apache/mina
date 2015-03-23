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
public class Http2HeadersFrame extends Http2Frame {

    private final byte[] padding;
    
    private final int streamDependencyID;
    
    private final boolean exclusiveMode;
    
    private final byte weight;
    
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

    public byte getWeight() {
        return weight;
    }

    public byte[] getHeaderBlockFragment() {
        return headerBlockFragment;
    }

    protected <T extends AbstractHttp2HeadersFrameBuilder<T,V>, V extends Http2HeadersFrame> Http2HeadersFrame(AbstractHttp2HeadersFrameBuilder<T, V> builder) {
        super(builder);
        this.padding = builder.getPadding();
        this.streamDependencyID = builder.getStreamDependencyID();
        this.exclusiveMode = builder.getExclusiveMode();
        this.weight = builder.getWeight();
        this.headerBlockFragment = builder.getHeaderBlockFragment();
    }

    
    public static abstract class AbstractHttp2HeadersFrameBuilder<T extends AbstractHttp2HeadersFrameBuilder<T,V>, V extends Http2HeadersFrame> extends AbstractHttp2FrameBuilder<T,V> {
        private byte[] padding;
        
        private int streamDependencyID;
        
        private byte weight;
        
        private byte[] headerBlockFragment;
        
        private boolean exclusiveMode;
        
        @SuppressWarnings("unchecked")
        public T padding(byte[] padding) {
            this.padding = padding;
            return (T) this;
        }
        
        public byte[] getPadding() {
            return padding;
        }

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
        public T weight(byte weight) {
            this.weight = weight;
            return (T) this;
        }
        
        public byte getWeight() {
            return weight;
        }

        @SuppressWarnings("unchecked")
        public T headerBlockFragment(byte[] headerBlockFragment) {
            this.headerBlockFragment = headerBlockFragment;
            return (T) this;
        }
        
        public byte[] getHeaderBlockFragment() {
            return headerBlockFragment;
        }
    }
    
    public static class Builder extends AbstractHttp2HeadersFrameBuilder<Builder, Http2HeadersFrame> {

        @Override
        public Http2HeadersFrame build() {
            return new Http2HeadersFrame(this);
        }
        
        public static Builder builder() {
            return new Builder();
        }

    }
}
