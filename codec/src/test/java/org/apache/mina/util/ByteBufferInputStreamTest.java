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

package org.apache.mina.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * A {@link ByteBufferInputStream} test.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ByteBufferInputStreamTest {
    @Test
    public void testEmpty() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(0);
        InputStream is = new ByteBufferInputStream(bb);
        assertEquals(-1, is.read());
        is.close();
    }

    @Test
    public void testReadArray() throws IOException {
        byte[] src = "HELLO MINA".getBytes();
        byte[] dst = new byte[src.length];
        ByteBuffer bb = ByteBuffer.wrap(src);
        InputStream is = new ByteBufferInputStream(bb);

        assertEquals(true, is.markSupported());
        is.mark(src.length);
        assertEquals(dst.length, is.read(dst));
        assertArrayEquals(src, dst);
        assertEquals(-1, is.read());
        is.close();

        is.reset();
        byte[] dstTooBig = new byte[src.length + 1];
        assertEquals(src.length, is.read(dstTooBig));

        assertEquals(-1, is.read(dstTooBig));
    }

    @Test
    public void testSkip() throws IOException {
        byte[] src = "HELLO MINA!".getBytes();
        ByteBuffer bb = ByteBuffer.wrap(src);
        InputStream is = new ByteBufferInputStream(bb);
        is.skip(6);

        assertEquals(5, is.available());
        assertEquals('M', is.read());
        assertEquals('I', is.read());
        assertEquals('N', is.read());
        assertEquals('A', is.read());

        is.skip((long) Integer.MAX_VALUE + 1);
        assertEquals(-1, is.read());
        is.close();
    }

}
