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
public class Http2RstStreamFrame extends Http2Frame {
    private final long errorCode;
    
    public long getErrorCode() {
        return errorCode;
    }

    protected <T extends AbstractHttp2RstStreamFrameBuilder<T,V>, V extends Http2RstStreamFrame> Http2RstStreamFrame(AbstractHttp2RstStreamFrameBuilder<T, V> builder) {
        super(builder);
        this.errorCode = builder.getErrorCode();
    }

    
    public static abstract class AbstractHttp2RstStreamFrameBuilder<T extends AbstractHttp2RstStreamFrameBuilder<T,V>, V extends Http2RstStreamFrame> extends AbstractHttp2FrameBuilder<T,V> {
        private long errorCode;
        
        @SuppressWarnings("unchecked")
        public T errorCode(long errorCode) {
            this.errorCode = errorCode;
            return (T) this;
        }
        
        public long getErrorCode() {
            return errorCode;
        }
    }
    
    public static class Builder extends AbstractHttp2RstStreamFrameBuilder<Builder, Http2RstStreamFrame> {

        @Override
        public Http2RstStreamFrame build() {
            return new Http2RstStreamFrame(this);
        }
        
        public static Builder builder() {
            return new Builder();
        }
    }
}
