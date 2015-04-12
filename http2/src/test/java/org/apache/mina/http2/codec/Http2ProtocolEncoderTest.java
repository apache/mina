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
package org.apache.mina.http2.codec;

import static org.junit.Assert.assertArrayEquals;

import org.apache.mina.http2.Http2Test;
import org.apache.mina.http2.TestMessages;
import org.apache.mina.http2.api.Http2Frame;
import org.junit.Test;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */

public class Http2ProtocolEncoderTest extends Http2Test {

    private Http2ProtocolEncoder encoder = new Http2ProtocolEncoder();
    
    @Test
    public void continuationNoHeaderBlock() {
        Http2Frame frame = TestMessages.CONTINUATION_NO_HEADER_FRAGMENT_FRAME;
        assertArrayEquals(TestMessages.CONTINUATION_NO_HEADER_FRAGMENT_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void continuationHeaderBlock() {
        Http2Frame frame = TestMessages.CONTINUATION_HEADER_FRAGMENT_FRAME;
        assertArrayEquals(TestMessages.CONTINUATION_HEADER_FRAGMENT_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    

    @Test
    public void dataNoPayloadNoPadding() {
        Http2Frame frame = TestMessages.DATA_NO_PAYLOAD_NO_PADDING_FRAME;
        assertArrayEquals(TestMessages.DATA_NO_PAYLOAD_NO_PADDING_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void dataPayloadNoPadding() {
        Http2Frame frame = TestMessages.DATA_PAYLOAD_NO_PADDING_FRAME;
        assertArrayEquals(TestMessages.DATA_PAYLOAD_NO_PADDING_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void dataNoPayloadPadding() {
        Http2Frame frame = TestMessages.DATA_NO_PAYLOAD_PADDING_FRAME;
        assertArrayEquals(TestMessages.DATA_NO_PAYLOAD_PADDING_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void dataPayloadPadding() {
        Http2Frame frame = TestMessages.DATA_PAYLOAD_PADDING_FRAME;
        assertArrayEquals(TestMessages.DATA_PAYLOAD_PADDING_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void goAwayNoAdditionalData() {
        Http2Frame frame = TestMessages.GOAWAY_NO_DATA_FRAME;
        assertArrayEquals(TestMessages.GOAWAY_NO_DATA_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void goAwayHighestLastStreamIDNoAdditionalData() {
        Http2Frame frame = TestMessages.GOAWAY_NO_DATA_HIGHEST_STREAMID_FRAME;
        assertArrayEquals(TestMessages.GOAWAY_NO_DATA_HIGHEST_STREAMID_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void goAwayHighestErrorCodeNoAdditionalData() {
        Http2Frame frame = TestMessages.GOAWAY_NO_DATA_HIGHEST_ERROR_CODE_FRAME;
        assertArrayEquals(TestMessages.GOAWAY_NO_DATA_HIGHEST_ERROR_CODE_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void goAwayAdditionalData() {
        Http2Frame frame = TestMessages.GOAWAY_DATA_FRAME;
        assertArrayEquals(TestMessages.GOAWAY_DATA_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void headersNoPaddingNoPriority() {
        Http2Frame frame = TestMessages.HEADERS_NO_PADDING_NO_PRIORITY_FRAME;
        assertArrayEquals(TestMessages.HEADERS_NO_PADDING_NO_PRIORITY_BUFFER,  toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void headersPaddingPriority() {
        Http2Frame frame = TestMessages.HEADERS_PADDING_PRIORITY_FRAME;
        assertArrayEquals(TestMessages.HEADERS_PADDING_PRIORITY_BUFFER,  toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void headersPaddingNoPriority() {
        Http2Frame frame = TestMessages.HEADERS_PADDING_NO_PRIORITY_FRAME;
        assertArrayEquals(TestMessages.HEADERS_PADDING_NO_PRIORITY_BUFFER,  toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void ping() {
        Http2Frame frame = TestMessages.PING_STANDARD_FRAME;
        assertArrayEquals(TestMessages.PING_STANDARD_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void pingExtraData() {
        Http2Frame frame = TestMessages.PING_EXTRA_DATA_FRAME;
        assertArrayEquals(TestMessages.PING_EXTRA_DATA_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void pingNotEnoughData() {
        Http2Frame frame = TestMessages.PING_NO_ENOUGH_DATA_FRAME;
        assertArrayEquals(TestMessages.PING_NO_ENOUGH_DATA_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void priorityNoExclusive() {
        Http2Frame frame = TestMessages.PRIORITY_NO_EXCLUSIVE_MODE_FRAME;
        assertArrayEquals(TestMessages.PRIORITY_NO_EXCLUSIVE_MODE_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
  
    @Test
    public void priorityExclusive() {
        Http2Frame frame = TestMessages.PRIORITY_EXCLUSIVE_MODE_FRAME;
        assertArrayEquals(TestMessages.PRIORITY_EXCLUSIVE_MODE_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void pushPromiseNoPadding() {
        Http2Frame frame = TestMessages.PUSH_PROMISE_NO_PADDING_FRAME;
        assertArrayEquals(TestMessages.PUSH_PROMISE_NO_PADDING_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void pushPromisePadding() {
        Http2Frame frame = TestMessages.PUSH_PROMISE_PADDING_FRAME;
        assertArrayEquals(TestMessages.PUSH_PROMISE_PADDING_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void rstStreamNoExtraPayload() {
        Http2Frame frame = TestMessages.RST_STREAM_NO_EXTRA_PAYLOAD_FRAME;
        assertArrayEquals(TestMessages.RST_STREAM_NO_EXTRA_PAYLOAD_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void rstStreamHighestValueNoExtraPayload() {
        Http2Frame frame = TestMessages.RST_STREAM_HIGHEST_VALUE_NO_EXTRA_PAYLOAD_FRAME;
        assertArrayEquals(TestMessages.RST_STREAM_HIGHEST_VALUE_NO_EXTRA_PAYLOAD_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void settings() {
        Http2Frame frame = TestMessages.SETTINGS_DEFAULT_FRAME;
        assertArrayEquals(TestMessages.SETTINGS_DEFAULT_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void settingsHighestID() {
        Http2Frame frame = TestMessages.SETTINGS_HIGHEST_ID_FRAME;
        assertArrayEquals(TestMessages.SETTINGS_HIGHEST_ID_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void settingsHighestValue() {
        Http2Frame frame = TestMessages.SETTINGS_HIGHEST_VALUE_FRAME;
        assertArrayEquals(TestMessages.SETTINGS_HIGHEST_VALUE_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void unknownFrame() {
        Http2Frame frame = TestMessages.UNKNOWN_PAYLOAD_FRAME;
        assertArrayEquals(TestMessages.UNKNOWN_PAYLOAD_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
    
    @Test
    public void unknownFrameNoPayload() {
        Http2Frame frame = TestMessages.UNKNOWN_NO_PAYLOAD_FRAME;
        assertArrayEquals(TestMessages.UNKNOWN_NO_PAYLOAD_BUFFER, toByteArray(encoder.encode(frame, null)));
    }
}
