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
package org.apache.mina.filter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;

/**
 * Filter implementation which makes it possible to write {@link InputStream}
 * objects directly using {@link IoSession#write(Object)}. When an
 * {@link InputStream} is written to a session this filter will read the bytes
 * from the stream into {@link ByteBuffer} objects and write those buffers
 * to the next filter. When end of stream has been reached this filter will
 * call {@link NextFilter#messageSent(IoSession, Object)} using the original
 * {@link InputStream} written to the session and notifies
 * {@link org.apache.mina.common.WriteFuture} on the
 * original {@link org.apache.mina.common.IoFilter.WriteRequest}.
 * <p/>
 * This filter will ignore written messages which aren't {@link InputStream}
 * instances. Such messages will be passed to the next filter directly.
 * </p>
 * <p/>
 * NOTE: this filter does not close the stream after all data from stream
 * has been written. The {@link org.apache.mina.common.IoHandler} should take
 * care of that in its
 * {@link org.apache.mina.common.IoHandler#messageSent(IoSession, Object)}
 * callback.
 * </p>
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class StreamWriteFilter extends IoFilterAdapter {
    /**
     * The default buffer size this filter uses for writing.
     */
    public static final int DEFAULT_STREAM_BUFFER_SIZE = 4096;

    /**
     * The attribute name used when binding the {@link InputStream} to the session.
     */
    public static final String CURRENT_STREAM = StreamWriteFilter.class
            .getName()
            + ".stream";

    protected static final String WRITE_REQUEST_QUEUE = StreamWriteFilter.class
            .getName()
            + ".queue";

    protected static final String INITIAL_WRITE_FUTURE = StreamWriteFilter.class
            .getName()
            + ".future";

    private int writeBufferSize = DEFAULT_STREAM_BUFFER_SIZE;

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        // If we're already processing a stream we need to queue the WriteRequest.
        if (session.getAttribute(CURRENT_STREAM) != null) {
            Queue<WriteRequest> queue = getWriteRequestQueue(session);
            if (queue == null) {
                queue = new ConcurrentLinkedQueue<WriteRequest>();
                session.setAttribute(WRITE_REQUEST_QUEUE, queue);
            }
            queue.add(writeRequest);
            return;
        }

        Object message = writeRequest.getMessage();

        if (message instanceof InputStream) {

            InputStream inputStream = (InputStream) message;

            ByteBuffer byteBuffer = getNextByteBuffer(inputStream);
            if (byteBuffer == null) {
                // End of stream reached.
                writeRequest.getFuture().setWritten(true);
                nextFilter.messageSent(session, message);
            } else {
                session.setAttribute(CURRENT_STREAM, inputStream);
                session.setAttribute(INITIAL_WRITE_FUTURE, writeRequest
                        .getFuture());

                nextFilter.filterWrite(session, new WriteRequest(byteBuffer));
            }

        } else {
            nextFilter.filterWrite(session, writeRequest);
        }
    }

    @SuppressWarnings("unchecked")
    private Queue<WriteRequest> getWriteRequestQueue(IoSession session) {
        return (Queue<WriteRequest>) session.getAttribute(WRITE_REQUEST_QUEUE);
    }

    @Override
    public void messageSent(NextFilter nextFilter, IoSession session,
            Object message) throws Exception {
        InputStream inputStream = (InputStream) session
                .getAttribute(CURRENT_STREAM);

        if (inputStream == null) {
            nextFilter.messageSent(session, message);
        } else {
            ByteBuffer byteBuffer = getNextByteBuffer(inputStream);

            if (byteBuffer == null) {
                // End of stream reached.
                session.removeAttribute(CURRENT_STREAM);
                WriteFuture writeFuture = (WriteFuture) session
                        .removeAttribute(INITIAL_WRITE_FUTURE);

                // Write queued WriteRequests.
                Queue<? extends WriteRequest> queue = (Queue<? extends WriteRequest>) session
                        .removeAttribute(WRITE_REQUEST_QUEUE);
                if (queue != null) {
                    WriteRequest wr = queue.poll();
                    while (wr != null) {
                        filterWrite(nextFilter, session, wr);
                        wr = queue.poll();
                    }
                }

                writeFuture.setWritten(true);
                nextFilter.messageSent(session, inputStream);
            } else {
                nextFilter.filterWrite(session, new WriteRequest(byteBuffer));
            }
        }
    }

    private ByteBuffer getNextByteBuffer(InputStream is) throws IOException {
        byte[] bytes = new byte[writeBufferSize];

        int off = 0;
        int n = 0;
        while (off < bytes.length
                && (n = is.read(bytes, off, bytes.length - off)) != -1) {
            off += n;
        }

        if (n == -1 && off == 0) {
            return null;
        }

        return ByteBuffer.wrap(bytes, 0, off);
    }

    /**
     * Returns the size of the write buffer in bytes. Data will be read from the
     * stream in chunks of this size and then written to the next filter.
     *
     * @return the write buffer size.
     */
    public int getWriteBufferSize() {
        return writeBufferSize;
    }

    /**
     * Sets the size of the write buffer in bytes. Data will be read from the
     * stream in chunks of this size and then written to the next filter.
     *
     * @throws IllegalArgumentException if the specified size is &lt; 1.
     */
    public void setWriteBufferSize(int writeBufferSize) {
        if (writeBufferSize < 1) {
            throw new IllegalArgumentException(
                    "writeBufferSize must be at least 1");
        }
        this.writeBufferSize = writeBufferSize;
    }

}
