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
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

/**
 * An {@link IoFilter} which compresses all data using
 * <a href="http://www.jcraft.com/jzlib/">JZlib</a>.
 * Support for the LZW (DLCZ) algorithm is also planned.
 * <p>
 * This filter only supports compression using the <code>PARTIAL FLUSH</code> method,
 * since that is the only method useful when doing stream level compression.
 * <p>
 * This filter supports compression/decompression of the input and output
 * channels selectively.  It can also be enabled/disabled on the fly.
 * <p>
 * This filter does not discard the zlib objects, keeping them around for the
 * entire life of the filter.  This is because the zlib dictionary needs to
 * be built up over time, which is used during compression and decompression.
 * Over time, as repetitive data is sent over the wire, the compression efficiency
 * steadily increases.
 * <p>
 * Note that the zlib header is written only once. It is not necessary that
 * the data received after processing by this filter may not be complete due
 * to packet fragmentation.
 * <p>
 * It goes without saying that the other end of this stream should also have a
 * compatible compressor/decompressor using the same algorithm.
 * <p>
 * Note: a inflater limit has been added to protect the application from ZBomb
 * (a compressed buffer that when inflated will create a giant buffer).
 * It can be set using the CompressionFilter constructor, passing a forth argument
 * with the expected limit.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class CompressionFilter extends IoFilterAdapter {
    /**
     * Max compression level.  Will give the highest compression ratio, but
     * will also take more cpu time and is the slowest.
     */
    public static final int COMPRESSION_MAX = Zlib.COMPRESSION_MAX;

    /**
     * Provides the best speed at the price of a low compression ratio.
     */
    public static final int COMPRESSION_MIN = Zlib.COMPRESSION_MIN;

    /**
     * No compression done on the data.
     */
    public static final int COMPRESSION_NONE = Zlib.COMPRESSION_NONE;

    /**
     * The default compression level used. Provides the best balance
     * between speed and compression
     */
    public static final int COMPRESSION_DEFAULT = Zlib.COMPRESSION_DEFAULT;

    /**
     * A session attribute that stores the {@link Zlib} object used for compression.
     */
    private final AttributeKey DEFLATER = new AttributeKey(getClass(), "deflater");

    /**
     * A session attribute that stores the {@link Zlib} object used for decompression.
     */
    private final AttributeKey INFLATER = new AttributeKey(getClass(), "inflater");

    /**
     * A flag that allows you to disable compression once.
     */
    public static final AttributeKey DISABLE_COMPRESSION_ONCE = 
            new AttributeKey(CompressionFilter.class, "disableOnce");

    private boolean compressInbound = true;

    private boolean compressOutbound = true;

    private int compressionLevel;

    /** The maximum decompressed size, to avoid an OOM. Default to 1Mb */
    private int maxDecompressedSize;

    /** Maximum decompression ratio **/
    private long maxDecompressRatio;

    /** Grace size before decompression ratio check is enforced **/
    private long decompressRatioMinSize;

    /**
     * Creates a new instance which compresses outboud data and decompresses
     * inbound data with default compression level.
     */
    public CompressionFilter() {
        this(true, true, COMPRESSION_DEFAULT, Zlib.MAX_DECOMPRESSED_SIZE, Zlib.MAX_DECOMPRESS_RATIO, 
                Zlib.DECOMPRESS_RATIO_MIN_SIZE);
    }

    /**
     * Creates a new instance which compresses outboud data and decompresses
     * inbound data with the specified <code>compressionLevel</code>.
     *
     * @param compressionLevel the level of compression to be used. Must
     *                         be one of {@link #COMPRESSION_DEFAULT},
     *                         {@link #COMPRESSION_MAX},
     *                         {@link #COMPRESSION_MIN}, and
     *                         {@link #COMPRESSION_NONE}.
     */
    public CompressionFilter(final int compressionLevel) {
        this(true, true, compressionLevel, Zlib.MAX_DECOMPRESSED_SIZE, Zlib.MAX_DECOMPRESS_RATIO, 
                Zlib.DECOMPRESS_RATIO_MIN_SIZE);
    }

    /**
     * Creates a new instance.
     *
     * @param compressInbound <code>true</code> if data read is to be decompressed
     * @param compressOutbound <code>true</code> if data written is to be compressed
     * @param compressionLevel the level of compression to be used. Must
     *                         be one of {@link #COMPRESSION_DEFAULT},
     *                         {@link #COMPRESSION_MAX},
     *                         {@link #COMPRESSION_MIN}, and
     *                         {@link #COMPRESSION_NONE}.
     */
    public CompressionFilter(final boolean compressInbound, final boolean compressOutbound, 
            final int compressionLevel) {
        this(compressInbound, compressOutbound, compressionLevel, Zlib.MAX_DECOMPRESSED_SIZE, 
                Zlib.MAX_DECOMPRESS_RATIO, Zlib.DECOMPRESS_RATIO_MIN_SIZE);
    }

    /**
     * Creates a new instance.
     * <p>
     * Use thgis constructor if you want to set a limit to the inflated buffer size.
     *
     * @param compressInbound <code>true</code> if data read is to be decompressed
     * @param compressOutbound <code>true</code> if data written is to be compressed
     * @param compressionLevel the level of compression to be used. Must
     *                         be one of {@link #COMPRESSION_DEFAULT},
     *                         {@link #COMPRESSION_MAX},
     *                         {@link #COMPRESSION_MIN}, and
     *                         {@link #COMPRESSION_NONE}.
     * @param maxDecompressedSize The maximum size for a buffer when inflating some data
     * @since 2.2.8
     */
    public CompressionFilter(final boolean compressInbound, final boolean compressOutbound,
            final int compressionLevel, final int maxDecompressedSize) {
        this(compressInbound, compressOutbound, compressionLevel, maxDecompressedSize, Zlib.MAX_DECOMPRESS_RATIO, 
                Zlib.DECOMPRESS_RATIO_MIN_SIZE);
    }

    /**
     * Creates a new instance with explicit zip-bomb protection parameters.
     *
     * @param compressInbound <code>true</code> if data read is to be decompressed
     * @param compressOutbound <code>true</code> if data written is to be compressed
     * @param compressionLevel the level of compression to be used. Must
     *                         be one of {@link #COMPRESSION_DEFAULT},
     *                         {@link #COMPRESSION_MAX},
     *                         {@link #COMPRESSION_MIN}, and
     *                         {@link #COMPRESSION_NONE}.
     * @param maxDecompressedSize the maximum size for a buffer when inflating data
     * @param maxDecompressRatio the maximum allowed cumulative ratio of
     *                           decompressed to compressed bytes.
     *                           A value &lt;= 0 disables the check.
     * @param decompressRatioMinSize the minimum cumulative decompressed size
     *                               below which the ratio check is skipped.
     * @since 2.2.8
     */
    public CompressionFilter(final boolean compressInbound, final boolean compressOutbound,
            final int compressionLevel, final int maxDecompressedSize,
            final long maxDecompressRatio, final long decompressRatioMinSize) {
        this.compressionLevel = compressionLevel;
        this.compressInbound = compressInbound;
        this.compressOutbound = compressOutbound;
        this.maxDecompressedSize = maxDecompressedSize;
        this.maxDecompressRatio = maxDecompressRatio;
        this.decompressRatioMinSize = decompressRatioMinSize;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        Object compressedMessage = doFilterWrite(nextFilter, session, writeRequest);
        
        if (compressedMessage != null && compressedMessage != writeRequest.getMessage()) {
            writeRequest.setMessage( compressedMessage );
        }
        
        nextFilter.filterWrite(session, writeRequest);
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        if (!compressInbound || !(message instanceof IoBuffer)) {
            nextFilter.messageReceived(session, message);
            return;
        }

        Zlib inflater = (Zlib) session.getAttribute(INFLATER);
        
        if (inflater == null) {
            throw new IllegalStateException();
        }

        IoBuffer inBuffer = (IoBuffer) message;
        nextFilter.messageReceived(session, inflater.inflate(inBuffer));
    }
    
    /*
     * @see org.apache.mina.core.IoFilter#filterWrite(
     *          org.apache.mina.core.IoFilter.NextFilter, 
     *          org.apache.mina.core.IoSession, 
     *          org.apache.mina.core.IoFilter.WriteRequest)
     */
    protected Object doFilterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest)
            throws IOException {
        if (!compressOutbound) {
            return null;
        }

        if (session.containsAttribute(DISABLE_COMPRESSION_ONCE)) {
            // Remove the marker attribute because it is temporary.
            session.removeAttribute(DISABLE_COMPRESSION_ONCE);
            return null;
        }

        Zlib deflater = (Zlib) session.getAttribute(DEFLATER);
        
        if (deflater == null) {
            throw new IllegalStateException();
        }

        IoBuffer inBuffer = (IoBuffer) writeRequest.getMessage();
        
        if (!inBuffer.hasRemaining()) {
            // Ignore empty buffers
            return null;
        } else {
            return deflater.deflate(inBuffer);
        }
    }

    @Override
    public void onPreAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
        if (parent.contains(CompressionFilter.class)) {
            throw new IllegalStateException("Only one " + CompressionFilter.class + " is permitted.");
        }

        Zlib deflater = new Zlib(compressionLevel, Zlib.MODE_INFLATER, maxDecompressedSize, 
                maxDecompressRatio, decompressRatioMinSize);
        Zlib inflater = new Zlib(compressionLevel, Zlib.MODE_INFLATER, maxDecompressedSize, 
                maxDecompressRatio, decompressRatioMinSize);

        IoSession session = parent.getSession();

        session.setAttribute(DEFLATER, deflater);
        session.setAttribute(INFLATER, inflater);
    }

    /**
     * Set the compression level. On of:
     * <ul>
     *   <li>Zlib.COMPRESSION_DEFAULT (-1)</li>
     *   <li>Zlib.COMPRESSION_NONE (0)</li>
     *   <li>Zlib.COMPRESSION_MIN (1)</li>
     *   <li>Zlib.COMPRESSION_MAX (9)</li>
     * </ul>
     * 
     * @param compressionLevel The compression level to set
     * @ The CompressionFilter instance
     */
    public CompressionFilter setCompressionLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
        
        return this;
    }

    /**
     * Set The maximum decompressed size, to avoid an OOM. Default to 1Mb
     *
     * @param maxDecompressedSize The maximum decompressed size
     * @return The CompressionFilter instance
     */
    public CompressionFilter setMaxDecompressedSize(int maxDecompressedSize) {
        this.maxDecompressedSize = maxDecompressedSize;
        
        return this;
    }

    /**
     * Grace size before decompression ratio check is enforced. Default to 1Mb.
     *
     * @param decompressRatioMinSize The maximum decompressed size before the ratio is checked
     * @return The CompressionFilter instance
     */
    public CompressionFilter setDecompressRatioMinSize(long decompressRatioMinSize) {
        this.decompressRatioMinSize = decompressRatioMinSize;
        
        return this;
    }
    
    /**
     * Set the max allowed compression ratio. If the inflated buffer exceed this ratio,
     * an error will be generated. Note that the  <code>decompressRatioMinSize</code> parameter
     * can be used to avoid bailing out for small inflated files with a high compression ratio.
     * 
     * @param maxDecompressRatio The maximum allowed compression ratio. Defaults to 100.
     * @return
     */
    public CompressionFilter setMaxDecompressRatio(long maxDecompressRatio) {
        this.maxDecompressRatio = maxDecompressRatio;
        
        return this;
    }

    /**
     * @return <code>true</code> if incoming data is being compressed.
     */
    public boolean isCompressInbound() {
        return compressInbound;
    }

    /**
     * Sets if incoming data has to be compressed.
     * 
     * @param compressInbound <code>true</code> if the incoming data has to be compressed
     */
    public void setCompressInbound(boolean compressInbound) {
        this.compressInbound = compressInbound;
    }

    /**
     * @return <code>true</code> if the filter is compressing data being written.
     */
    public boolean isCompressOutbound() {
        return compressOutbound;
    }

    /**
     * Set if outgoing data has to be compressed.
     * 
     * @param compressOutbound <code>true</code> if the outgoing data has to be compressed
     */
    public void setCompressOutbound(boolean compressOutbound) {
        this.compressOutbound = compressOutbound;
    }

    @Override
    public void onPostRemove(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
        super.onPostRemove(parent, name, nextFilter);
        IoSession session = parent.getSession();
        if (session == null) {
            return;
        }

        Zlib inflater = (Zlib) session.getAttribute(INFLATER);
        Zlib deflater = (Zlib) session.getAttribute(DEFLATER);
        if (deflater != null) {
            deflater.cleanUp();
        }

        if (inflater != null) {
            inflater.cleanUp();
        }
    }
}
