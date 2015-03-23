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

    protected <T extends AbstractHttp2PushPromiseFrameBuilder<T,V>, V extends Http2PushPromiseFrame> Http2PushPromiseFrame(AbstractHttp2PushPromiseFrameBuilder<T, V> builder) {
        super(builder);
        this.padding = builder.getPadding();
        this.promisedStreamID = builder.getPromisedStreamID();
        this.headerBlockFragment = builder.getHeaderBlockFragment();
    }

    public static abstract class AbstractHttp2PushPromiseFrameBuilder<T extends AbstractHttp2PushPromiseFrameBuilder<T,V>, V extends Http2PushPromiseFrame> extends AbstractHttp2FrameBuilder<T,V> {
        private byte[] padding = EMPTY_BYTE_ARRAY;
        
        private int promisedStreamID;
        
        private byte[] headerBlockFragment = EMPTY_BYTE_ARRAY;
        
        @SuppressWarnings("unchecked")
        public T padding(byte[] padding) {
            this.padding = padding;
            return (T) this;
        }
        
        public byte[] getPadding() {
            return padding;
        }

        @SuppressWarnings("unchecked")
        public T promisedStreamID(int promisedStreamID) {
            this.promisedStreamID = promisedStreamID;
            return (T) this;
        }
        
        public int getPromisedStreamID() {
            return promisedStreamID;
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
    
    public static class Builder extends AbstractHttp2PushPromiseFrameBuilder<Builder, Http2PushPromiseFrame> {

        @Override
        public Http2PushPromiseFrame build() {
            return new Http2PushPromiseFrame(this);
        }
        
        public static Builder builder() {
            return new Builder();
        }
    }
}
