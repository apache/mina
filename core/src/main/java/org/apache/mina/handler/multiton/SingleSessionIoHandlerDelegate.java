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

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

/**
 * An {@link IoHandler} implementation which delegates all requests to
 * {@link SingleSessionIoHandler}s.  A {@link SingleSessionIoHandlerFactory}
 * is used to create a new {@link SingleSessionIoHandler} for each newly
 * created session.
 *
 * WARNING : This {@link IoHandler} implementation may be easier to understand and 
 * thus to use but the user should be aware that creating one handler by session 
 * will lower scalability if building an high performance server. This should only
 * be used with very specific needs in mind.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
@Deprecated
public class SingleSessionIoHandlerDelegate implements IoHandler {
    /**
     * The key used to store the {@link SingleSessionIoHandler} as a session
     * attribute.
     */
    public static final AttributeKey HANDLER = new AttributeKey(SingleSessionIoHandlerDelegate.class, "handler");

    /**
     * The {@link SingleSessionIoHandlerFactory} used to create new
     * {@link SingleSessionIoHandler}s.
     */
    private final SingleSessionIoHandlerFactory factory;

    /**
     * Creates a new instance that uses the passed in
     * {@link SingleSessionIoHandlerFactory} to create new
     * {@link SingleSessionIoHandler}s.
     *
     * @param factory  the factory for {@link SingleSessionIoHandler}s
     */
    public SingleSessionIoHandlerDelegate(SingleSessionIoHandlerFactory factory) {
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }
        this.factory = factory;
    }

    /**
     * Returns the {@link SingleSessionIoHandlerFactory} that is used to create a new
     * {@link SingleSessionIoHandler} instance.
     */
    public SingleSessionIoHandlerFactory getFactory() {
        return factory;
    }

    /**
     * Creates a new instance with the factory passed to the constructor of
     * this class.  The created handler is stored as a session
     * attribute named {@link #HANDLER}.
     *
     * @see org.apache.mina.core.service.IoHandler#sessionCreated(org.apache.mina.core.session.IoSession)
     */
    public void sessionCreated(IoSession session) throws Exception {
        SingleSessionIoHandler handler = factory.getHandler(session);
        session.setAttribute(HANDLER, handler);
        handler.sessionCreated();
    }

    /**
     * Delegates the method call to the
     * {@link SingleSessionIoHandler#sessionOpened()} method of the handler
     * assigned to this session.
     */
    public void sessionOpened(IoSession session) throws Exception {
        SingleSessionIoHandler handler = (SingleSessionIoHandler) session
                .getAttribute(HANDLER);
        handler.sessionOpened();
    }

    /**
     * Delegates the method call to the
     * {@link SingleSessionIoHandler#sessionClosed()} method of the handler
     * assigned to this session.
     */
    public void sessionClosed(IoSession session) throws Exception {
        SingleSessionIoHandler handler = (SingleSessionIoHandler) session
                .getAttribute(HANDLER);
        handler.sessionClosed();
    }

    /**
     * Delegates the method call to the
     * {@link SingleSessionIoHandler#sessionIdle(IdleStatus)} method of the
     * handler assigned to this session.
     */
    public void sessionIdle(IoSession session, IdleStatus status)
            throws Exception {
        SingleSessionIoHandler handler = (SingleSessionIoHandler) session
                .getAttribute(HANDLER);
        handler.sessionIdle(status);
    }

    /**
     * Delegates the method call to the
     * {@link SingleSessionIoHandler#exceptionCaught(Throwable)} method of the
     * handler assigned to this session.
     */
    public void exceptionCaught(IoSession session, Throwable cause)
            throws Exception {
        SingleSessionIoHandler handler = (SingleSessionIoHandler) session
                .getAttribute(HANDLER);
        handler.exceptionCaught(cause);
    }

    /**
     * Delegates the method call to the
     * {@link SingleSessionIoHandler#messageReceived(Object)} method of the
     * handler assigned to this session.
     */
    public void messageReceived(IoSession session, Object message)
            throws Exception {
        SingleSessionIoHandler handler = (SingleSessionIoHandler) session
                .getAttribute(HANDLER);
        handler.messageReceived(message);
    }

    /**
     * Delegates the method call to the
     * {@link SingleSessionIoHandler#messageSent(Object)} method of the handler
     * assigned to this session.
     */
    public void messageSent(IoSession session, Object message) throws Exception {
        SingleSessionIoHandler handler = (SingleSessionIoHandler) session
                .getAttribute(HANDLER);
        handler.messageSent(message);
    }
}
