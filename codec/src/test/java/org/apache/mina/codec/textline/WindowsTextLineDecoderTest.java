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
package org.apache.mina.codec.textline;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.apache.mina.codec.textline.TextLineDecoder.Context;
import org.junit.Test;

/**
 * A {@link TextLineDecoder} test.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class WindowsTextLineDecoderTest {

    @Test
    public void testThatEmptyBufferReturnsEmptyResult() {
        TextLineDecoder decoder = new TextLineDecoder(LineDelimiter.WINDOWS);
        Context context = decoder.createDecoderState();
        String results = decoder.decode(ByteBuffer.allocate(0), context);
        assertNull(results);
    }

    @Test
    public void testThatNonLineTerminatedStringReturnsEmptyResult() {
        TextLineDecoder decoder = new TextLineDecoder(LineDelimiter.WINDOWS);
        Context context = decoder.createDecoderState();
        String results = decoder.decode(ByteBuffer.wrap("a string".getBytes()), context);
        assertNull(results);
        assertEquals(8, context.getBuffer().position());
    }

    @Test
    public void testThatUnixLineTerminatedStringReturnsEmptyResult() {
        TextLineDecoder decoder = new TextLineDecoder(LineDelimiter.WINDOWS);
        Context context = decoder.createDecoderState();
        String results = decoder.decode(ByteBuffer.wrap("a string\n".getBytes()), context);
        assertNull(results);
        assertEquals(9, context.getBuffer().position());
    }

    @Test
    public void testThatWindowsLineTerminatedStringReturnsNonEmptyResult() {
        TextLineDecoder decoder = new TextLineDecoder(LineDelimiter.WINDOWS);
        Context context = decoder.createDecoderState();
        String results = decoder.decode(ByteBuffer.wrap("a string\r\n".getBytes()), context);
        assertNotNull(results);
        assertEquals("a string", results);
        assertEquals(0, context.getBuffer().position());
    }

    @Test
    public void testThatContextIsMaintainedBetweenMessages() {
        TextLineDecoder decoder = new TextLineDecoder(LineDelimiter.WINDOWS);
        Context context = decoder.createDecoderState();
        String results = decoder.decode(ByteBuffer.wrap("a string\r\na".getBytes()), context);
        assertNotNull(results);
        assertEquals("a string", results);
        assertEquals(1, context.getBuffer().position());
        results = decoder.decode(ByteBuffer.wrap(" string\r\n".getBytes()), context);
        assertNotNull(results);
        assertEquals("a string", results);
        assertEquals(0, context.getBuffer().position());
    }

    @Test
    public void testThatUnixLineTerminatedLongStringReturnsEmptyResult() {
        TextLineDecoder decoder = new TextLineDecoder(LineDelimiter.WINDOWS);
        Context context = decoder.createDecoderState();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 100; ++i) {
            sb.append("a string");
        }
        String results = decoder.decode(ByteBuffer.wrap((sb.toString() + "\n").getBytes()), context);
        assertNull(results);
        assertEquals(801, context.getBuffer().position());
    }

    @Test
    public void testThatWindowsLineTerminatedLongStringReturnsNonEmptyResult() {
        TextLineDecoder decoder = new TextLineDecoder(LineDelimiter.WINDOWS);
        Context context = decoder.createDecoderState();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 100; ++i) {
            sb.append("a string");
        }
        String results = decoder.decode(ByteBuffer.wrap((sb.toString() + "\r\n").getBytes()), context);
        assertNotNull(results);
        assertEquals(sb.toString(), results);
        assertEquals(0, context.getBuffer().position());
    }
}
