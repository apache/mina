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
package org.apache.mina.http2.impl;

import org.apache.mina.http2.api.Http2Frame;
import org.apache.mina.http2.api.Http2Frame.AbstractHttp2FrameBuilder;
import org.apache.mina.http2.impl.Http2FrameHeadePartialDecoder.Http2FrameHeader;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class Http2FrameDecoder implements PartialDecoder<Http2Frame> {
    private Http2FrameHeader header;
    
    private Http2Frame frame;

    public Http2FrameDecoder(Http2FrameHeader header) {
        this.header = header;
    }
    
    protected boolean isFlagSet(short mask) {
        return (header.getFlags() & mask) == mask;
    }
    
    protected void initBuilder(AbstractHttp2FrameBuilder builder) {
        builder.length(header.getLength());
        builder.type(header.getType());
        builder.flags(header.getFlags());
        builder.streamID(header.getStreamID());
    }
    
    protected Http2FrameHeader getHeader() {
        return header;
    }
    
    @Override
    public Http2Frame getValue() {
        return frame;
    }
    
    protected void setValue(Http2Frame frame) {
        this.frame = frame;
    }

    @Override
    public void reset() {
    }
    
}
