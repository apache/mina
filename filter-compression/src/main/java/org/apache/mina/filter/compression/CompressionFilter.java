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
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.util.WriteRequestFilter;

/**
 * An {@link IoFilter} which compresses all data using
 * <a href="http://www.jcraft.com/jzlib/">JZlib</a>.
 * Support for the LZW (DLCZ) algorithm is also planned.
 * <p>
 * This filter only supports compression using the <tt>PARTIAL FLUSH</tt> method,
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
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class CompressionFilter extends WriteRequestFilter {
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
    public static final AttributeKey DISABLE_COMPRESSION_ONCE = new AttributeKey(CompressionFilter.class, "disableOnce"); 

    private boolean compressInbound = true;

    private boolean compressOutbound = true;

    private int compressionLevel;

    /**
     * Creates a new instance which compresses outboud data and decompresses
     * inbound data with default compression level.
     */
    public CompressionFilter() {
        this(true, true, COMPRESSION_DEFAULT);
    }

    /**
     * Creates a new instance which compresses outboud data and decompresses
     * inbound data with the specified <tt>compressionLevel</tt>.
     *
     * @param compressionLevel the level of compression to be used. Must
     *                         be one of {@link #COMPRESSION_DEFAULT},
     *                         {@link #COMPRESSION_MAX},
     *                         {@link #COMPRESSION_MIN}, and
     *                         {@link #COMPRESSION_NONE}.
     */
    public CompressionFilter(final int compressionLevel) {
        this(true, true, compressionLevel);
    }

    /**
     * Creates a new instance.
     *
     * @param compressInbound <tt>true</tt> if data read is to be decompressed
     * @param compressOutbound <tt>true</tt> if data written is to be compressed
     * @param compressionLevel the level of compression to be used. Must
     *                         be one of {@link #COMPRESSION_DEFAULT},
     *                         {@link #COMPRESSION_MAX},
     *                         {@link #COMPRESSION_MIN}, and
     *                         {@link #COMPRESSION_NONE}.
     */
    public CompressionFilter(final boolean compressInbound,
            final boolean compressOutbound, final int compressionLevel) {
        this.compressionLevel = compressionLevel;
        this.compressInbound = compressInbound;
        this.compressOutbound = compressOutbound;
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session,
            Object message) throws Exception {
        if (!compressInbound || !(message instanceof IoBuffer)) {
            nextFilter.messageReceived(session, message);
            return;
        }

        Zlib inflater = (Zlib) session.getAttribute(INFLATER);
        if (inflater == null) {
            throw new IllegalStateException();
        }

        IoBuffer inBuffer = (IoBuffer) message;
        IoBuffer outBuffer = inflater.inflate(inBuffer);
        nextFilter.messageReceived(session, outBuffer);
    }

    /*
     * @see org.apache.mina.core.IoFilter#filterWrite(org.apache.mina.core.IoFilter.NextFilter, org.apache.mina.core.IoSession, org.apache.mina.core.IoFilter.WriteRequest)
     */
    @Override
    protected Object doFilterWrite(
            NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws IOException {
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
    public void onPreAdd(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
        if (parent.contains(CompressionFilter.class)) {
            throw new IllegalStateException(
                    "Only one " + CompressionFilter.class + " is permitted.");
        }

        Zlib deflater = new Zlib(compressionLevel, Zlib.MODE_DEFLATER);
        Zlib inflater = new Zlib(compressionLevel, Zlib.MODE_INFLATER);

        IoSession session = parent.getSession();

        session.setAttribute(DEFLATER, deflater);
        session.setAttribute(INFLATER, inflater);
    }

    /**
     * Returns <tt>true</tt> if incoming data is being compressed.
     */
    public boolean isCompressInbound() {
        return compressInbound;
    }

    /**
     * Sets if incoming data has to be compressed.
     */
    public void setCompressInbound(boolean compressInbound) {
        this.compressInbound = compressInbound;
    }

    /**
     * Returns <tt>true</tt> if the filter is compressing data being written.
     */
    public boolean isCompressOutbound() {
        return compressOutbound;
    }

    /**
     * Set if outgoing data has to be compressed.
     */
    public void setCompressOutbound(boolean compressOutbound) {
        this.compressOutbound = compressOutbound;
    }

    @Override
    public void onPostRemove(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
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
