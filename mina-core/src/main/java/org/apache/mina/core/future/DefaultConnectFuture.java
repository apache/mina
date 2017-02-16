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
package org.apache.mina.core.future;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.session.IoSession;

/**
 * A default implementation of {@link ConnectFuture}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultConnectFuture extends DefaultIoFuture implements ConnectFuture {
    /** A static object stored into the ConnectFuture when teh connection has been cancelled */
    private static final Object CANCELED = new Object();

    /**
     * Creates a new instance.
     */
    public DefaultConnectFuture() {
        super(null);
    }

    /**
     * Creates a new instance of a Connection Failure, with the associated cause.
     * 
     * @param exception The exception that caused the failure
     * @return a new {@link ConnectFuture} which is already marked as 'failed to connect'.
     */
    public static ConnectFuture newFailedFuture(Throwable exception) {
        DefaultConnectFuture failedFuture = new DefaultConnectFuture();
        failedFuture.setException(exception);
        
        return failedFuture;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoSession getSession() {
        Object v = getValue();
        
        if (v instanceof IoSession) {
            return (IoSession) v;
        } else if (v instanceof RuntimeException) {
            throw (RuntimeException) v;
        } else if (v instanceof Error) {
            throw (Error) v;
        } else if (v instanceof Throwable) {
            throw (RuntimeIoException) new RuntimeIoException("Failed to get the session.").initCause((Throwable) v);
        } else  {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Throwable getException() {
        Object v = getValue();
        
        if (v instanceof Throwable) {
            return (Throwable) v;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConnected() {
        return getValue() instanceof IoSession;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCanceled() {
        return getValue() == CANCELED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSession(IoSession session) {
        if (session == null) {
            throw new IllegalArgumentException("session");
        }
        
        setValue(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setException(Throwable exception) {
        if (exception == null) {
            throw new IllegalArgumentException("exception");
        }
        
        setValue(exception);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean cancel() {
        return setValue(CANCELED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectFuture await() throws InterruptedException {
        return (ConnectFuture) super.await();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectFuture awaitUninterruptibly() {
        return (ConnectFuture) super.awaitUninterruptibly();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectFuture addListener(IoFutureListener<?> listener) {
        return (ConnectFuture) super.addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectFuture removeListener(IoFutureListener<?> listener) {
        return (ConnectFuture) super.removeListener(listener);
    }
}
