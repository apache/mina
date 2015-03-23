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
public class Http2WindowUpdateFrame extends Http2Frame {
    private final int windowUpdateIncrement;
    
    public int getWindowUpdateIncrement() {
        return windowUpdateIncrement;
    }

    protected <T extends AbstractHttp2WindowUpdateFrameBuilder<T,V>, V extends Http2WindowUpdateFrame> Http2WindowUpdateFrame(AbstractHttp2WindowUpdateFrameBuilder<T, V> builder) {
        super(builder);
        this.windowUpdateIncrement = builder.getWindowUpdateIncrement();
    }

    
    public static abstract class AbstractHttp2WindowUpdateFrameBuilder<T extends AbstractHttp2WindowUpdateFrameBuilder<T,V>, V extends Http2WindowUpdateFrame> extends AbstractHttp2FrameBuilder<T,V> {
        private int windowUpdateIncrement;
        
        @SuppressWarnings("unchecked")
        public T windowUpdateIncrement(int windowUpdateIncrement) {
            this.windowUpdateIncrement = windowUpdateIncrement;
            return (T) this;
        }
        
        public int getWindowUpdateIncrement() {
            return windowUpdateIncrement;
        }
    }
    
    public static class Builder extends AbstractHttp2WindowUpdateFrameBuilder<Builder, Http2WindowUpdateFrame> {

        @Override
        public Http2WindowUpdateFrame build() {
            return new Http2WindowUpdateFrame(this);
        }
        
        public static Builder builder() {
            return new Builder();
        }
    }
}
