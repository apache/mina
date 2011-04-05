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
 * flush method supported is <tt>Z_SYNC_FLUSH</tt> also known as <tt>Z_PARTIAL_FLUSH</tt>
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

    /** The inner stream used to inflate or deflate the data */
    private ZStream zStream = null;

    /** The selected operation mode : INFLATE or DEFLATE */
    private int mode = -1;

    /**
     * Creates an instance of the ZLib class.
     * 
     * @param compressionLevel the level of compression that should be used. One of
     * <tt>COMPRESSION_MAX</tt>, <tt>COMPRESSION_MIN</tt>,
     * <tt>COMPRESSION_NONE</tt> or <tt>COMPRESSION_DEFAULT</tt>
     * @param mode the mode in which the instance will operate. Can be either
     * of <tt>MODE_DEFLATER</tt> or <tt>MODE_INFLATER</tt>
     * @throws IllegalArgumentException if the mode is incorrect
     */
    public Zlib(int compressionLevel, int mode) {
        switch (compressionLevel) {
        case COMPRESSION_MAX:
        case COMPRESSION_MIN:
        case COMPRESSION_NONE:
        case COMPRESSION_DEFAULT:
            this.compressionLevel = compressionLevel;
            break;
        default:
            throw new IllegalArgumentException(
                    "invalid compression level specified");
        }

        // create a new instance of ZStream. This will be done only once.
        zStream = new ZStream();

        switch (mode) {
        case MODE_DEFLATER:
            zStream.deflateInit(this.compressionLevel);
            break;
        case MODE_INFLATER:
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

        synchronized( zStream ) {
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
                    // need more space for output. store current output and get more
                    outBuffer.put(outBytes, 0, zStream.next_out_index);
                    zStream.next_out_index = 0;
                    zStream.avail_out = outBytes.length;
                    break;
                default:
                    // unknown error
                    outBuffer = null;
                    if (zStream.msg == null) {
                        throw new IOException("Unknown error. Error code : "
                                + retval);
                    } else {
                        throw new IOException("Unknown error. Error code : "
                                + retval + " and message : " + zStream.msg);
                    }
                }
            } while (zStream.avail_in > 0);
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
        inBuffer.get(inBytes).flip();

        // according to spec, destination buffer should be 0.1% larger
        // than source length plus 12 bytes. We add a single byte to safeguard
        // against rounds that round down to the smaller value
        int outLen = (int) Math.round(inBytes.length * 1.001) + 1 + 12;
        byte[] outBytes = new byte[outLen];

        synchronized(zStream) {
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
                throw new IOException("Compression failed with return value : "
                        + retval);
            }
    
            IoBuffer outBuf = IoBuffer
                    .wrap(outBytes, 0, zStream.next_out_index);

            return outBuf;
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
