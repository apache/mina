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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.apache.mina.http2.Http2Test;
import org.apache.mina.http2.TestMessages;
import org.apache.mina.http2.api.Http2ContinuationFrame;
import org.apache.mina.http2.api.Http2DataFrame;
import org.apache.mina.http2.api.Http2Frame;
import org.apache.mina.http2.api.Http2GoAwayFrame;
import org.apache.mina.http2.api.Http2HeadersFrame;
import org.apache.mina.http2.api.Http2PingFrame;
import org.apache.mina.http2.api.Http2PriorityFrame;
import org.apache.mina.http2.api.Http2PushPromiseFrame;
import org.apache.mina.http2.api.Http2RstStreamFrame;
import org.apache.mina.http2.api.Http2Setting;
import org.apache.mina.http2.api.Http2SettingsFrame;
import org.apache.mina.http2.api.Http2UnknownFrame;
import org.apache.mina.http2.impl.Http2Connection;
import org.junit.Test;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */

public class Http2ProtocolDecoderTest extends Http2Test {

    private Http2ProtocolDecoder decoder = new Http2ProtocolDecoder();
    private Http2Connection context = new Http2Connection();
    
    @Test
    public void continuationNoHeaderBlock() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.CONTINUATION_NO_HEADER_FRAGMENT_BUFFER);
        Http2ContinuationFrame frame = (Http2ContinuationFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(0, frame.getLength());
        assertEquals(9, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(50, frame.getStreamID());
        assertEquals(0, frame.getHeaderBlockFragment().length);
    }

    @Test
    public void continuationHeaderBlock() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.CONTINUATION_HEADER_FRAGMENT_BUFFER);
        Http2ContinuationFrame frame = (Http2ContinuationFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(10, frame.getLength());
        assertEquals(9, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(50, frame.getStreamID());
        assertEquals(10, frame.getHeaderBlockFragment().length);
        assertArrayEquals(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A}, frame.getHeaderBlockFragment());
    }
    
    @Test
    public void dataNoPayloadNoPadding() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.DATA_NO_PAYLOAD_NO_PADDING_BUFFER);
        Http2DataFrame frame = (Http2DataFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(0, frame.getLength());
        assertEquals(0, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(50, frame.getStreamID());
        assertEquals(0, frame.getData().length);
        assertEquals(0, frame.getPadding().length);
    }
    
    @Test
    public void dataPayloadNoPadding() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.DATA_PAYLOAD_NO_PADDING_BUFFER);
        Http2DataFrame frame = (Http2DataFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(10, frame.getLength());
        assertEquals(0, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(50, frame.getStreamID());
        assertEquals(10, frame.getData().length);
        assertArrayEquals(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A}, frame.getData());
        assertEquals(0, frame.getPadding().length);
    }
    
    @Test
    public void dataNoPayloadPadding() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.DATA_NO_PAYLOAD_PADDING_BUFFER);
        Http2DataFrame frame = (Http2DataFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(3, frame.getLength());
        assertEquals(0, frame.getType());
        assertEquals(0x08, frame.getFlags());
        assertEquals(50, frame.getStreamID());
        assertEquals(0,frame.getData().length);
        assertEquals(2, frame.getPadding().length);
        assertArrayEquals(new byte[] {0x0E,  0x28}, frame.getPadding());
    }
    
    @Test
    public void dataPayloadPadding() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.DATA_PAYLOAD_PADDING_BUFFER);
        Http2DataFrame frame = (Http2DataFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(13, frame.getLength());
        assertEquals(0, frame.getType());
        assertEquals(0x08, frame.getFlags());
        assertEquals(50, frame.getStreamID());
        assertEquals(10, frame.getData().length);
        assertArrayEquals(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A}, frame.getData());
        assertEquals(2, frame.getPadding().length);
        assertArrayEquals(new byte[] {0x0E, 0x28}, frame.getPadding());
    }
    
    @Test
    public void goAwayNoAdditionalData() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.GOAWAY_NO_DATA_BUFFER);
        Http2GoAwayFrame frame = (Http2GoAwayFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(8, frame.getLength());
        assertEquals(7, frame.getType());
        assertEquals(0, frame.getFlags());
        assertEquals(1, frame.getStreamID());
        assertEquals(256, frame.getLastStreamID());
        assertEquals(0x010203, frame.getErrorCode());
    }
    
    @Test
    public void goAwayHighestLastStreamIDNoAdditionalData() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.GOAWAY_NO_DATA_HIGHEST_STREAMID_BUFFER);
        Http2GoAwayFrame frame = (Http2GoAwayFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(8, frame.getLength());
        assertEquals(7, frame.getType());
        assertEquals(0, frame.getFlags());
        assertEquals(1, frame.getStreamID());
        assertEquals(0x7FFFFFFF, frame.getLastStreamID());
        assertEquals(0x010203, frame.getErrorCode());
    }
    
    @Test
    public void goAwayHighestLastStreamIDReservedBitSetNoAdditionalData() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.GOAWAY_NO_DATA_HIGHEST_STREAMID_RESERVED_BIT_BUFFER);
        Http2GoAwayFrame frame = (Http2GoAwayFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(8, frame.getLength());
        assertEquals(7, frame.getType());
        assertEquals(0, frame.getFlags());
        assertEquals(1, frame.getStreamID());
        assertEquals(0x7FFFFFFF, frame.getLastStreamID());
        assertEquals(0x010203, frame.getErrorCode());
    }
    
    @Test
    public void goAwayHighestErrorCodeNoAdditionalData() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.GOAWAY_NO_DATA_HIGHEST_ERROR_CODE_BUFFER);
        Http2GoAwayFrame frame = (Http2GoAwayFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(8, frame.getLength());
        assertEquals(7, frame.getType());
        assertEquals(0, frame.getFlags());
        assertEquals(1, frame.getStreamID());
        assertEquals(0x7FFFFFFF, frame.getLastStreamID());
        assertEquals(0x00FFFFFFFFL, frame.getErrorCode());
    }
    
    @Test
    public void goAwayAdditionalData() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.GOAWAY_DATA_BUFFER);
        Http2GoAwayFrame frame = (Http2GoAwayFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(9, frame.getLength());
        assertEquals(7, frame.getType());
        assertEquals(0, frame.getFlags());
        assertEquals(1, frame.getStreamID());
        assertEquals(256, frame.getLastStreamID());
        assertEquals(0x00010203, frame.getErrorCode());
        assertArrayEquals(new byte[] {0x01}, frame.getData());
    }
    
    @Test
    public void headersNoPaddingNoPriority() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.HEADERS_NO_PADDING_NO_PRIORITY_BUFFER);
        Http2HeadersFrame frame = (Http2HeadersFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(1, frame.getLength());
        assertEquals(1, frame.getType());
        assertEquals(0, frame.getFlags());
        assertEquals(1, frame.getStreamID());
        assertEquals(1, frame.getHeaderBlockFragment().length);
        assertEquals(0x0082, frame.getHeaderBlockFragment()[0] & 0x00FF);
    }
    
    @Test
    public void headersPaddingPriority() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.HEADERS_PADDING_PRIORITY_BUFFER);
        Http2HeadersFrame frame = (Http2HeadersFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(23, frame.getLength());
        assertEquals(1, frame.getType());
        assertEquals(0x28, frame.getFlags());
        assertEquals(3, frame.getStreamID());
        assertEquals(10,  frame.getWeight());
        assertEquals(1, frame.getHeaderBlockFragment().length);
        assertEquals(0x0082, frame.getHeaderBlockFragment()[0] & 0x00FF);
        assertEquals(16,  frame.getPadding().length);
        assertArrayEquals(new byte[] {0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x70, 0x61, 0x64, 0x64, 0x69, 0x6E, 0x67, 0x2E}, frame.getPadding());
    }
    
    @Test
    public void headersPaddingNoPriority() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.HEADERS_PADDING_NO_PRIORITY_BUFFER);
        Http2HeadersFrame frame = (Http2HeadersFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(18, frame.getLength());
        assertEquals(1, frame.getType());
        assertEquals(0x08, frame.getFlags());
        assertEquals(3, frame.getStreamID());
        assertEquals(1, frame.getHeaderBlockFragment().length);
        assertEquals(0x0082, frame.getHeaderBlockFragment()[0] & 0x00FF);
        assertEquals(16,  frame.getPadding().length);
        assertArrayEquals(new byte[] {0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x70, 0x61, 0x64, 0x64, 0x69, 0x6E, 0x67, 0x2E}, frame.getPadding());
    }
    
    @Test
    public void ping() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.PING_STANDARD_BUFFER);
        Http2PingFrame frame = (Http2PingFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(8, frame.getLength());
        assertEquals(6, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertArrayEquals(new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07}, frame.getData());
    }
    
    @Test
    public void pingExtraData() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.PING_EXTRA_DATA_BUFFER);
        Http2PingFrame frame = (Http2PingFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(9, frame.getLength());
        assertEquals(6, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertArrayEquals(new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08}, frame.getData());
    }
    
    @Test
    public void pingNotEnoughData() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.PING_NO_ENOUGH_DATA_BUFFER);
        Http2PingFrame frame = (Http2PingFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(1, frame.getLength());
        assertEquals(6, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertArrayEquals(new byte[] {0x00}, frame.getData());
    }
    
    @Test
    public void priorityNoExclusive() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.PRIORITY_NO_EXCLUSIVE_MODE_BUFFER);
        Http2PriorityFrame frame = (Http2PriorityFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(5, frame.getLength());
        assertEquals(2, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(256, frame.getStreamDependencyID());
        assertFalse(frame.getExclusiveMode());
        assertEquals(2, frame.getWeight());
    }
  
    @Test
    public void priorityExclusive() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.PRIORITY_EXCLUSIVE_MODE_BUFFER);
        Http2PriorityFrame frame = (Http2PriorityFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(5, frame.getLength());
        assertEquals(2, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(256, frame.getStreamDependencyID());
        assertTrue(frame.getExclusiveMode());
        assertEquals(2, frame.getWeight());
    }
    
    @Test
    public void pushPromiseNoPadding() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.PUSH_PROMISE_NO_PADDING_BUFFER);
        Http2PushPromiseFrame frame = (Http2PushPromiseFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(5, frame.getLength());
        assertEquals(5, frame.getType());
        assertEquals(0, frame.getFlags());
        assertEquals(1, frame.getStreamID());
        assertEquals(256, frame.getPromisedStreamID());
        assertEquals(1, frame.getHeaderBlockFragment().length);
        assertEquals(0x0082, frame.getHeaderBlockFragment()[0] & 0x00FF);
    }
    
    @Test
    public void pushPromisePadding() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.PUSH_PROMISE_PADDING_BUFFER);
        Http2PushPromiseFrame frame = (Http2PushPromiseFrame) decoder.decode(buffer, context);
        assertEquals(22, frame.getLength());
        assertEquals(5, frame.getType());
        assertEquals(0x08, frame.getFlags());
        assertEquals(3, frame.getStreamID());
        assertEquals(20, frame.getPromisedStreamID());
        assertEquals(1, frame.getHeaderBlockFragment().length);
        assertEquals(0x0082, frame.getHeaderBlockFragment()[0] & 0x00FF);
        assertEquals(16,  frame.getPadding().length);
        assertArrayEquals(new byte[] {0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x70, 0x61, 0x64, 0x64, 0x69, 0x6E, 0x67, 0x2E}, frame.getPadding());
    }
    
    @Test
    public void rstStreamNoExtraPayload() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.RST_STREAM_NO_EXTRA_PAYLOAD_BUFFER);
        Http2RstStreamFrame frame = (Http2RstStreamFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(4, frame.getLength());
        assertEquals(3, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(256, frame.getErrorCode());
    }
    
    @Test
    public void rstStreamHighestValueNoExtraPayload() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.RST_STREAM_HIGHEST_VALUE_NO_EXTRA_PAYLOAD_BUFFER);
        Http2RstStreamFrame frame = (Http2RstStreamFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(4, frame.getLength());
        assertEquals(3, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(0x00FFFFFFFFL, frame.getErrorCode());
    }
    
    @Test
    public void rstStreamExtraPayload() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.RST_STREAM_EXTRA_PAYLOAD_BUFFER);
        Http2RstStreamFrame frame = (Http2RstStreamFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(6, frame.getLength());
        assertEquals(3, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(256, frame.getErrorCode());
    }
    
    @Test
    public void rstStreamHighestValueExtraPayload() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.RST_STREAM_EXTRA_PAYLOAD_HIGHEST_BUFFER);
        Http2RstStreamFrame frame = (Http2RstStreamFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(6, frame.getLength());
        assertEquals(3, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(0x00FFFFFFFFL, frame.getErrorCode());
    }
    
    @Test
    public void settings() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.SETTINGS_DEFAULT_BUFFER);
        Http2SettingsFrame frame = (Http2SettingsFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(6, frame.getLength());
        assertEquals(4, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(1, frame.getSettings().size());
        Http2Setting setting = frame.getSettings().iterator().next();
        assertEquals(1, setting.getID());
        assertEquals(0x01020304L, setting.getValue());
    }
    
    @Test
    public void settingsHighestID() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.SETTINGS_HIGHEST_ID_BUFFER);
        Http2SettingsFrame frame = (Http2SettingsFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(6, frame.getLength());
        assertEquals(4, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(1, frame.getSettings().size());
        Http2Setting setting = frame.getSettings().iterator().next();
        assertEquals(0x00FFFF, setting.getID());
        assertEquals(0x01020304L, setting.getValue());
    }
    
    @Test
    public void settingsHighestValue() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.SETTINGS_HIGHEST_VALUE_BUFFER);
        Http2SettingsFrame frame = (Http2SettingsFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(6, frame.getLength());
        assertEquals(4, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(1, frame.getSettings().size());
        Http2Setting setting = frame.getSettings().iterator().next();
        assertEquals(1, setting.getID());
        assertEquals(0xFFFFFFFFL, setting.getValue());
    }
    
    @Test
    public void unknownFrame() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.UNKNOWN_PAYLOAD_BUFFER);
        Http2UnknownFrame frame = (Http2UnknownFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(2, frame.getLength());
        assertEquals(255, frame.getType() & 0x00FF);
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(2, frame.getPayload().length);
        assertArrayEquals(new byte[] {0x0E,  0x18}, frame.getPayload());
    }
    
    @Test
    public void unknownFrameNoPayload() {
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.UNKNOWN_NO_PAYLOAD_BUFFER);
        Http2UnknownFrame frame = (Http2UnknownFrame) decoder.decode(buffer, context);
        assertNotNull(frame);
        assertEquals(0, frame.getLength());
        assertEquals(255, frame.getType() & 0x00FF);
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(0, frame.getPayload().length);
    }
}
