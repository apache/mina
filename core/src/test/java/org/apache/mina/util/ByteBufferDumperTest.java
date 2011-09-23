/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mina.util;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;

public class ByteBufferDumperTest {

    @Test
    public void string_test() {
        ByteBufferDumper dumper = new ByteBufferDumper();

        String toTest = "yopYOP\n\r";
        byte[] charData = toTest.getBytes();
        assertEquals(toTest.length(), charData.length);

        ByteBuffer myBuffer = ByteBuffer.allocate(toTest.length());
        for (int i = 0; i < toTest.length(); i++) {
            myBuffer.put(charData[i]);
        }
        myBuffer.flip();

        int remaining = myBuffer.remaining();
        int pos = myBuffer.position();
        String dump = dumper.dump(myBuffer);
        assertEquals("ByteBuffer[len=8,str='" + toTest + "'", dump);
        assertEquals(remaining, myBuffer.remaining());
        assertEquals(pos, myBuffer.position());
    }

    @Test
    public void binary_test() {
        ByteBufferDumper dumper = new ByteBufferDumper();
        ByteBuffer myBuffer = ByteBuffer.allocate(4);
        myBuffer.put((byte) 0x88);
        myBuffer.put((byte) 0x03);
        myBuffer.put((byte) 0xFF);
        myBuffer.flip();

        int remaining = myBuffer.remaining();
        int pos = myBuffer.position();
        String dump = dumper.dump(myBuffer);
        System.err.println(dump);
        assertEquals("ByteBuffer[len=3,bytes='88 03 FF'", dump);
        assertEquals(remaining, myBuffer.remaining());
        assertEquals(pos, myBuffer.position());

    }

}
