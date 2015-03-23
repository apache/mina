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
public class Http2GoAwayFrame extends Http2Frame {
    private final int lastStreamID;
    
    private final int errorCode;

    private byte[] data;
    
    public int getLastStreamID() {
        return lastStreamID;
    }
    
    public int getErrorCode() {
        return errorCode;
    }

    public byte[] getData() {
        return data;
    }
    
    protected <T extends AbstractHttp2GoAwayFrameBuilder<T,V>, V extends Http2GoAwayFrame> Http2GoAwayFrame(AbstractHttp2GoAwayFrameBuilder<T, V> builder) {
        super(builder);
        this.lastStreamID = builder.getLastStreamID();
        this.errorCode = builder.getErrorCode();
        this.data = builder.getData();
    }

    
    public static abstract class AbstractHttp2GoAwayFrameBuilder<T extends AbstractHttp2GoAwayFrameBuilder<T,V>, V extends Http2GoAwayFrame> extends AbstractHttp2FrameBuilder<T,V> {
        private int lastStreamID;
        
        private int errorCode;
        
        private byte[] data;
        
        @SuppressWarnings("unchecked")
        public T lastStreamID(int lastStreamID) {
            this.lastStreamID = lastStreamID;
            return (T) this;
        }
        
        public int getLastStreamID() {
            return lastStreamID;
        }

        @SuppressWarnings("unchecked")
        public T errorCode(int errorCode) {
            this.errorCode = errorCode;
            return (T) this;
        }
        
        public int getErrorCode() {
            return errorCode;
        }
        
        @SuppressWarnings("unchecked")
        public T data(byte[] data) {
            this.data = data;
            return (T) this;
        }
        
        public byte[] getData() {
            return data;
        }
    }
    
    public static class Builder extends AbstractHttp2GoAwayFrameBuilder<Builder, Http2GoAwayFrame> {

        @Override
        public Http2GoAwayFrame build() {
            return new Http2GoAwayFrame(this);
        }
        
        public static Builder builder() {
            return new Builder();
        }
    }
}
