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

import java.io.IOException;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

/**
 * A session handler without an {@link IoSession} parameter for simplicity.
 * <p>
 * A {@link SingleSessionIoHandler} is similar to an {@link IoHandler} with
 * the notable difference that a {@link SingleSessionIoHandler} is used only
 * by one session at a time. Thus, there is no {@link IoSession} parameter in
 * the methods of this interface.
 * </p>
 * <p>
 * Because events are passed to the session in order, it is possible to store
 * conversational state as instance variables in this object.
 * </p>
 *
 * WARNING: This class is badly named as the actual {@link IoHandler} implementor
 * is in fact the {@link SingleSessionIoHandlerDelegate}.
 * 
 * @deprecated This class is not to be used anymore
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
@Deprecated
public interface SingleSessionIoHandler {

    /**
     * Invoked when the session is created. Initialize default socket parameters
     * and user-defined attributes here.
     *
     * @throws Exception If the session can't be created
     * @see IoHandler#sessionCreated(IoSession)
     */
    void sessionCreated() throws Exception;

    /**
     * Invoked when the connection is opened. This method is not invoked if the
     * transport type is UDP.
     *
     * @throws Exception If the session can't be opened
     * @see IoHandler#sessionOpened(IoSession)
     */
    void sessionOpened() throws Exception;

    /**
     * Invoked when the connection is closed. This method is not invoked if the
     * transport type is UDP.
     *
     * @throws Exception If the session can't be closed
     * @see IoHandler#sessionClosed(IoSession)
     */
    void sessionClosed() throws Exception;

    /**
     * Invoked when the connection is idle. Refer to {@link IdleStatus}. This
     * method is not invoked if the transport type is UDP.
     *
     * @param status the type of idleness
     * @throws Exception If the idle event can't be handled
     * @see IoHandler#sessionIdle(IoSession, IdleStatus)
     */
    void sessionIdle(IdleStatus status) throws Exception;

    /**
     * Invoked when any exception is thrown by user {@link IoHandler}
     * implementation or by MINA. If <code>cause</code> is instanceof
     * {@link IOException}, MINA will close the connection automatically.
     *
     * @param cause the caught exception
     * @throws Exception If the exception can't be handled
     * @see IoHandler#exceptionCaught(IoSession, Throwable)
     */
    void exceptionCaught(Throwable cause) throws Exception;

    /**
     * Invoked when a half-duplex connection is closed
     * 
     * @param session The current session
     */
    void inputClosed(IoSession session);

    /**
     * Invoked when protocol message is received. Implement your protocol flow
     * here.
     *
     * @param message the received message
     * @throws Exception If the received message can't be processed
     * @see IoHandler#messageReceived(IoSession, Object)
     */
    void messageReceived(Object message) throws Exception;

    /**
     * Invoked when protocol message that user requested by
     * {@link IoSession#write(Object)} is sent out actually.
     *
     * @param message the sent message
     * @throws Exception If the sent message can't be processed
     * @see IoHandler#messageSent(IoSession, Object)
     */
    void messageSent(Object message) throws Exception;

}
