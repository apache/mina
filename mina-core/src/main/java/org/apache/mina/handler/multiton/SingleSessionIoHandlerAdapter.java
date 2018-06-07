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
package org.apache.mina.handler.multiton;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

/**
 * Adapter class for implementors of the {@link SingleSessionIoHandler}
 * interface. The session to which the handler is assigned is accessible
 * through the getSession() method.
 * 
 * @deprecated This class is deprecated
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
@Deprecated
public class SingleSessionIoHandlerAdapter implements SingleSessionIoHandler {

    /**
     * The session to which the handler is assigned.
     */
    private final IoSession session;

    /**
     * Creates a new instance that is assigned to the passed in session.
     *
     * @param session the session to which the handler is assigned
     */
    public SingleSessionIoHandlerAdapter(IoSession session) {
        if (session == null) {
            throw new IllegalArgumentException("session");
        }
        
        this.session = session;
    }

    /**
     * Retrieves the session to which this handler is assigned.
     *
     * @return the session
     */
    protected IoSession getSession() {
        return session;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exceptionCaught(Throwable th) throws Exception {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void inputClosed(IoSession session) {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageReceived(Object message) throws Exception {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageSent(Object message) throws Exception {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionClosed() throws Exception {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionCreated() throws Exception {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionIdle(IdleStatus status) throws Exception {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionOpened() throws Exception {
        // Do nothing
    }
}
