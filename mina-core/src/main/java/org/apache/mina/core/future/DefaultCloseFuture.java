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

import org.apache.mina.core.session.IoSession;

/**
 * A default implementation of {@link CloseFuture}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultCloseFuture extends DefaultIoFuture implements CloseFuture {
    /**
     * Creates a new instance.
     * 
     * @param session The associated session
     */
    public DefaultCloseFuture(IoSession session) {
        super(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClosed() {
        if (isDone()) {
            return ((Boolean) getValue()).booleanValue();
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setClosed() {
        setValue(Boolean.TRUE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloseFuture await() throws InterruptedException {
        return (CloseFuture) super.await();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloseFuture awaitUninterruptibly() {
        return (CloseFuture) super.awaitUninterruptibly();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloseFuture addListener(IoFutureListener<?> listener) {
        return (CloseFuture) super.addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloseFuture removeListener(IoFutureListener<?> listener) {
        return (CloseFuture) super.removeListener(listener);
    }
}
