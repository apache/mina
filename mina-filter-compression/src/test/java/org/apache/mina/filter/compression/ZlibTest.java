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
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Random;

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

    private IoBuffer deflateZeros(int size) throws IOException {
        try {
            return new Zlib(Zlib.COMPRESSION_MAX, Zlib.MODE_DEFLATER)
                    .deflate(IoBuffer.wrap(new byte[size]));
        } catch (Exception e) {
            throw new AssertionError("failed to deflate test fixture", e);
        }
    }

    private IoBuffer deflateRandom(int size) throws IOException {
        try {
            byte[] data = new byte[size];
            new Random(0).nextBytes(data);
            return new Zlib(Zlib.COMPRESSION_MAX, Zlib.MODE_DEFLATER)
                    .deflate(IoBuffer.wrap(data));
        } catch (Exception e) {
            throw new AssertionError("failed to deflate test fixture", e);
        }
    }

    @Test
    public void testCompression() throws Exception {
        String strInput = "";

        // increase the count to as many as required to generate a long
        // string for input
        for (int i = 0; i < 10; i++) {
            strInput += "The quick brown fox jumps over the lazy dog.  ";
        }
        IoBuffer byteInput = IoBuffer.wrap(strInput.getBytes(StandardCharsets.UTF_8));

        // increase the count to have the compression and decompression
        // done using the same instance of Zlib
        for (int i = 0; i < 5; i++) {
            IoBuffer byteCompressed = deflater.deflate(byteInput);
            IoBuffer byteUncompressed = inflater.inflate(byteCompressed);
            String strOutput = byteUncompressed.getString(Charset.forName("UTF8").newDecoder());
            assertTrue(strOutput.equals(strInput));
            byteInput.flip();
        }
    }

    @Test
    public void testCorruptedData() throws Exception {
        String strInput = "Hello World";
        IoBuffer byteInput = IoBuffer.wrap(strInput.getBytes(StandardCharsets.UTF_8));

        IoBuffer byteCompressed = deflater.deflate(byteInput);
        // change the contents to something else. Since this doesn't check
        // for integrity, it wont throw an exception
        byteCompressed.put(5, (byte) 0xa);
        IoBuffer byteUncompressed = inflater.inflate(byteCompressed);
        String strOutput = byteUncompressed.getString(StandardCharsets.UTF_8.newDecoder());
        assertFalse(strOutput.equals(strInput));
    }

    @Test
    public void testCorruptedHeader() throws Exception {
        String strInput = "Hello World";
        IoBuffer byteInput = IoBuffer.wrap(strInput.getBytes(StandardCharsets.UTF_8));

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
        IoBuffer byteInput = IoBuffer.wrap(strInput.getBytes(StandardCharsets.UTF_8));
        IoBuffer byteCompressed = null;

        for (int i = 0; i < 5; i++) {
            byteCompressed = deflater.deflate(byteInput);
            if (i == 0) {
                // decompress the first compressed output since it contains
                // the zlib header, which will not be generated for further
                // compressions done with the same instance
                IoBuffer byteUncompressed = inflater.inflate(byteCompressed);
                String strOutput = byteUncompressed.getString(Charset.forName("UTF8").newDecoder());
                assertTrue(strOutput.equals(strInput));
            }
            
            byteInput.flip();
        }
        // check if the last compressed data block can be decompressed
        // successfully.
        IoBuffer byteUncompressed = inflater.inflate(byteCompressed);
        String strOutput = byteUncompressed.getString(Charset.forName("UTF8").newDecoder());
        assertTrue(strOutput.equals(strInput));
    }
    
    
    /**
     * Test the inflater with no limit
     * We create buffers of various sizes:
     * <ul>
     *   <li>A 1MB buffer that once compressed should inflate properly
     *   <li>A 10MB buffer that once compressed should inflate properly
     *   <li>
     * </ul>
     * @throws Exception
     */
    @Test
    public void testZBombDataNoLimit() throws Exception {
        // Create an inflater with no size limit and the ratio check disabled
        Zlib inflaterNoLimit = new Zlib(Zlib.COMPRESSION_MAX, Zlib.MODE_INFLATER,
                Zlib.MAX_DECOMPRESSED_SIZE, 0L, 0L);

        // Try a 10MB buffer bomb. Should succeed
        IoBuffer byteCompressed = deflateZeros(1_024 * 1_024 * 10);

        // Should be fine
        inflaterNoLimit.inflate(byteCompressed);
    }


    /**
     * Test the inflater default limit.
     * We create buffers of various sizes:
     * <ul>
     *   <li>A 1MB Buffer that once compressed should inflate properly
     *   <li>A 1MB+1byte buffer that once compressed should generate an exception when inflated
     *   <li>
     * </ul>
     * @throws Exception
     */
    @Test
    public void testZBombData() throws Exception {
        // Create an inflater with a 1Mb size limit and the ratio check disabled
        // so this test stays focused on the size limit.
        Zlib inflaterWithLimit = new Zlib(Zlib.COMPRESSION_MAX, Zlib.MODE_INFLATER, 1_024*1_024, 0L, 0L);

        // Both inputs are fed to the same inflater as a continuous zlib
        // stream, so use the shared deflater rather than the fresh-stream
        // deflateZeros() helper.

        // Right at the size limit: should succeed.
        inflaterWithLimit.inflate(deflater.deflate(IoBuffer.wrap(new byte[1_024 * 1_024])));

        // One byte over the size limit: should throw.
        IoBuffer overLimit = deflater.deflate(IoBuffer.wrap(new byte[1_024 * 1_024 + 1]));
        assertThrows(IOException.class, () -> inflaterWithLimit.inflate(overLimit));
    }


    /**
     * A highly compressible payload that exceeds both the default ratio (100)
     * and the default ratio min-size threshold should be rejected by the
     * inflater.
     */
    @Test
    public void testDecompressRatioExceeded() throws Exception {
        // 64KiB of zeros compresses to well under 64KiB/100 bytes.
        int size = 64 * 1_024;
        IoBuffer byteCompressed = deflateZeros(size);
        int compressedSize = byteCompressed.remaining();
        long actualCompressRatio = size / compressedSize;

        // Inflater configured one ratio step below the actual payload's
        // ratio: the inflate call must throw.
        Zlib inflater = new Zlib(Zlib.COMPRESSION_MAX, Zlib.MODE_INFLATER, Zlib.MAX_DECOMPRESSED_SIZE, actualCompressRatio - 1, 0L);
        assertThrows(IOException.class, () -> inflater.inflate(byteCompressed));
    }


    /**
     * The ratio check must not fire while the cumulative decompressed size is
     * below the configured min-size threshold.
     */
    @Test
    public void testDecompressRatioBelowMinSize() throws Exception {
        int size = 1_024 * 1_024;
        IoBuffer byteCompressed = deflateZeros(size);

        // Ratio of 100 would normally trip on this payload; raise the min-size
        // threshold above the payload so the check is skipped.
        Zlib inflater = new Zlib(Zlib.COMPRESSION_MAX, Zlib.MODE_INFLATER, Zlib.MAX_DECOMPRESSED_SIZE, 1L, size);
        inflater.inflate(byteCompressed);
    }


    /**
     * The ratio check is cumulative across multiple inflate() calls on the
     * same stream, so a bomb cannot bypass it by being split into small
     * fragments.
     */
    @Test
    public void testDecompressRatioCumulative() throws Exception {
        Zlib inflater = new Zlib(Zlib.COMPRESSION_MAX, Zlib.MODE_INFLATER, Zlib.MAX_DECOMPRESSED_SIZE, 1L, Zlib.DECOMPRESS_RATIO_MIN_SIZE);

        // Below the min-size gate
        int chunkSize = (int) Zlib.DECOMPRESS_RATIO_MIN_SIZE;
        inflater.inflate(deflater.deflate(IoBuffer.wrap(new byte[chunkSize])));

        // Exceeds the min-size gate
        IoBuffer second = deflater.deflate(IoBuffer.wrap(new byte[chunkSize]));
        assertThrows(IOException.class, () -> inflater.inflate(second));
    }


    /**
     * An empty input buffer produces no decompressed output, so the ratio
     * check must not fire even with a pathologically tight max ratio of 1
     * and the min-size gate wide open.
     */
    @Test
    public void testInflateEmptyBuffer() throws Exception {
        Zlib inflater = new Zlib(Zlib.COMPRESSION_MAX, Zlib.MODE_INFLATER, Zlib.MAX_DECOMPRESSED_SIZE, 1L, 0L);
        inflater.inflate(IoBuffer.allocate(0));
    }


    /**
     * The default-constructor inflater must apply the documented defaults
     * (max ratio = 100, min-size gate = 1 MiB). Three legs:
     * <ul>
     *   <li>Small + high ratio: zeros below the gate — must succeed (catches a min-size drop).</li>
     *   <li>Large + low ratio: pseudo-random bytes above the gate — must succeed (catches an unintended max-ratio bump).</li>
     *   <li>Large + high ratio: cumulative zeros above the gate — must throw (catches either default being effectively disabled).</li>
     * </ul>
     */
    @Test
    public void testDefaults() throws Exception {
        // Leg 1: small + high ratio. Below the 1 MiB gate, check is skipped.
        Zlib smallHighRatio = new Zlib(Zlib.COMPRESSION_MAX, Zlib.MODE_INFLATER);
        smallHighRatio.inflate(deflateZeros(512 * 1_024));

        // Leg 2: large + low ratio. Above the gate, but ratio ≈ 1 << 100.
        Zlib largeLowRatio = new Zlib(Zlib.COMPRESSION_MAX, Zlib.MODE_INFLATER);
        largeLowRatio.inflate(deflateRandom(2 * 1_024 * 1_024));

        // Leg 3: large + high ratio. Above the gate, ratio >> 100, throws.
        Zlib largeHighRatio = new Zlib(Zlib.COMPRESSION_MAX, Zlib.MODE_INFLATER);
        IoBuffer bomb = deflateZeros(2 * 1_024 * 1_024);
        assertThrows(IOException.class, () -> largeHighRatio.inflate(bomb));
    }
}

