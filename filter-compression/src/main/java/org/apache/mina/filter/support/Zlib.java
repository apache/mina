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
package org.apache.mina.filter.support;

import java.io.IOException;

import org.apache.mina.common.ByteBuffer;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZStream;

/**
 * A helper class for interfacing with the JZlib library. This class acts both
 * as a compressor and decompressor, but only as one at a time.  The only
 * flush method supported is <tt>Z_SYNC_FLUSH</tt> also known as <tt>Z_PARTIAL_FLUSH</tt>
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class Zlib {
    public static final int COMPRESSION_MAX = JZlib.Z_BEST_COMPRESSION;

    public static final int COMPRESSION_MIN = JZlib.Z_BEST_SPEED;

    public static final int COMPRESSION_NONE = JZlib.Z_NO_COMPRESSION;

    public static final int COMPRESSION_DEFAULT = JZlib.Z_DEFAULT_COMPRESSION;

    public static final int MODE_DEFLATER = 1;

    public static final int MODE_INFLATER = 2;

    private int compressionLevel;

    private ZStream zStream = null;

    private int mode = -1;

    /**
     * @param compressionLevel the level of compression that should be used
     * @param mode the mode in which the instance will operate. Can be either 
     * of <tt>MODE_DEFLATER</tt> or <tt>MODE_INFLATER</tt>
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
     * @param inBuffer the {@link ByteBuffer} to be decompressed. The contents
     * of the buffer are transferred into a local byte array and the buffer is
     * flipped and returned intact.
     * @return the decompressed data. If not passed to the MINA methods that 
     * release the buffer automatically, the buffer has to be manually released 
     * @throws IOException if the decompression of the data failed for some reason.
     */
    public ByteBuffer inflate(ByteBuffer inBuffer) throws IOException {
        if (mode == MODE_DEFLATER) {
            throw new IllegalStateException("not initialized as INFLATER");
        }

        byte[] inBytes = new byte[inBuffer.remaining()];
        inBuffer.get(inBytes).flip();

        // We could probably do this better, if we're willing to return multiple buffers
        //  (e.g. with a callback function)
        byte[] outBytes = new byte[inBytes.length * 2];
        ByteBuffer outBuffer = ByteBuffer.allocate(outBytes.length);
        outBuffer.setAutoExpand(true);

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
                outBuffer.release();
                outBuffer = null;
                if (zStream.msg == null)
                    throw new IOException("Unknown error. Error code : "
                            + retval);
                else
                    throw new IOException("Unknown error. Error code : "
                            + retval + " and message : " + zStream.msg);
            }
        } while (zStream.avail_in > 0);

        return outBuffer.flip();
    }

    /**
     * @param inBuffer the buffer to be compressed. The contents are transferred
     * into a local byte array and the buffer is flipped and returned intact.
     * @return the buffer with the compressed data. If not passed to any of the
     * MINA methods that automatically release the buffer, the buffer has to be
     * released manually.
     * @throws IOException if the compression of teh buffer failed for some reason
     */
    public ByteBuffer deflate(ByteBuffer inBuffer) throws IOException {
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

        ByteBuffer outBuf = ByteBuffer
                .wrap(outBytes, 0, zStream.next_out_index);

        return outBuf;
    }

    /**
     * Cleans up the resources used by the compression library.
     */
    public void cleanUp() {
        if (zStream != null)
            zStream.free();
    }
}
