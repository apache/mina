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
package org.apache.mina.http2;

import java.util.Collections;

import org.apache.mina.http2.api.Http2ContinuationFrame;
import org.apache.mina.http2.api.Http2ContinuationFrame.Http2ContinuationFrameBuilder;
import org.apache.mina.http2.api.Http2DataFrame;
import org.apache.mina.http2.api.Http2DataFrame.Http2DataFrameBuilder;
import org.apache.mina.http2.api.Http2GoAwayFrame;
import org.apache.mina.http2.api.Http2GoAwayFrame.Http2GoAwayFrameBuilder;
import org.apache.mina.http2.api.Http2HeadersFrame;
import org.apache.mina.http2.api.Http2HeadersFrame.Http2HeadersFrameBuilder;
import org.apache.mina.http2.api.Http2PingFrame;
import org.apache.mina.http2.api.Http2PingFrame.Http2PingFrameBuilder;
import org.apache.mina.http2.api.Http2PriorityFrame;
import org.apache.mina.http2.api.Http2PriorityFrame.Http2PriorityFrameBuilder;
import org.apache.mina.http2.api.Http2PushPromiseFrame;
import org.apache.mina.http2.api.Http2PushPromiseFrame.Http2PushPromiseFrameBuilder;
import org.apache.mina.http2.api.Http2RstStreamFrame;
import org.apache.mina.http2.api.Http2RstStreamFrame.Http2RstStreamFrameBuilder;
import org.apache.mina.http2.api.Http2Setting;
import org.apache.mina.http2.api.Http2SettingsFrame;
import org.apache.mina.http2.api.Http2SettingsFrame.Http2SettingsFrameBuilder;
import org.apache.mina.http2.api.Http2UnknownFrame;
import org.apache.mina.http2.api.Http2UnknownFrame.Http2UnknownFrameBuilder;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */

public final class TestMessages {

    public static final byte[] CONTINUATION_NO_HEADER_FRAGMENT_BUFFER = new byte[] {
        0x00, 0x00, 0x00, /*length*/
        0x09, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x32 /*streamID*/
        };

    public static final Http2ContinuationFrame CONTINUATION_NO_HEADER_FRAGMENT_FRAME = Http2ContinuationFrameBuilder.builder().
            streamID(50).
            build();
    
    public static final byte[] CONTINUATION_HEADER_FRAGMENT_BUFFER = new byte[] {
        0x00, 0x00, 0x0A, /*length*/
        0x09, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x32, /*streamID*/
        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A /*headerFragment*/
        };
    
    public static final Http2ContinuationFrame CONTINUATION_HEADER_FRAGMENT_FRAME = Http2ContinuationFrameBuilder.builder().
                streamID(50).
                headerBlockFragment(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A}).
                build();
    
    public static final byte[] DATA_NO_PAYLOAD_NO_PADDING_BUFFER = new byte[] {
        0x00, 0x00, 0x00, /*length*/
        0x00, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x32 /*streamID*/
        };
    
    public static final Http2DataFrame DATA_NO_PAYLOAD_NO_PADDING_FRAME = Http2DataFrameBuilder.builder().
            streamID(50).
            build();
    
    public static final byte[] DATA_PAYLOAD_NO_PADDING_BUFFER = new byte[] {
        0x00, 0x00, 0x0A, /*length*/
        0x00, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x32, /*streamID*/
        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A /*headerFragment*/
        };
    
    public static final Http2DataFrame DATA_PAYLOAD_NO_PADDING_FRAME = Http2DataFrameBuilder.builder().
            streamID(50).
            data(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A}).
            build();
    
    public static final byte[] DATA_NO_PAYLOAD_PADDING_BUFFER = new byte[] {
        0x00, 0x00, 0x03, /*length*/
        0x00, /*type*/
        0x08, /*flags*/
        0x00, 0x00, 0x00, 0x32, /*streamID*/
        0x02, /*padLength*/
        0x0E, 0x28 /*padding*/
        };
    
    public static final Http2DataFrame DATA_NO_PAYLOAD_PADDING_FRAME = Http2DataFrameBuilder.builder().
            streamID(50).
            padding(new byte[] {0x0E, 0x28}).
            build();
    
    public static final byte[] DATA_PAYLOAD_PADDING_BUFFER = new byte[] {
        0x00, 0x00, 0x0D, /*length*/
        0x00, /*type*/
        0x08, /*flags*/
        0x00, 0x00, 0x00, 0x32, /*streamID*/
        0x02, /*padLength*/
        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, /*data*/
        0x0E, 0x28 /*padding*/
        };
    
    public static final Http2DataFrame DATA_PAYLOAD_PADDING_FRAME = Http2DataFrameBuilder.builder().
            streamID(50).
            data(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A}).
            padding(new byte[] {0x0E, 0x28}).
            build();
    
    public static final byte[] GOAWAY_NO_DATA_BUFFER = new byte[] {
        0x00, 0x00, 0x08, /*length*/
        0x07, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x01, /*streamID*/
        0x00, 0x00, 0x01, 0x00, /*lastStreamID*/
        0x00, 0x01, 0x02, 0x03 /*errorCode*/
        };
    
    public static final Http2GoAwayFrame GOAWAY_NO_DATA_FRAME = Http2GoAwayFrameBuilder.builder().
            streamID(1).
            lastStreamID(256).
            errorCode(0x010203).
            build();
    
    public static final byte[] GOAWAY_NO_DATA_HIGHEST_STREAMID_BUFFER = new byte[] {
        0x00, 0x00, 0x08, /*length*/
        0x07, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x01, /*streamID*/
        0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, /*lastStreamID*/
        0x00, 0x01, 0x02, 0x03 /*errorCode*/
        };
    
    public static final Http2GoAwayFrame GOAWAY_NO_DATA_HIGHEST_STREAMID_FRAME = Http2GoAwayFrameBuilder.builder().
            streamID(1).
            lastStreamID(0x7FFFFFFF).
            errorCode(0x010203).
            build();
    
    public static final byte[] GOAWAY_NO_DATA_HIGHEST_STREAMID_RESERVED_BIT_BUFFER = new byte[] {
        0x00, 0x00, 0x08, /*length*/
        0x07, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x01, /*streamID*/
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, /*lastStreamID*/
        0x00, 0x01, 0x02, 0x03 /*errorCode*/
        };
    
    public static final byte[] GOAWAY_NO_DATA_HIGHEST_ERROR_CODE_BUFFER = new byte[] {
        0x00, 0x00, 0x08, /*length*/
        0x07, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x01, /*streamID*/
        (byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, /*lastStreamID*/
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF /*errorCode*/
        };
    
    public static final Http2GoAwayFrame GOAWAY_NO_DATA_HIGHEST_ERROR_CODE_FRAME = Http2GoAwayFrameBuilder.builder().
            streamID(1).
            lastStreamID(0x7FFFFFFF).
            errorCode(0x00FFFFFFFFL).
            build();
    
    public static final byte[] GOAWAY_DATA_BUFFER = new byte[] {
        0x00, 0x00, 0x09, /*length*/
        0x07, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x01, /*streamID*/
        0x00, 0x00, 0x01, 0x00, /*lastStreamID*/
        0x00, 0x01, 0x02, 0x03, /*errorCode*/
        0x01 /*additionData*/
        };
    
    public static final Http2GoAwayFrame GOAWAY_DATA_FRAME = Http2GoAwayFrameBuilder.builder().
            streamID(1).
            lastStreamID(256).
            errorCode(0x010203).
            data(new byte[] { 0x01}).
            build();
    
    public static final byte[] HEADERS_NO_PADDING_NO_PRIORITY_BUFFER = new byte[] {
        0x00, 0x00, 0x01, /*length*/
        0x01, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x01, /*streamID*/
        (byte) 0x82 /*headerFragment*/
        };
    
    public static final Http2HeadersFrame HEADERS_NO_PADDING_NO_PRIORITY_FRAME = Http2HeadersFrameBuilder.builder().
            streamID(1).
            headerBlockFragment(new byte[] {(byte) 0x82}).
            build();
    
    public static final byte[] HEADERS_PADDING_PRIORITY_BUFFER = new byte[] {
        0x00, 0x00, 0x17, /*length*/
        0x01, /*type*/
        0x28, /*flags*/
        0x00, 0x00, 0x00, 0x03, /*streamID*/
        0x10, /*padding length*/
        (byte)0x0080, 0x00, 0x00, 0x14, /*stream dependency*/
        0x09, /*weight*/
        (byte) 0x82, /*headerFragment*/
        0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x70, 0x61, 0x64, 0x64, 0x69, 0x6E, 0x67, 0x2E /*padding*/
        };
    
    public static final Http2HeadersFrame HEADERS_PADDING_PRIORITY_FRAME = Http2HeadersFrameBuilder.builder().
            streamID(3).
            exclusiveMode(true).
            streamDependencyID(20).
            weight((short) 10).
            headerBlockFragment(new byte[] { (byte) 0x82}).
            padding(new byte[] {0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x70, 0x61, 0x64, 0x64, 0x69, 0x6E, 0x67, 0x2E}).
            build();
    
    public static final byte[] HEADERS_PADDING_NO_PRIORITY_BUFFER = new byte[] {
        0x00, 0x00, 0x12, /*length*/
        0x01, /*type*/
        0x08, /*flags*/
        0x00, 0x00, 0x00, 0x03, /*streamID*/
        0x10, /*padding length*/
        (byte) 0x0082, /*headerFragment*/
        0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x70, 0x61, 0x64, 0x64, 0x69, 0x6E, 0x67, 0x2E /*padding*/
        };
    
    public static final Http2HeadersFrame HEADERS_PADDING_NO_PRIORITY_FRAME = Http2HeadersFrameBuilder.builder().
            streamID(3).
            headerBlockFragment(new byte[] { (byte) 0x82}).
            padding(new byte[] {0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x70, 0x61, 0x64, 0x64, 0x69, 0x6E, 0x67, 0x2E}).
            build();
    
    public static final byte[] PING_STANDARD_BUFFER = new byte[] {
        0x00, 0x00, 0x08, /*length*/
        0x06, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x20, /*streamID*/
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07/*opaqueData*/
        };
    
    public static final Http2PingFrame PING_STANDARD_FRAME = Http2PingFrameBuilder.builder().
            streamID(32).
            data(new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07}).
            build();
    
    public static final byte[] PING_EXTRA_DATA_BUFFER = new byte[] {
        0x00, 0x00, 0x09, /*length*/
        0x06, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x20, /*streamID*/
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08/*opaqueData*/
        };
    
    public static final Http2PingFrame PING_EXTRA_DATA_FRAME = Http2PingFrameBuilder.builder().
            streamID(32).
            data(new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08}).
            build();
    
    public static final byte[] PING_NO_ENOUGH_DATA_BUFFER = new byte[] {
        0x00, 0x00, 0x01, /*length*/
        0x06, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x20, /*streamID*/
        0x00/*opaqueData*/
        };
    
    public static final Http2PingFrame PING_NO_ENOUGH_DATA_FRAME = Http2PingFrameBuilder.builder().
            streamID(32).
            data(new byte[] {0x00}).
            build();
    
    public static final byte[] PRIORITY_NO_EXCLUSIVE_MODE_BUFFER = new byte[] {
        0x00, 0x00, 0x05, /*length*/
        0x02, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x20, /*streamID*/
        0x00, 0x00, 0x01, 0x00, /*streamDependency*/
        0x01 /*weight*/
        };
    
    public static final Http2PriorityFrame PRIORITY_NO_EXCLUSIVE_MODE_FRAME = Http2PriorityFrameBuilder.builder().
            streamID(32).
            weight((short) 2).
            streamDependencyID(256).
            build();
    
    public static final byte[] PRIORITY_EXCLUSIVE_MODE_BUFFER = new byte[] {
        0x00, 0x00, 0x05, /*length*/
        0x02, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x20, /*streamID*/
        (byte) 0x80, 0x00, 0x01, 0x00, /*streamDependency*/
        0x01 /*weight*/
        };
    
    public static final Http2PriorityFrame PRIORITY_EXCLUSIVE_MODE_FRAME = Http2PriorityFrameBuilder.builder().
            streamID(32).
            weight((short) 2).
            streamDependencyID(256).
            exclusiveMode(true).
            build();
    
    public static final byte[] PUSH_PROMISE_NO_PADDING_BUFFER = new byte[] {
        0x00, 0x00, 0x05, /*length*/
        0x05, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x01, /*streamID*/
        0x00, 0x00, 0x01, 0x00, /*promisedStreamID*/
        (byte) 0x82 /*headerFragment*/
        };
    
    public static final Http2PushPromiseFrame PUSH_PROMISE_NO_PADDING_FRAME = Http2PushPromiseFrameBuilder.builder().
            streamID(1).
            promisedStreamID(256).
            headerBlockFragment(new byte[] {(byte) 0x82}).
            build();
    
    public static final byte[] PUSH_PROMISE_PADDING_BUFFER = new byte[] {
        0x00, 0x00, 0x16, /*length*/
        0x05, /*type*/
        0x08, /*flags*/
        0x00, 0x00, 0x00, 0x03, /*streamID*/
        0x10, /*padding length*/
        0x00, 0x00, 0x00, 0x14, /*promisedStreamID*/
        (byte) 0x0082, /*headerFragment*/
        0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x70, 0x61, 0x64, 0x64, 0x69, 0x6E, 0x67, 0x2E /*padding*/
        };
    
    public static final Http2PushPromiseFrame PUSH_PROMISE_PADDING_FRAME = Http2PushPromiseFrameBuilder.builder().
            streamID(3).
            promisedStreamID(20).
            headerBlockFragment(new byte[] {(byte) 0x82}).
            padding(new byte[] { 0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x70, 0x61, 0x64, 0x64, 0x69, 0x6E, 0x67, 0x2E}).
            build();
    
    public static final byte[] RST_STREAM_NO_EXTRA_PAYLOAD_BUFFER = new byte[] {
        0x00, 0x00, 0x04, /*length*/
        0x03, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x20, /*streamID*/
        0x00, 0x00, 0x01, 0x00, /*errorCode*/
        };
    
    public static final Http2RstStreamFrame RST_STREAM_NO_EXTRA_PAYLOAD_FRAME = Http2RstStreamFrameBuilder.builder().
            streamID(32).
            errorCode(256).
            build();
    
    public static final byte[] RST_STREAM_HIGHEST_VALUE_NO_EXTRA_PAYLOAD_BUFFER = new byte[] {0x00, 0x00, 0x04, /*length*/
        0x03, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x20, /*streamID*/
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, /*errorCode*/
        };
    
    public static final Http2RstStreamFrame RST_STREAM_HIGHEST_VALUE_NO_EXTRA_PAYLOAD_FRAME = Http2RstStreamFrameBuilder.builder().
            streamID(32).
            errorCode(0x00FFFFFFFFL).
            build();

    public static final byte[] RST_STREAM_EXTRA_PAYLOAD_BUFFER = new byte[] {
        0x00, 0x00, 0x06, /*length*/
        0x03, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x20, /*streamID*/
        0x00, 0x00, 0x01, 0x00, /*errorCode*/
        0x0E, 0x28
        };
   
    public static final byte[] RST_STREAM_EXTRA_PAYLOAD_HIGHEST_BUFFER = new byte[] {
        0x00, 0x00, 0x06, /*length*/
        0x03, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x20, /*streamID*/
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, /*errorCode*/
        0x0E, 0x28
        };
    
    public static final byte[] SETTINGS_DEFAULT_BUFFER = new byte[] {
        0x00, 0x00, 0x06, /*length*/
        0x04, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x20, /*streamID*/
        0x00, 0x01, /*ID*/
        0x01, 0x02, 0x03, 0x04, /*value*/
        };
    
    public static final Http2SettingsFrame SETTINGS_DEFAULT_FRAME = Http2SettingsFrameBuilder.builder().
            streamID(32).
            settings(Collections.singletonList(new Http2Setting(1, 0x01020304))).
            build();
    
    public static final byte[] SETTINGS_HIGHEST_ID_BUFFER = new byte[] {
        0x00, 0x00, 0x06, /*length*/
        0x04, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x20, /*streamID*/
        (byte) 0xFF, (byte) 0xFF, /*ID*/
        0x01, 0x02, 0x03, 0x04, /*value*/
        };
    
    public static final Http2SettingsFrame SETTINGS_HIGHEST_ID_FRAME = Http2SettingsFrameBuilder.builder().
            streamID(32).
            settings(Collections.singletonList(new Http2Setting(0x00FFFF, 0x01020304))).
            build();
    
    public static final byte[] SETTINGS_HIGHEST_VALUE_BUFFER = new byte[] {
        0x00, 0x00, 0x06, /*length*/
        0x04, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x20, /*streamID*/
        0x00, 0x01, /*ID*/
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, /*value*/
        };
    
    public static final Http2SettingsFrame SETTINGS_HIGHEST_VALUE_FRAME = Http2SettingsFrameBuilder.builder().
            streamID(32).
            settings(Collections.singletonList(new Http2Setting(1, 0xFFFFFFFFL))).
            build();
    
    public static final byte[] UNKNOWN_PAYLOAD_BUFFER = new byte[] {
        0x00, 0x00, 0x02, /*length*/
        (byte) 0x00FF, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x20, /*streamID*/
        0x0E, 0x18
        };
    
    public static final Http2UnknownFrame UNKNOWN_PAYLOAD_FRAME = Http2UnknownFrameBuilder.builder().
            type((short) 255).
            streamID(32).
            payload(new byte[] { 0x0E, 0x18}).
            build();
    
    public static final byte[] UNKNOWN_NO_PAYLOAD_BUFFER = new byte[] {
        0x00, 0x00, 0x00, /*length*/
        (byte) 0x00FF, /*type*/
        0x00, /*flags*/
        0x00, 0x00, 0x00, 0x20 /*streamID*/
        };
    
    public static final Http2UnknownFrame UNKNOWN_NO_PAYLOAD_FRAME = Http2UnknownFrameBuilder.builder().
            type((short) 255).
            streamID(32).
            build();
}
