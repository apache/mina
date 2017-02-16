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
package org.apache.mina.core.write;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;

/**
 * The default implementation of {@link WriteRequest}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultWriteRequest implements WriteRequest {
    /** An empty message */
    public static final byte[] EMPTY_MESSAGE = new byte[] {};

    /** An empty FUTURE */
    private static final WriteFuture UNUSED_FUTURE = new WriteFuture() {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isWritten() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setWritten() {
            // Do nothing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public IoSession getSession() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void join() {
            // Do nothing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean join(long timeoutInMillis) {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isDone() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public WriteFuture addListener(IoFutureListener<?> listener) {
            throw new IllegalStateException("You can't add a listener to a dummy future.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public WriteFuture removeListener(IoFutureListener<?> listener) {
            throw new IllegalStateException("You can't add a listener to a dummy future.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public WriteFuture await() throws InterruptedException {
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean await(long timeoutMillis) throws InterruptedException {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public WriteFuture awaitUninterruptibly() {
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean awaitUninterruptibly(long timeoutMillis) {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Throwable getException() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setException(Throwable cause) {
            // Do nothing
        }
    };

    private final Object message;

    private final WriteFuture future;

    private final SocketAddress destination;

    /**
     * Creates a new instance without {@link WriteFuture}.  You'll get
     * an instance of {@link WriteFuture} even if you called this constructor
     * because {@link #getFuture()} will return a bogus future.
     * 
     * @param message The message that will be written
     */
    public DefaultWriteRequest(Object message) {
        this(message, null, null);
    }

    /**
     * Creates a new instance with {@link WriteFuture}.
     * 
     * @param message The message that will be written
     * @param future The associated {@link WriteFuture}
     */
    public DefaultWriteRequest(Object message, WriteFuture future) {
        this(message, future, null);
    }

    /**
     * Creates a new instance.
     *
     * @param message a message to write
     * @param future a future that needs to be notified when an operation is finished
     * @param destination the destination of the message.  This property will be
     *                    ignored unless the transport supports it.
     */
    public DefaultWriteRequest(Object message, WriteFuture future, SocketAddress destination) {
        if (message == null) {
            throw new IllegalArgumentException("message");
        }

        if (future == null) {
            future = UNUSED_FUTURE;
        }

        this.message = message;
        this.future = future;
        this.destination = destination;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteFuture getFuture() {
        return future;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getMessage() {
        return message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteRequest getOriginalRequest() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketAddress getDestination() {
        return destination;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("WriteRequest: ");

        // Special case for the CLOSE_REQUEST writeRequest : it just
        // carries a native Object instance
        if (message.getClass().getName().equals(Object.class.getName())) {
            sb.append("CLOSE_REQUEST");
        } else {
            if (getDestination() == null) {
                sb.append(message);
            } else {
                sb.append(message);
                sb.append(" => ");
                sb.append(getDestination());
            }
        }

        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEncoded() {
        return false;
    }
}