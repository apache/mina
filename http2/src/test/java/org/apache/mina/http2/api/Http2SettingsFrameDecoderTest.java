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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.ByteBuffer;

import org.apache.mina.http2.Http2Test;
import org.apache.mina.http2.TestMessages;
import org.apache.mina.http2.impl.Http2Connection;
import org.junit.Test;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Http2SettingsFrameDecoderTest extends Http2Test {

    @Test
    public void decode() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.SETTINGS_DEFAULT_BUFFER);
        Http2SettingsFrame frame = (Http2SettingsFrame) connection.decode(buffer);
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
    public void encode() {
        Http2SettingsFrame frame = TestMessages.SETTINGS_DEFAULT_FRAME;
         assertArrayEquals(TestMessages.SETTINGS_DEFAULT_BUFFER, toByteArray(frame.toBuffer()));       
    }

    @Test
    public void decodeHighestID() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.SETTINGS_HIGHEST_ID_BUFFER);
        Http2SettingsFrame frame = (Http2SettingsFrame) connection.decode(buffer);
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
    public void encodeHighestID() {
        Http2SettingsFrame frame = TestMessages.SETTINGS_HIGHEST_ID_FRAME;
         assertArrayEquals(TestMessages.SETTINGS_HIGHEST_ID_BUFFER, toByteArray(frame.toBuffer()));       
    }

    @Test
    public void decodeHighestValue() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.SETTINGS_HIGHEST_VALUE_BUFFER);
        Http2SettingsFrame frame = (Http2SettingsFrame) connection.decode(buffer);
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
    public void encodeHighestValue() {
        Http2SettingsFrame frame = TestMessages.SETTINGS_HIGHEST_VALUE_FRAME;
         assertArrayEquals(TestMessages.SETTINGS_HIGHEST_VALUE_BUFFER, toByteArray(frame.toBuffer()));       
    }

}
