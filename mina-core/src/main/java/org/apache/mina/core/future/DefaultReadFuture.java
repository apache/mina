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

import java.io.IOException;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.session.IoSession;

/**
 * A default implementation of {@link WriteFuture}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultReadFuture extends DefaultIoFuture implements ReadFuture {
    /** A static object used when the session is closed */
    private static final Object CLOSED = new Object();

    /**
     * Creates a new instance.
     * 
     * @param session The associated session
     */
    public DefaultReadFuture(IoSession session) {
        super(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getMessage() {
        if (isDone()) {
            Object v = getValue();
            
            if (v == CLOSED) {
                return null;
            }

            if (v instanceof RuntimeException) {
                throw (RuntimeException) v;
            }
            
            if (v instanceof Error) {
                throw (Error) v;
            }
            
            if (v instanceof IOException || v instanceof Exception) {
                throw new RuntimeIoException((Exception) v);
            }

            return v;
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRead() {
        if (isDone()) {
            Object v = getValue();
            
            return v != CLOSED && !(v instanceof Throwable);
        }
        
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClosed() {
        if (isDone()) {
            return getValue() == CLOSED;
        }
        
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Throwable getException() {
        if (isDone()) {
            Object v = getValue();
            
            if (v instanceof Throwable) {
                return (Throwable)v;
            }
        }
        
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setClosed() {
        setValue(CLOSED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRead(Object message) {
        if (message == null) {
            throw new IllegalArgumentException("message");
        }
        
        setValue(message);
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
    public ReadFuture await() throws InterruptedException {
        return (ReadFuture) super.await();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadFuture awaitUninterruptibly() {
        return (ReadFuture) super.awaitUninterruptibly();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadFuture addListener(IoFutureListener<?> listener) {
        return (ReadFuture) super.addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadFuture removeListener(IoFutureListener<?> listener) {
        return (ReadFuture) super.removeListener(listener);
    }
}
