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
public class Http2PingFrame extends Http2Frame {
    private final byte[] data;
    
    public byte[] getData() {
        return data;
    }

    protected <T extends AbstractHttp2PingFrameBuilder<T,V>, V extends Http2PingFrame> Http2PingFrame(AbstractHttp2PingFrameBuilder<T, V> builder) {
        super(builder);
        this.data = builder.getData();
    }

    
    public static abstract class AbstractHttp2PingFrameBuilder<T extends AbstractHttp2PingFrameBuilder<T,V>, V extends Http2PingFrame> extends AbstractHttp2FrameBuilder<T,V> {
        private byte[] data;
        
        @SuppressWarnings("unchecked")
        public T data(byte[] data) {
            this.data = data;
            return (T) this;
        }
        
        public byte[] getData() {
            return data;
        }
    }
    
    public static class Builder extends AbstractHttp2PingFrameBuilder<Builder, Http2PingFrame> {

        @Override
        public Http2PingFrame build() {
            return new Http2PingFrame(this);
        }
        
        public static Builder builder() {
            return new Builder();
        }
    }
}
