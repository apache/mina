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

import java.io.IOException;

import org.apache.mina.core.buffer.IoBuffer;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZStream;

/**
 * A helper class for interfacing with the JZlib library. This class acts both
 * as a compressor and decompressor, but only as one at a time.  The only
 * flush method supported is {@code Z_SYNC_FLUSH} also known as {@code Z_PARTIAL_FLUSH}
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
class Zlib {
    /** Try o get the best possible compression */
    public static final int COMPRESSION_MAX = JZlib.Z_BEST_COMPRESSION;

    /** Favor speed over compression ratio */
    public static final int COMPRESSION_MIN = JZlib.Z_BEST_SPEED;

    /** No compression */
    public static final int COMPRESSION_NONE = JZlib.Z_NO_COMPRESSION;

    /** Default compression */
    public static final int COMPRESSION_DEFAULT = JZlib.Z_DEFAULT_COMPRESSION;

    /** Compression mode */
    public static final int MODE_DEFLATER = 1;

    /** Uncompress mode */
    public static final int MODE_INFLATER = 2;

    /** The requested compression level */
    private int compressionLevel;
    
    /** The maximum size of an inflated buffer. Default to 1Mb */
    /* Package protected */ 
    static final int MAX_DECOMPRESSED_SIZE = Integer.MAX_VALUE;

    /**
     * Default maximum decompression ratio (decompressed / compressed).
     */
    /* Package protected */
    static final long MAX_DECOMPRESS_RATIO = 100L;

    /**
     * Grace size before decompression ratio check is enforced.
     *
     * <p>Below this threshold the check is skipped to avoid false positives on small payloads where framing/header
     * overhead dominates the ratio.</p>
     */
    /* Package protected */
    static final long DECOMPRESS_RATIO_MIN_SIZE = 1024L * 1024L;

    private int maxDecompressedSize = MAX_DECOMPRESSED_SIZE;

    private long maxDecompressRatio = MAX_DECOMPRESS_RATIO;

    private long decompressRatioMinSize = DECOMPRESS_RATIO_MIN_SIZE;
 
    /** The inner stream used to inflate or deflate the data */
    private ZStream zStream = null;

    /** The selected operation mode : INFLATE or DEFLATE */
    private int mode = -1;

    /**
     * Creates an instance of the ZLib class.
     * 
     * @param compressionLevel the level of compression that should be used. One of
     * {@code COMPRESSION_MAX}, {@code COMPRESSION_MIN},
     * {@code COMPRESSION_NONE} or {@code COMPRESSION_DEFAULT}
     * @param mode the mode in which the instance will operate. Can be either
     * of {@code MODE_DEFLATER} or {@code MODE_INFLATER}
     * @throws IllegalArgumentException if the mode is incorrect
     */
    public Zlib(int compressionLevel, int mode) {
        this(compressionLevel, mode, MAX_DECOMPRESSED_SIZE, MAX_DECOMPRESS_RATIO, DECOMPRESS_RATIO_MIN_SIZE);
    }
    

    /**
     * Creates an instance of the ZLib class.
     * 
     * @param compressionLevel the level of compression that should be used. One of
     * <code>COMPRESSION_MAX</code>, <code>COMPRESSION_MIN</code>,
     * <code>COMPRESSION_NONE</code> or <code>COMPRESSION_DEFAULT</code>
     * @param mode the mode in which the instance will operate. Can be either
     * of <code>MODE_DEFLATER</code> or <code>MODE_INFLATER</code>
     * @param maxDecompressedSize the maximum inflation size for a buffer
     * @param maxDecompressRatio the maximum allowed ratio of decompressed to
     * compressed bytes, evaluated cumulatively over the lifetime of this
     * inflater. A value &lt;= 0 disables the check.
     * @param decompressRatioMinSize the minimum cumulative decompressed size
     * (in bytes) below which the ratio check is skipped.
     * @throws IllegalArgumentException if the mode is incorrect
     */
    public Zlib(int compressionLevel, int mode, int maxDecompressedSize,
            long maxDecompressRatio, long decompressRatioMinSize) {
        switch (compressionLevel) {
            case COMPRESSION_MAX:
            case COMPRESSION_MIN:
            case COMPRESSION_NONE:
            case COMPRESSION_DEFAULT:
                this.compressionLevel = compressionLevel;
                break;
            default:
                throw new IllegalArgumentException("invalid compression level specified");
        }

        // create a new instance of ZStream. This will be done only once.
        zStream = new ZStream();

        switch (mode) {
            case MODE_DEFLATER:
                zStream.deflateInit(this.compressionLevel);
                break;
            case MODE_INFLATER:
                this.maxDecompressedSize = maxDecompressedSize;
                this.maxDecompressRatio = maxDecompressRatio;
                this.decompressRatioMinSize = decompressRatioMinSize;
                zStream.inflateInit();
                break;
            default:
                throw new IllegalArgumentException("invalid mode specified");
        }

        this.mode = mode;
    }
    

    /**
     * Uncompress the given buffer, returning it in a new buffer.
     * 
     * @param inBuffer the {@link IoBuffer} to be decompressed. The contents
     * of the buffer are transferred into a local byte array and the buffer is
     * flipped and returned intact.
     * @return the decompressed data
     * @throws IOException if the decompression of the data failed for some reason.
     * @throws IllegalArgumentException if the mode is not <code>MODE_DEFLATER</code>
     */
    public IoBuffer inflate(IoBuffer inBuffer) throws IOException {
        if (mode == MODE_DEFLATER) {
            throw new IllegalStateException("not initialized as INFLATER");
        }

        byte[] inBytes = new byte[inBuffer.remaining()];
        inBuffer.get(inBytes).flip();

        // We could probably do this better, if we're willing to return multiple buffers
        // (e.g. with a callback function)
        byte[] outBytes = new byte[inBytes.length * 2];
        IoBuffer outBuffer = IoBuffer.allocate(outBytes.length);
        outBuffer.setAutoExpand(true);

        synchronized (zStream) {
            zStream.next_in = inBytes;
            zStream.next_in_index = 0;
            zStream.avail_in = inBytes.length;
            zStream.next_out = outBytes;
            zStream.next_out_index = 0;
            zStream.avail_out = outBytes.length;
            int retval = 0;

            do {
                retval = zStream.inflate(JZlib.Z_SYNC_FLUSH);
                switch (retval) {
                    case JZlib.Z_OK:
                        // completed decompression, lets copy data and get out
                    case JZlib.Z_BUF_ERROR:
                        // Try to avoid exhausting the JVM memory by controling the resulting buffer 
                        // size after inflation
                        if (outBuffer.position() + zStream.next_out_index > maxDecompressedSize) {
                            throw new IOException("decompressed size exceeds max " + maxDecompressedSize);
                        }
                       
                        checkDecompressRatio();
 
                        // need more space for output. store current output and get more
                        outBuffer.put(outBytes, 0, zStream.next_out_index);
                        zStream.next_out_index = 0;
                        zStream.avail_out = outBytes.length;
                        break;
                    default:
                        // unknown error
                        outBuffer = null;
                        if (zStream.msg == null) {
                            throw new IOException("Unknown error. Error code : " + retval);
                        } else {
                            throw new IOException("Unknown error. Error code : " + retval + " and message : " + zStream.msg);
                        }
                }
            } while (zStream.avail_in > 0);
            
            cleanUp();
        }

        return outBuffer.flip();
    }

    /**
     * Compress the input. The result will be put in a new buffer.
     *  
     * @param inBuffer the buffer to be compressed. The contents are transferred
     * into a local byte array and the buffer is flipped and returned intact.
     * @return the buffer with the compressed data
     * @throws IOException if the compression of teh buffer failed for some reason
     * @throws IllegalStateException if the mode is not <code>MODE_DEFLATER</code>
     */
    public IoBuffer deflate(IoBuffer inBuffer) throws IOException {
        if (mode == MODE_INFLATER) {
            throw new IllegalStateException("not initialized as DEFLATER");
        }

        byte[] inBytes = new byte[inBuffer.remaining()];
        inBuffer.get(inBytes);

        // according to spec, destination buffer should be 0.1% larger
        // than source length plus 12 bytes. We add a single byte to safeguard
        // against rounds that round down to the smaller value
        int outLen = (int) Math.round(inBytes.length * 1.001) + 1 + 12;
        byte[] outBytes = new byte[outLen];

        synchronized (zStream) {
            zStream.next_in = inBytes;
            zStream.next_in_index = 0;
            zStream.avail_in = inBytes.length;
            zStream.next_out = outBytes;
            zStream.next_out_index = 0;
            zStream.avail_out = outBytes.length;

            int retval = zStream.deflate(JZlib.Z_SYNC_FLUSH);
            if (retval != JZlib.Z_OK) {
                outBytes = null;
                inBytes = null;
                throw new IOException("Compression failed with return value : " + retval);
            }

            IoBuffer outBuf = IoBuffer.wrap(outBytes, 0, zStream.next_out_index);

            cleanUp();
            
            return outBuf;
        }
    }

    /**
     * Checks the cumulative decompression ratio against the configured maximum.
     *
     * @throws IOException if the cumulative ratio exceeds {@code maxDecompressRatio}
     */
    private void checkDecompressRatio() throws IOException {
        if (maxDecompressRatio <= 0L) {
            return;
        }
        long totalOut = zStream.getTotalOut();
        long totalIn = zStream.getTotalIn();
        if (totalIn > 0L && totalOut > decompressRatioMinSize && totalOut / totalIn > maxDecompressRatio) {
            throw new IOException("decompression ratio " + (totalOut / totalIn) + " exceeds max " + maxDecompressRatio);
        }
    }

    /**
     * Cleans up the resources used by the compression library.
     */
    public void cleanUp() {
        if (zStream != null) {
            zStream.free();
        }
    }
}
