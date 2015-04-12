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
import static org.apache.mina.http2.api.Http2Constants.FLAGS_PADDING;
import static org.apache.mina.http2.api.Http2Constants.FRAME_TYPE_DATA;
import static org.apache.mina.http2.api.Http2Constants.HTTP2_HEADER_LENGTH;

import java.nio.ByteBuffer;

/**
 * An HTTP2 data frame
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
    
    /* (non-Javadoc)
     * @see org.apache.mina.http2.api.Http2Frame#writePayload(java.nio.ByteBuffer)
     */
    @Override
    public void writePayload(ByteBuffer buffer) {
        if (isFlagSet(FLAGS_PADDING)) {
            buffer.put((byte) getPadding().length);
        }
        buffer.put(getData());
        if (isFlagSet(FLAGS_PADDING)) {
            buffer.put(getPadding());
        }
    }

    protected Http2DataFrame(Http2DataFrameBuilder builder) {
        super(builder);
        data = builder.getData();
        padding = builder.getPadding();
    }

    public static class Http2DataFrameBuilder extends AbstractHttp2FrameBuilder<Http2DataFrameBuilder,Http2DataFrame> {
        private byte[] data = EMPTY_BYTE_ARRAY;
        
        private byte[] padding = EMPTY_BYTE_ARRAY;

        public Http2DataFrameBuilder data(byte[] data) {
            this.data = data;
            return this;
        }
        
        public byte[] getData() {
            return data;
        }

        public Http2DataFrameBuilder padding(byte[] padding) {
            this.padding = padding;
            addFlag(FLAGS_PADDING);
            return this;
        }
        
        public byte[] getPadding() {
            return padding;
        }

        @Override
        public Http2DataFrame build() {
            if (getLength() == (-1)) {
                int length = getData().length;
                if (isFlagSet(FLAGS_PADDING)) {
                    length += getPadding().length + 1;
                }
                length(length);
            }
            return new Http2DataFrame(type(FRAME_TYPE_DATA));
        }
        
        public static Http2DataFrameBuilder builder() {
            return new Http2DataFrameBuilder();
        }
    }
}
