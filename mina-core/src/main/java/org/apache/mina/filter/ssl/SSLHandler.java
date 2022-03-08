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
package org.apache.mina.filter.ssl;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRejectedException;
import org.apache.mina.core.write.WriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default interface for SSL exposed to the {@link SSLFilter}
 * 
 * @author Jonathan Valliere
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class SSLHandler {

    /**
     * Minimum size of encoder buffer in packets
     */
    static protected final int MIN_ENCODER_BUFFER_PACKETS = 2;

    /**
     * Maximum size of encoder buffer in packets
     */
    static protected final int MAX_ENCODER_BUFFER_PACKETS = 8;

    /**
     * Zero length buffer used to prime the ssl engine
     */
    static protected final IoBuffer ZERO = IoBuffer.allocate(0, true);

    /**
     * Static logger
     */
    static protected final Logger LOGGER = LoggerFactory.getLogger(SSLHandler.class);

    /**
     * Write Requests which are enqueued prior to the completion of the handshaking
     */
    protected final Deque<WriteRequest> mEncodeQueue = new ConcurrentLinkedDeque<>();

    /**
     * Requests which have been sent to the socket and waiting acknowledgment
     */
    protected final Deque<WriteRequest> mAckQueue = new ConcurrentLinkedDeque<>();

    /**
     * SSL Engine
     */
    protected final SSLEngine mEngine;

    /**
     * Task executor
     */
    protected final Executor mExecutor;

    /**
     * Socket session
     */
    protected final IoSession mSession;

    /**
     * Progressive decoder buffer
     */
    protected IoBuffer mDecodeBuffer;

    /**
     * Instantiates a new handler
     * 
     * @param sslEngine The SSLEngine instance
     * @param executor The executor instance to use to process tasks
     * @param session The session to handle
     */
    public SSLHandler(SSLEngine sslEngine, Executor executor, IoSession session) {
        this.mEngine = sslEngine;
        this.mExecutor = executor;
        this.mSession = session;
    }

    /**
     * @return {@code true} if the encryption session is open
     */
    abstract public boolean isOpen();

    /**
     * @return {@code true} if the encryption session is connected and secure
     */
    abstract public boolean isConnected();

    /**
     * Opens the encryption session, this may include sending the initial handshake
     * message
     * 
     * @param next The next filter in the chain
     * 
     * @throws SSLException If we get an SSL exception while processing the opening
     */
    abstract public void open(NextFilter next) throws SSLException;

    /**
     * Decodes encrypted messages and passes the results to the {@code next} filter.
     * 
     * @param next The next filter in the chain
     * @param message The message to process
     * 
     * @throws SSLException If we get an SSL exception while processing the message
     */
    abstract public void receive(NextFilter next, IoBuffer message) throws SSLException;

    /**
     * Acknowledge that a {@link WriteRequest} has been successfully written to the
     * {@link IoSession}
     * <p>
     * This functionality is used to enforce flow control by allowing only a
     * specific number of pending write operations at any moment of time. When one
     * {@code WriteRequest} is acknowledged, another can be encoded and written.
     * 
     * @param next The next filter in the chain
     * @param request The written request
     * 
     * @throws SSLException If we get an SSL exception while processing the ack
     */
    abstract public void ack(NextFilter next, WriteRequest request) throws SSLException;

    /**
     * Encrypts and writes the specified {@link WriteRequest} to the
     * {@link IoSession} or enqueues it to be processed later.
     * <p>
     * The encryption session may be currently handshaking preventing application
     * messages from being written.
     * 
     * @param next The next filter in the chain
     * @param request The message to write
     * 
     * @throws SSLException If we get an SSL exception while writing the message
     * @throws WriteRejectedException when the session is closing
     */
    abstract public void write(NextFilter next, WriteRequest request) throws SSLException, WriteRejectedException;

    /**
     * Closes the encryption session and writes any required messages
     * 
     * @param next The next filter in the chain
     * @param linger if true, write any queued messages before closing
     * 
     * @throws SSLException If we get an SSL exception while processing the opening session closure
     **/
    abstract public void close(NextFilter next, boolean linger) throws SSLException;

    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuilder b = new StringBuilder();

        b.append(this.getClass().getSimpleName());
        b.append("@");
        b.append(Integer.toHexString(this.hashCode()));
        b.append("[mode=");

        if (mEngine.getUseClientMode()) {
            b.append("client");
        } else {
            b.append("server");
        }

        b.append(", connected=");
        b.append(isConnected());

        b.append("]");

        return b.toString();
    }

    /**
     * Combines the received data with any previously received data
     * 
     * @param source received data
     * @return buffer to decode
     */
    protected IoBuffer resume_decode_buffer(IoBuffer source) {
        if (mDecodeBuffer == null) {
            if (source == null) {
                return ZERO;
            } else {
                mDecodeBuffer = source;
                
                return source;
            }
        } else {
            if (source != null && source != ZERO) {
                mDecodeBuffer.expand(source.remaining());
                mDecodeBuffer.put(source);
                source.free();
            }
            
            mDecodeBuffer.flip();
            
            return mDecodeBuffer;
        }
    }

    /**
     * Stores data for later use if any is remaining
     * 
     * @param source the buffer previously returned by
     *               {@link #resume_decode_buffer(IoBuffer)}
     */
    protected void suspend_decode_buffer(IoBuffer source) {
        if (source.hasRemaining()) {
            if (source.isDerived()) {
                mDecodeBuffer = IoBuffer.allocate(source.remaining());
                mDecodeBuffer.put(source);
            } else {
                source.compact();
                mDecodeBuffer = source;
            }
        } else {
            if (source != ZERO) {
                source.free();
            }
            
            mDecodeBuffer = null;
        }
    }

    /**
     * Allocates the default encoder buffer for the given source size
     * 
     * @param source
     * @return buffer
     */
    protected IoBuffer allocate_encode_buffer(int estimate) {
        SSLSession session = mEngine.getHandshakeSession();
        
        if (session == null) {
            session = mEngine.getSession();
        }
        
        int packets = Math.max(MIN_ENCODER_BUFFER_PACKETS,
                Math.min(MAX_ENCODER_BUFFER_PACKETS, 1 + (estimate / session.getApplicationBufferSize())));
        
        return IoBuffer.allocate(packets * session.getPacketBufferSize());
    }

    /**
     * Allocates the default decoder buffer for the given source size
     * 
     * @param source
     * @return buffer
     */
    protected IoBuffer allocate_app_buffer(int estimate) {
        SSLSession session = mEngine.getHandshakeSession();
        
        if (session == null) { 
            session = mEngine.getSession();
        }
        
        int packets = 1 + (estimate / session.getPacketBufferSize());
        
        return IoBuffer.allocate(packets * session.getApplicationBufferSize());
    }
}
