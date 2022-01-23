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

import org.junit.Test;

public class IoBufferHexDumperTest {

    @Test
    public void checkHexDumpLength() {
        IoBuffer buf = IoBuffer.allocate(5000);

        for (int i = 0; i < 20; i++) {
            buf.putShort((short) 0xF0A0);
        }

        buf.flip();

        /* special case */
        assertEquals(0, buf.getHexDump(0).length());

        /* no truncate needed */
        assertEquals(buf.limit() * 3 - 1, buf.getHexDump().length());
        assertEquals((Math.min(300, buf.limit()) * 3) - 1, buf.getHexDump(300).length());

        /* must truncate */
        assertEquals((7 * 3) - 1, buf.getHexDump(7).length());
        assertEquals((10 * 3) - 1, buf.getHexDump(10).length());
        assertEquals((30 * 3) - 1, buf.getHexDump(30).length());

    }

    @Test
    public void checkPrettyHexDumpLength() {
        IoBuffer buf = IoBuffer.allocate(5000);

        for (int i = 0; i < 20; i++) {
            buf.putShort((short) 0xF0A0);
        }

        buf.flip();

        String[] dump = buf.getHexDump(50, true).split("\\n");
        
        assertEquals(4, dump.length);
    }
}
