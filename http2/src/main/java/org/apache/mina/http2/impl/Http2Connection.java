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

import java.nio.ByteBuffer;

import org.apache.mina.http2.api.Http2Frame;
import org.apache.mina.http2.impl.Http2FrameHeadePartialDecoder.Http2FrameHeader;

import static org.apache.mina.http2.api.Http2Constants.FRAME_TYPE_DATA;
import static org.apache.mina.http2.api.Http2Constants.FRAME_TYPE_HEADERS;
import static org.apache.mina.http2.api.Http2Constants.FRAME_TYPE_PRIORITY;
import static org.apache.mina.http2.api.Http2Constants.FRAME_TYPE_RST_STREAM;
import static org.apache.mina.http2.api.Http2Constants.FRAME_TYPE_SETTINGS;
import static org.apache.mina.http2.api.Http2Constants.FRAME_TYPE_PUSH_PROMISE;
import static org.apache.mina.http2.api.Http2Constants.FRAME_TYPE_PING;
import static org.apache.mina.http2.api.Http2Constants.FRAME_TYPE_GOAWAY;
import static org.apache.mina.http2.api.Http2Constants.FRAME_TYPE_WINDOW_UPDATE;
import static org.apache.mina.http2.api.Http2Constants.FRAME_TYPE_CONTINUATION;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Http2Connection {

    private static enum DecoderState {
        HEADER,
        FRAME
    }
    
    private final Http2FrameHeadePartialDecoder headerDecoder = new Http2FrameHeadePartialDecoder();
    private Http2FrameDecoder frameDecoder;
    private DecoderState decoderState = DecoderState.HEADER;

    /**
     * Decode the incoming message and if all frame data has been received,
     * then the decoded frame will be returned. Otherwise, null is returned.
     * 
     * @param input the input buffer to decode
     * @return the decoder HTTP2 frame or null if more data are required
     */
    public Http2Frame decode(ByteBuffer input) {
        Http2Frame frame = null;
        switch (decoderState) {
        case HEADER:
            if (headerDecoder.consume(input)) {
                Http2FrameHeader header = headerDecoder.getValue();
                headerDecoder.reset();
                decoderState = DecoderState.FRAME;
                switch (header.getType()) {
                case FRAME_TYPE_DATA:
                    frameDecoder = new Http2DataFrameDecoder(header);
                    break;
                case FRAME_TYPE_HEADERS:
                    frameDecoder = new Http2HeadersFrameDecoder(header);
                    break;
                case FRAME_TYPE_PRIORITY:
                    frameDecoder = new Http2PriorityFrameDecoder(header);
                    break;
                case FRAME_TYPE_RST_STREAM:
                    frameDecoder = new Http2RstStreamFrameDecoder(header);
                    break;
                case FRAME_TYPE_SETTINGS:
                    frameDecoder =new Http2SettingsFrameDecoder(header);
                    break;
                case FRAME_TYPE_PUSH_PROMISE:
                    frameDecoder = new Http2PushPromiseFrameDecoder(header);
                    break;
                case FRAME_TYPE_PING:
                    frameDecoder = new Http2PingFrameDecoder(header);
                    break;
                case FRAME_TYPE_GOAWAY:
                    frameDecoder = new Http2GoAwayFrameDecoder(header);
                    break;
                case FRAME_TYPE_WINDOW_UPDATE:
                    frameDecoder = new Http2WindowUpdateFrameDecoder(header);
                    break;
                case FRAME_TYPE_CONTINUATION:
                    frameDecoder = new Http2ContinuationFrameDecoder(header);
                    break;
                default:
                    frameDecoder = new Http2UnknownFrameDecoder(header);
                    break;
                }
                if (frameDecoder.consume(input)) {
                    frame = frameDecoder.getValue();
                    decoderState = DecoderState.HEADER;
                }
            }
            break;
        case FRAME:
            if (frameDecoder.consume(input)) {
                frame = frameDecoder.getValue();
                decoderState = DecoderState.HEADER;
            }
            break;
        }
        return frame;
    }
}
