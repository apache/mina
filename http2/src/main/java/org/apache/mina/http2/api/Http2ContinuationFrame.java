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
public class Http2ContinuationFrame extends Http2Frame {

    private final byte[] headerBlockFragment;
    
    
    public byte[] getHeaderBlockFragment() {
        return headerBlockFragment;
    }

    protected <T extends AbstractHttp2ContinuationFrameBuilder<T,V>, V extends Http2ContinuationFrame> Http2ContinuationFrame(AbstractHttp2ContinuationFrameBuilder<T, V> builder) {
        super(builder);
        this.headerBlockFragment = builder.getHeaderBlockFragment();
    }

    
    public static abstract class AbstractHttp2ContinuationFrameBuilder<T extends AbstractHttp2ContinuationFrameBuilder<T,V>, V extends Http2ContinuationFrame> extends AbstractHttp2FrameBuilder<T,V> {
        private byte[] headerBlockFragment = new byte[0];
        
        @SuppressWarnings("unchecked")
        public T headerBlockFragment(byte[] headerBlockFragment) {
            this.headerBlockFragment = headerBlockFragment;
            return (T) this;
        }
        
        public byte[] getHeaderBlockFragment() {
            return headerBlockFragment;
        }
    }
    
    public static class Builder extends AbstractHttp2ContinuationFrameBuilder<Builder, Http2ContinuationFrame> {

        @Override
        public Http2ContinuationFrame build() {
            return new Http2ContinuationFrame(this);
        }
        
        public static Builder builder() {
            return new Builder();
        }
    }
}
