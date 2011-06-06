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
package org.apache.mina.filter.compression;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.mina.core.buffer.IoBuffer;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ZlibTest {
    private Zlib deflater = null;

    private Zlib inflater = null;

    @Before
    public void setUp() throws Exception {
        deflater = new Zlib(Zlib.COMPRESSION_MAX, Zlib.MODE_DEFLATER);
        inflater = new Zlib(Zlib.COMPRESSION_MAX, Zlib.MODE_INFLATER);
    }

    @Test
    public void testCompression() throws Exception {
        String strInput = "";

        // increase the count to as many as required to generate a long
        // string for input
        for (int i = 0; i < 10; i++) {
            strInput += "The quick brown fox jumps over the lazy dog.  ";
        }
        IoBuffer byteInput = IoBuffer.wrap(strInput.getBytes("UTF8"));

        // increase the count to have the compression and decompression
        // done using the same instance of Zlib
        for (int i = 0; i < 5; i++) {
            IoBuffer byteCompressed = deflater.deflate(byteInput);
            IoBuffer byteUncompressed = inflater.inflate(byteCompressed);
            String strOutput = byteUncompressed.getString(Charset.forName(
                    "UTF8").newDecoder());
            assertTrue(strOutput.equals(strInput));
        }
    }

    @Test
    public void testCorruptedData() throws Exception {
        String strInput = "Hello World";
        IoBuffer byteInput = IoBuffer.wrap(strInput.getBytes("UTF8"));

        IoBuffer byteCompressed = deflater.deflate(byteInput);
        // change the contents to something else. Since this doesn't check
        // for integrity, it wont throw an exception
        byteCompressed.put(5, (byte) 0xa);
        IoBuffer byteUncompressed = inflater.inflate(byteCompressed);
        String strOutput = byteUncompressed.getString(Charset.forName("UTF8")
                .newDecoder());
        assertFalse(strOutput.equals(strInput));
    }

    @Test
    public void testCorruptedHeader() throws Exception {
        String strInput = "Hello World";
        IoBuffer byteInput = IoBuffer.wrap(strInput.getBytes("UTF8"));

        IoBuffer byteCompressed = deflater.deflate(byteInput);
        // write a bad value into the zlib header. Make sure that
        // the decompression fails
        byteCompressed.put(0, (byte) 0xca);
        try {
            inflater.inflate(byteCompressed);
        } catch (IOException e) {
            assertTrue(true);
            return;
        }
        assertTrue(false);
    }

    @Test
    public void testFragments() throws Exception {
        String strInput = "";
        for (int i = 0; i < 10; i++) {
            strInput += "The quick brown fox jumps over the lazy dog.  ";
        }
        IoBuffer byteInput = IoBuffer.wrap(strInput.getBytes("UTF8"));
        IoBuffer byteCompressed = null;

        for (int i = 0; i < 5; i++) {
            byteCompressed = deflater.deflate(byteInput);
            if (i == 0) {
                // decompress the first compressed output since it contains
                // the zlib header, which will not be generated for further
                // compressions done with the same instance
                IoBuffer byteUncompressed = inflater.inflate(byteCompressed);
                String strOutput = byteUncompressed.getString(Charset.forName(
                        "UTF8").newDecoder());
                assertTrue(strOutput.equals(strInput));
            }
        }
        // check if the last compressed data block can be decompressed
        // successfully.
        IoBuffer byteUncompressed = inflater.inflate(byteCompressed);
        String strOutput = byteUncompressed.getString(Charset.forName("UTF8")
                .newDecoder());
        assertTrue(strOutput.equals(strInput));
    }
}
