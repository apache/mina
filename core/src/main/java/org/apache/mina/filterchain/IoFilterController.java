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

package org.apache.mina.filterchain;

import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoSession;

/**
 * An implementation that is responsible for performing IO (network, file or
 * any other kind of IO)
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoFilterController {

    /**
     * Call this method for processing a session created event using this chain.
     * @param session {@link IoSession} the freshly created session
     */
    void processSessionCreated(IoSession session);

    /**
     * Call this method for processing a session open event using this chain.
     * @param session {@link IoSession} the opened session
     */
    void processSessionOpened(IoSession session);

    /**
     * Call this method for processing a session closed event using the chain.
     * @param session {@link IoSession} the closed session
     */
    void processSessionClosed(IoSession session);

    /**
     * Call this method for processing a received message using this chain.
     * This processing is done in reverse order.
     * @param session {@link IoSession} associated with this message
     * @param message the received message
     */
    void processMessageReceived(IoSession session, Object message);

    /**
     * Call this method for processing a message for writing using this chain.
     * Once the message is processed, the resulting ByteBuffers are enqueued in the session write requests queue
     * @param session {@link IoSession} associated with this message
     * @param message the message to write
     * @param writeFuture the write future to iform of write success (optional can be <code>null</code>)
     */
    void processMessageWriting(IoSession session, Object message, IoFuture<Void> writeFuture);
}