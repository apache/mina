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
package org.apache.mina.filter.buffer;

import java.io.BufferedOutputStream;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.util.LazyInitializedCacheMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link IoFilter} implementation used to buffer outgoing {@link WriteRequest} almost 
 * like what {@link BufferedOutputStream} does. Using this filter allows to be less dependent 
 * from network latency. It is also useful when a session is generating very small messages 
 * too frequently and consequently generating unnecessary traffic overhead.
 * 
 * Please note that it should always be placed before the {@link ProtocolCodecFilter} 
 * as it only handles {@link WriteRequest}'s carrying {@link IoBuffer} objects.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M2
 * @org.apache.xbean.XBean
 */
public final class BufferedWriteFilter extends IoFilterAdapter {
    private final Logger logger = LoggerFactory
            .getLogger(BufferedWriteFilter.class);

    /**
     * Default buffer size value in bytes.
     */
    public final static int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * The buffer size allocated for each new session's buffer.
     */
    private int bufferSize = DEFAULT_BUFFER_SIZE;

    /**
     * The map that matches an {@link IoSession} and it's {@link IoBuffer}
     * buffer.
     */
    private final LazyInitializedCacheMap<IoSession, IoBuffer> buffersMap;

    /**
     * Default constructor. Sets buffer size to {@link #DEFAULT_BUFFER_SIZE}
     * bytes. Uses a default instance of {@link ConcurrentHashMap}.
     */
    public BufferedWriteFilter() {
        this(DEFAULT_BUFFER_SIZE, null);
    }

    /**
     * Constructor which sets buffer size to <code>bufferSize</code>.Uses a default 
     * instance of {@link ConcurrentHashMap}.
     * 
     * @param bufferSize the new buffer size
     */
    public BufferedWriteFilter(int bufferSize) {
        this(bufferSize, null);
    }

    /**
     * Constructor which sets buffer size to <code>bufferSize</code>. If 
     * <code>buffersMap</code> is null then a default instance of {@link ConcurrentHashMap} 
     * is created else the provided instance is used.
     * 
     * @param bufferSize the new buffer size
     * @param buffersMap the map to use for storing each session buffer 
     */
    public BufferedWriteFilter(int bufferSize,
            LazyInitializedCacheMap<IoSession, IoBuffer> buffersMap) {
        super();
        this.bufferSize = bufferSize;
        if (buffersMap == null) {
            this.buffersMap = new LazyInitializedCacheMap<IoSession, IoBuffer>();
        } else {
            this.buffersMap = buffersMap;
        }
    }

    /**
     * Returns buffer size.
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Sets the buffer size but only for the newly created buffers.
     * 
     * @param bufferSize the new buffer size
     */
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws Exception if <code>writeRequest.message</code> isn't an
     *                   {@link IoBuffer} instance.
     */
    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {

        Object data = writeRequest.getMessage();

        if (data instanceof IoBuffer) {
            write(session, (IoBuffer) data);
        } else {
            throw new IllegalArgumentException(
                    "This filter should only buffer IoBuffer objects");
        }
    }

    /**
     * Writes an {@link IoBuffer} to the session's buffer.
     * 
     * @param session the session to which a write is requested
     * @param data the data to buffer
     */
    private void write(IoSession session, IoBuffer data) {
        IoBuffer dest = buffersMap.putIfAbsent(session,
                new IoBufferLazyInitializer(bufferSize));

        write(session, data, dest);
    }

    /**
     * Writes <code>data</code> {@link IoBuffer} to the <code>buf</code>
     * {@link IoBuffer} which buffers write requests for the
     * <code>session</code> {@ link IoSession} until buffer is full 
     * or manually flushed.
     * 
     * @param session the session where buffer will be written
     * @param data the data to buffer
     * @param buf the buffer where data will be temporarily written 
     */
    private void write(IoSession session, IoBuffer data, IoBuffer buf) {
        try {
            int len = data.remaining();
            if (len >= buf.capacity()) {
                /*
                 * If the request length exceeds the size of the output buffer,
                 * flush the output buffer and then write the data directly.
                 */
                NextFilter nextFilter = session.getFilterChain().getNextFilter(
                        this);
                internalFlush(nextFilter, session, buf);
                nextFilter.filterWrite(session, new DefaultWriteRequest(data));
                return;
            }
            if (len > (buf.limit() - buf.position())) {
                internalFlush(session.getFilterChain().getNextFilter(this),
                        session, buf);
            }
            synchronized (buf) {
                buf.put(data);
            }
        } catch (Throwable e) {
            session.getFilterChain().fireExceptionCaught(e);
        }
    }

    /**
     * Internal method that actually flushes the buffered data.
     * 
     * @param nextFilter the {@link NextFilter} of this filter
     * @param session the session where buffer will be written
     * @param buf the data to write
     * @throws Exception if a write operation fails
     */
    private void internalFlush(NextFilter nextFilter, IoSession session,
            IoBuffer buf) throws Exception {
        IoBuffer tmp = null;
        synchronized (buf) {
            buf.flip();
            tmp = buf.duplicate();
            buf.clear();
        }
        logger.debug("Flushing buffer: {}", tmp);
        nextFilter.filterWrite(session, new DefaultWriteRequest(tmp));
    }

    /**
     * Flushes the buffered data.
     * 
     * @param session the session where buffer will be written
     */
    public void flush(IoSession session) {
        try {
            internalFlush(session.getFilterChain().getNextFilter(this),
                    session, buffersMap.get(session));
        } catch (Throwable e) {
            session.getFilterChain().fireExceptionCaught(e);
        }
    }

    /**
     * Internal method that actually frees the {@link IoBuffer} that contains
     * the buffered data that has not been flushed.
     * 
     * @param session the session we operate on
     */
    private void free(IoSession session) {
        IoBuffer buf = buffersMap.remove(session);
        if (buf != null) {
            buf.free();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exceptionCaught(NextFilter nextFilter, IoSession session,
            Throwable cause) throws Exception {
        free(session);
        nextFilter.exceptionCaught(session, cause);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionClosed(NextFilter nextFilter, IoSession session)
            throws Exception {
        free(session);
        nextFilter.sessionClosed(session);
    }
}