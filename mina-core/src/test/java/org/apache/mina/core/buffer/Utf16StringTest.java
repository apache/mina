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
package org.apache.mina.core.buffer;

import static org.junit.Assert.assertEquals;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class Utf16StringTest {
    /*
     * Based on mina-core\src\test\java\org\apache\mina\core\buffer\IoBufferTest.testGetString(CharsetDecoder)
     * in branch 2.1.X
     */
    @Test
    public void testMacron() throws CharacterCodingException {
        IoBuffer buf = IoBuffer.allocate(16);

        buf.clear();
        buf.fillAndReset(buf.limit());
        CharsetDecoder decoder = StandardCharsets.UTF_16BE.newDecoder();

        buf.put((byte) 0);
        buf.put((byte) 'A');
        buf.put((byte) 0);
        buf.put((byte) 'B');
        buf.put((byte) 0);
        buf.put((byte) 'C');
        buf.put((byte) 0);
        buf.put((byte) 0);

        buf.position(0);
        assertEquals( "ABC",  buf.getString(decoder) );
        // all good

        buf.clear();
        buf.fillAndReset(buf.limit());

        // now put "MĀORI" there: \u004d \u0100 \u004f \u0052 \u0049
        // 00 4D 01 00 00 4F 00 52 00 49

        buf = IoBuffer.allocate(16);
        buf.put((byte) 0x00);
        buf.put((byte) 0x4d);
        buf.put((byte) 0x01);
        buf.put((byte) 0x00);
        buf.put((byte) 0x00);
        buf.put((byte) 0x4f);
        buf.put((byte) 0x00);
        buf.put((byte) 0x52);
        buf.put((byte) 0x00);
        buf.put((byte) 0x49);
        buf.put((byte) 0x00);

        buf.position(0);
        
        assertEquals( "MĀORI",  buf.getString(decoder) );

        buf.clear();
        buf.fillAndReset(buf.limit());

        // now put "MĀORI" there: \u004d \u0100 \u004f \u0052 \u0049
        // 01 00 00 00

        buf = IoBuffer.allocate(16);
        buf.put((byte) 0x01);
        buf.put((byte) 0x00);

        buf.position(0);
        
        assertEquals( "Ā",  buf.getString(decoder) );
    }
    
    @Test
    public void testNotZeroTerminatedUtf16String() throws CharacterCodingException {
        IoBuffer buf = IoBuffer.allocate(2);
        buf.put((byte) 0x01);
        buf.put((byte) 0x00);
        buf.position(0);
        String decoded = buf.getString(StandardCharsets.UTF_16BE.newDecoder());
        assertEquals("Ā", decoded);
    }
}

