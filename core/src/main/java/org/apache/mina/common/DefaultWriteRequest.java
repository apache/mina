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
package org.apache.mina.common;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * The default implementation of {@link WriteRequest}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultWriteRequest implements WriteRequest {
    private static final WriteFuture UNUSED_FUTURE = new WriteFuture() {
        public boolean isWritten() {
            return false;
        }

        public void setWritten(boolean written) {
        }

        public IoSession getSession() {
            return null;
        }

        public void join() {
        }

        public boolean join(long timeoutInMillis) {
            return true;
        }

        public boolean isReady() {
            return true;
        }

        public WriteFuture addListener(IoFutureListener listener) {
            throw new IllegalStateException(
                    "You can't add a listener to a dummy future.");
        }

        public WriteFuture removeListener(IoFutureListener listener) {
            throw new IllegalStateException(
                    "You can't add a listener to a dummy future.");
        }

        public WriteFuture await() throws InterruptedException {
            return this;
        }

        public boolean await(long timeout, TimeUnit unit)
                throws InterruptedException {
            return true;
        }

        public boolean await(long timeoutMillis) throws InterruptedException {
            return true;
        }

        public WriteFuture awaitUninterruptibly() {
            return this;
        }

        public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
            return true;
        }

        public boolean awaitUninterruptibly(long timeoutMillis) {
            return true;
        }
    };

    private final Object message;

    private final WriteFuture future;

    private final SocketAddress destination;

    /**
     * Creates a new instance without {@link WriteFuture}.  You'll get
     * an instance of {@link WriteFuture} even if you called this constructor
     * because {@link #getFuture()} will return a bogus future.
     */
    public DefaultWriteRequest(Object message) {
        this(message, null, null);
    }

    /**
     * Creates a new instance with {@link WriteFuture}.
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
    public DefaultWriteRequest(Object message, WriteFuture future,
            SocketAddress destination) {
        if (message == null) {
            throw new NullPointerException("message");
        }

        if (future == null) {
            future = UNUSED_FUTURE;
        }

        this.message = message;
        this.future = future;
        this.destination = destination;
    }

    /**
     * Returns {@link WriteFuture} that is associated with this write request.
     */
    public WriteFuture getFuture() {
        return future;
    }

    /**
     * Returns a message object to be written.
     */
    public Object getMessage() {
        return message;
    }

    /**
     * Returne the destination of this write request.
     *
     * @return <tt>null</tt> for the default destination
     */
    public SocketAddress getDestination() {
        return destination;
    }

    @Override
    public String toString() {
        return message.toString();
    }
}