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
package org.apache.mina.filter.stream;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;

/**
 * Filter implementation which makes it possible to write Stream
 * objects directly using {@link IoSession#write(Object)}.
 * 
 * @param <T> The type of Stream
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractStreamWriteFilter<T> extends IoFilterAdapter {
    /**
     * The default buffer size this filter uses for writing.
     */
    public static final int DEFAULT_STREAM_BUFFER_SIZE = 4096;

    /**
     * The attribute name used when binding the streaming object to the session.
     */
    protected static final AttributeKey CURRENT_STREAM = new AttributeKey(AbstractStreamWriteFilter.class, "stream");

    protected static final AttributeKey WRITE_REQUEST_QUEUE = new AttributeKey(AbstractStreamWriteFilter.class, "queue");

    protected static final AttributeKey CURRENT_WRITE_REQUEST = new AttributeKey(AbstractStreamWriteFilter.class, "writeRequest");

    private int writeBufferSize = DEFAULT_STREAM_BUFFER_SIZE;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPreAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
        Class<? extends IoFilterAdapter> clazz = getClass();
        
        if (parent.contains(clazz)) {
            throw new IllegalStateException("Only one " + clazz.getName() + " is permitted.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        // If we're already processing a stream we need to queue the WriteRequest.
        if (session.getAttribute(CURRENT_STREAM) != null) {
            Queue<WriteRequest> queue = getWriteRequestQueue(session);
            
            if (queue == null) {
                queue = new ConcurrentLinkedQueue<>();
                session.setAttribute(WRITE_REQUEST_QUEUE, queue);
            }
            
            queue.add(writeRequest);
            
            return;
        }

        Object message = writeRequest.getMessage();

        if (getMessageClass().isInstance(message)) {

            T stream = getMessageClass().cast(message);

            IoBuffer buffer = getNextBuffer(stream);
            
            if (buffer == null) {
                // End of stream reached.
                writeRequest.getFuture().setWritten();
                nextFilter.messageSent(session, writeRequest);
            } else {
                session.setAttribute(CURRENT_STREAM, message);
                session.setAttribute(CURRENT_WRITE_REQUEST, writeRequest);

                nextFilter.filterWrite(session, new DefaultWriteRequest(buffer));
            }

        } else {
            nextFilter.filterWrite(session, writeRequest);
        }
    }

    protected abstract Class<T> getMessageClass();

    @SuppressWarnings("unchecked")
    private Queue<WriteRequest> getWriteRequestQueue(IoSession session) {
        return (Queue<WriteRequest>) session.getAttribute(WRITE_REQUEST_QUEUE);
    }

    @SuppressWarnings("unchecked")
    private Queue<WriteRequest> removeWriteRequestQueue(IoSession session) {
        return (Queue<WriteRequest>) session.removeAttribute(WRITE_REQUEST_QUEUE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageSent(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        T stream = getMessageClass().cast(session.getAttribute(CURRENT_STREAM));

        if (stream == null) {
            nextFilter.messageSent(session, writeRequest);
        } else {
            IoBuffer buffer = getNextBuffer(stream);

            if (buffer == null) {
                // End of stream reached.
                session.removeAttribute(CURRENT_STREAM);
                WriteRequest currentWriteRequest = (WriteRequest) session.removeAttribute(CURRENT_WRITE_REQUEST);

                // Write queued WriteRequests.
                Queue<WriteRequest> queue = removeWriteRequestQueue(session);
                
                if (queue != null) {
                    WriteRequest wr = queue.poll();
                    
                    while (wr != null) {
                        filterWrite(nextFilter, session, wr);
                        wr = queue.poll();
                    }
                }

                currentWriteRequest.getFuture().setWritten();
                nextFilter.messageSent(session, currentWriteRequest);
            } else {
                nextFilter.filterWrite(session, new DefaultWriteRequest(buffer));
            }
        }
    }

    /**
     * @return the size of the write buffer in bytes. Data will be read from the
     * stream in chunks of this size and then written to the next filter.
     */
    public int getWriteBufferSize() {
        return writeBufferSize;
    }

    /**
     * Sets the size of the write buffer in bytes. Data will be read from the
     * stream in chunks of this size and then written to the next filter.
     *
     * @param writeBufferSize The size of the write buffer
     * @throws IllegalArgumentException if the specified size is &lt; 1.
     */
    public void setWriteBufferSize(int writeBufferSize) {
        if (writeBufferSize < 1) {
            throw new IllegalArgumentException("writeBufferSize must be at least 1");
        }
        
        this.writeBufferSize = writeBufferSize;
    }

    protected abstract IoBuffer getNextBuffer(T message) throws IOException;
}
