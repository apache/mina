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

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

/**
 * A {@link ByteBufferOutputStream} test.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ByteBufferOutputStreamTest {
    @Test
    public void testEmpty() throws IOException {
        ByteBufferOutputStream bbos = new ByteBufferOutputStream();

        bbos.close();

        assertEquals(0, bbos.getByteBuffer().remaining());
    }

    @Test
    public void testSingleWrite() throws IOException {
        ByteBufferOutputStream bbos = new ByteBufferOutputStream();
        bbos.write(86);
        bbos.close();

        assertEquals(1, bbos.getByteBuffer().remaining());
        assertEquals(86, bbos.getByteBuffer().get());
    }

    @Test
    public void testWrite() throws IOException {
        byte[] src = "HELLO MINA!".getBytes();

        ByteBufferOutputStream bbos = new ByteBufferOutputStream(1024);
        bbos.write(src);
        bbos.close();

        assertEquals(src.length, bbos.getByteBuffer().remaining());
        assertEquals(ByteBuffer.wrap(src), bbos.getByteBuffer());
    }

    @Test
    public void testElasticity() throws IOException {
        final int allocatedSize = 1024;
        List<ByteBufferOutputStream> list = new LinkedList<ByteBufferOutputStream>();
        ByteBufferOutputStream elastic = new ByteBufferOutputStream(allocatedSize);
        elastic.setElastic(true);
        ByteBufferOutputStream nonElastic = new ByteBufferOutputStream(allocatedSize);
        nonElastic.setElastic(false);
        list.add(elastic);
        list.add(nonElastic);
        byte[] src = new byte[321];
        for (int i = 0; i < src.length; i++)
            src[i] = (byte) (0xff & (i / 7));

        for (ByteBufferOutputStream bbos : list) {
            for (int j = 0; j < allocatedSize * 10; j += src.length) {
                try {
                    bbos.write(src);
                    if (!bbos.isElastic() && j > allocatedSize)
                        fail("Overflooded buffer without any exception!");
                } catch (BufferOverflowException boe) {
                    if (bbos.isElastic())
                        fail("Elastic buffer overflooded! " + boe);
                }
            }
            bbos.close();
        }
    }

}
