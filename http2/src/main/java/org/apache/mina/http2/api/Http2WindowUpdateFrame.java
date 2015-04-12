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

import static org.apache.mina.http2.api.Http2Constants.FRAME_TYPE_WINDOW_UPDATE;

import java.nio.ByteBuffer;

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

    /* (non-Javadoc)
     * @see org.apache.mina.http2.api.Http2Frame#writePayload(java.nio.ByteBuffer)
     */
    @Override
    public void writePayload(ByteBuffer buffer) {
        buffer.putInt(getWindowUpdateIncrement());
    }

    protected Http2WindowUpdateFrame(Http2WindowUpdateFrameBuilder builder) {
        super(builder);
        this.windowUpdateIncrement = builder.getWindowUpdateIncrement();
    }

    
    public static class Http2WindowUpdateFrameBuilder extends AbstractHttp2FrameBuilder<Http2WindowUpdateFrameBuilder,Http2WindowUpdateFrame> {
        private int windowUpdateIncrement;
        
        public Http2WindowUpdateFrameBuilder windowUpdateIncrement(int windowUpdateIncrement) {
            this.windowUpdateIncrement = windowUpdateIncrement;
            return this;
        }
        
        public int getWindowUpdateIncrement() {
            return windowUpdateIncrement;
        }

        @Override
        public Http2WindowUpdateFrame build() {
            if (getLength() == (-1)) {
                setLength(4);
            }
            return new Http2WindowUpdateFrame(type(FRAME_TYPE_WINDOW_UPDATE));
        }
        
        public static Http2WindowUpdateFrameBuilder builder() {
            return new Http2WindowUpdateFrameBuilder();
        }
    }
}
