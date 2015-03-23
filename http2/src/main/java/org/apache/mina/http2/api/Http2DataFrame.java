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
public class Http2DataFrame extends Http2Frame {
    private final byte[] data;
    
    private final byte[] padding;
    
    public byte[] getData() {
        return data;
    }
    
    public byte[] getPadding() {
        return padding;
    }
    
    protected <T extends AbstractHttp2DataFrameBuilder<T,V>, V extends Http2Frame> Http2DataFrame(AbstractHttp2DataFrameBuilder<T, V> builder) {
        super(builder);
        this.data = builder.getData();
        this.padding = builder.getPadding();
    }

    
    public static abstract class AbstractHttp2DataFrameBuilder<T extends AbstractHttp2DataFrameBuilder<T,V>, V extends Http2Frame> extends AbstractHttp2FrameBuilder<T,V> {
        private byte[] data = EMPTY_BYTE_ARRAY;
        
        private byte[] padding = EMPTY_BYTE_ARRAY;

        @SuppressWarnings("unchecked")
        public T data(byte[] data) {
            this.data = data;
            return (T) this;
        }
        
        public byte[] getData() {
            return data;
        }

        @SuppressWarnings("unchecked")
        public T padding(byte[] padding) {
            this.padding = padding;
            return (T) this;
        }
        
        public byte[] getPadding() {
            return padding;
        }
}
    
    public static class Builder extends AbstractHttp2DataFrameBuilder<Builder, Http2Frame> {

        @Override
        public Http2Frame build() {
            return new Http2DataFrame(this);
        }
        
        public static Builder builder() {
            return new Builder();
        }
    }
}
