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

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.SessionUtil;

/**
 * Adapter class for implementors of the {@link SingleSessionIoHandler}
 * interface. The session to which the handler is assigned is accessible
 * through the {@link #getSession()} method.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
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
            throw new NullPointerException("session");
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

    public void exceptionCaught(Throwable th) throws Exception {
    }

    public void messageReceived(Object message) throws Exception {
    }

    public void messageSent(Object message) throws Exception {
    }

    public void sessionClosed() throws Exception {
    }

    public void sessionCreated() throws Exception {
        SessionUtil.initialize(getSession());
    }

    public void sessionIdle(IdleStatus status) throws Exception {
    }

    public void sessionOpened() throws Exception {
    }
}
