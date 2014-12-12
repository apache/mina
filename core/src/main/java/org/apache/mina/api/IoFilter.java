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

package org.apache.mina.api;

import org.apache.mina.filterchain.ReadFilterChainController;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.apache.mina.session.WriteRequest;

/**
 * Filter are interceptors/processors for incoming data received/sent.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoFilter {

    /**
     * Invoked when a connection has been opened.
     * 
     * @param session {@link IoSession} associated with the invocation
     */
    void sessionOpened(IoSession session);

    /**
     * Invoked when a connection is closed.
     * 
     * @param session {@link IoSession} associated with the invocation
     */
    void sessionClosed(IoSession session);

    /**
     * Invoked with the related {@link IdleStatus} when a connection becomes idle.
     * 
     * @param session {@link IoSession} associated with the invocation
     */
    void sessionIdle(IoSession session, IdleStatus status);

    /**
     * Invoked when a message is received.
     * 
     * @param session {@link IoSession} associated with the invocation
     * @param message the incoming message to process
     */
    void messageReceived(IoSession session, Object message, ReadFilterChainController controller);

    /**
     * Invoked when a message is under writing. The filter is supposed to apply the needed transformation.
     * 
     * @param session {@link IoSession} associated with the invocation
     * @param message the message to process before writing
     */
    void messageWriting(IoSession session, WriteRequest message, WriteFilterChainController controller);

    /**
     * Invoked when a high level message was written to the low level O/S buffer.
     * 
     * @param session {@link IoSession} associated with the invocation
     * @param message the incoming message to process
     */
    void messageSent(IoSession session, Object message);
    
    /**
     * Invoked when a secure handshake has been started.
     * 
     * @param session {@link IoSession} associated with the invocation
     */
    void handshakeStarted(IoSession session);
    
    /**
     * Invoked when a secure handshake has been completed.
     * 
     * @param session {@link IoSession} associated with the invocation
     */
    void handshakeCompleted(IoSession session);

    /**
     * Invoked when a secure context has been closed.
     * 
     * @param session {@link IoSession} associated with the invocation
     */
    void secureClosed(IoSession session);

}