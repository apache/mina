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

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ByteBufferDumperTest {

    @Test
    public void stringTest() {
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
        String dump = ByteBufferDumper.dump(myBuffer);
        assertEquals("ByteBuffer[len=8,str='" + toTest + "']", dump);
        assertEquals(remaining, myBuffer.remaining());
        assertEquals(pos, myBuffer.position());
    }

    @Test
    public void binaryTest() {
        ByteBuffer myBuffer = ByteBuffer.allocate(4);
        myBuffer.put((byte) 0x88);
        myBuffer.put((byte) 0x03);
        myBuffer.put((byte) 0xFF);
        myBuffer.flip();

        int remaining = myBuffer.remaining();
        int pos = myBuffer.position();
        String dump = ByteBufferDumper.dump(myBuffer);
        System.err.println(dump);
        assertEquals("ByteBuffer[len=3,bytes='0x88 0x03 0xFF']", dump);
        assertEquals(remaining, myBuffer.remaining());
        assertEquals(pos, myBuffer.position());
    }

    @Test
    public void testWithSizeLimit() {
        ByteBuffer bb = ByteBuffer.allocate(10);
        bb.put(new byte[] { 0x01, (byte) 0x8F, 0x04, 0x7A, (byte) 0xc2, 0x23, (byte) 0xA0, 0x08, 0x44 });
        bb.flip();

        assertEquals("ByteBuffer[len=9,bytes='0x01 0x8F 0x04 0x7A 0xC2']", ByteBufferDumper.dump(bb, 5, false));
        assertEquals("ByteBuffer[len=9,bytes='0x01 0x8F 0x04 0x7A 0xC2']", ByteBufferDumper.dump(bb, 5, true));
        assertEquals("ByteBuffer[len=9,str='']", ByteBufferDumper.dump(bb, 0, true));
        assertEquals("ByteBuffer[len=9,bytes='0x01 0x8F 0x04 0x7A 0xC2 0x23 0xA0 0x08 0x44']",
                ByteBufferDumper.dump(bb, 10, true));
        assertEquals("ByteBuffer[len=9,bytes='0x01 0x8F 0x04 0x7A 0xC2 0x23 0xA0 0x08 0x44']",
                ByteBufferDumper.dump(bb, -1, false));
    }

    @Test
    public void toHex() {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.put((byte) 0);
        bb.put((byte) 1);
        bb.put((byte) 2);
        bb.put((byte) 254);
        bb.flip();
        assertEquals("000102FE", ByteBufferDumper.toHex(bb));
    }
    
    @Test
    public void checkFromHexStringEmptyStringReturnsEmptyByteArray() {
        ByteBuffer buffer = ByteBufferDumper.fromHexString("");
        assertEquals(0, buffer.remaining());
    }
    
    @Test
    public void checkFromHexStringNormalStringReturnsByteArray() {
        ByteBuffer buffer = ByteBufferDumper.fromHexString("ff");
        assertEquals(1, buffer.remaining());
        assertEquals(-1, buffer.get());
    }
    
    @Test
    public void checkFromHexStringNormalStringUppercaseReturnsByteArray() {
        ByteBuffer buffer = ByteBufferDumper.fromHexString("FF");
        assertEquals(1, buffer.remaining());
        assertEquals(-1, buffer.get());
    }
    
    @Test(expected=NumberFormatException.class)
    public void checkFromHexStringInvalidStringReturnsException() {
        ByteBuffer buffer = ByteBufferDumper.fromHexString("non-hexastring");
        assertEquals(1, buffer.remaining());
        assertEquals(-1, buffer.get());
    }
}
