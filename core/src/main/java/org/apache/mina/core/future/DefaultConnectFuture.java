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
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;


/**
 * A default implementation of {@link ConnectFuture}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultConnectFuture<H> extends DefaultIoFuture implements ConnectFuture<H> {
    /** The Handle we connect to */
    private H handle;
    
    /** The associated connector */
    private IoConnector connector;
    
    /** The maximum delay for the connection establishment */
    private long deadline;
    
    /** The session initializer callback */
    private IoSessionInitializer<? extends ConnectFuture<H>> sessionInitializer;

    private static final Object CANCELED = new Object();

    /**
     * Returns a new {@link ConnectFuture} which is already marked as 'failed to connect'.
     */
    public static ConnectFuture newFailedFuture(Throwable exception) {
        DefaultConnectFuture failedFuture = new DefaultConnectFuture();
        failedFuture.setException(exception);
        return failedFuture;
    }

    /**
     * Creates a new instance.
     */
    public DefaultConnectFuture() {
        super(null);
    }
    
    @Override
    public IoSession getSession() {
        Object v = getValue();
        
        if (v instanceof RuntimeException) {
            throw (RuntimeException) v;
        } else if (v instanceof Error) {
            throw (Error) v;
        } else if (v instanceof Throwable) {
            throw (RuntimeIoException) new RuntimeIoException(
                    "Failed to get the session.").initCause((Throwable) v);
        } else if (v instanceof IoSession) {
            return (IoSession) v;
        } else {
            return null;
        }
    }

    public Throwable getException() {
        Object v = getValue();
        if (v instanceof Throwable) {
            return (Throwable) v;
        } else {
            return null;
        }
    }

    public boolean isConnected() {
        return getValue() instanceof IoSession;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCanceled() {
        return getValue() == CANCELED;
    }

    public void setSession(IoSession session) {
        if (session == null) {
            throw new NullPointerException("session");
        }
        setValue(session);
    }

    public void setException(Throwable exception) {
        if (exception == null) {
            throw new NullPointerException("exception");
        }
        setValue(exception);
    }

    public void cancel() {
        if ( !isDone() ) {
            setValue(CANCELED);
        }
    }
    
    @Override
    public ConnectFuture await() throws InterruptedException {
        return (ConnectFuture) super.await();
    }

    @Override
    public ConnectFuture awaitUninterruptibly() {
        return (ConnectFuture) super.awaitUninterruptibly();
    }

    @Override
    public ConnectFuture addListener(IoFutureListener<?> listener) {
        return (ConnectFuture) super.addListener(listener);
    }

    @Override
    public ConnectFuture removeListener(IoFutureListener<?> listener) {
        return (ConnectFuture) super.removeListener(listener);
    }

    public IoConnector getConnector() {
        return connector;
    }

    public H getHandle() {
        return handle;
    }

    public long getDeadline() {
        return deadline;
    }

    public IoSessionInitializer<? extends ConnectFuture<H>> getSessionInitializer() {
        return sessionInitializer;
    }

    /**
     * @param handler the handle to set
     */
    public void setHandle( H handle ) {
        this.handle = handle;
    }

    /**
     * @param connector the connector to set
     */
    public void setConnector( IoConnector connector ) {
        this.connector = connector;
    }

    /**
     * @param deadline the deadline to set
     */
    public void setDeadline( long deadline ) {
        this.deadline = deadline;
    }

    /**
     * @param sessionInitializer the sessionInitializer to set
     */
    public void setSessionInitializer( IoSessionInitializer<? extends ConnectFuture<H>> sessionInitializer ){
        this.sessionInitializer = sessionInitializer;
    }
}
