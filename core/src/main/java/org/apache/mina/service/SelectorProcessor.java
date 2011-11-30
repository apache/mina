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

package org.apache.mina.service;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.mina.api.IoServer;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSession;

/**
 * A processor in charge of a group of client session and server sockets.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 */
public interface SelectorProcessor {

    /**
     * create a session for a freshly accepted client socket
     * @param service
     * @param clientChannel
     */
    void createSession(IoService service, Object clientSocket);

    /**
     * Bind and start processing this new server address
     * @param address local address to bind
     * @throws IOException exception thrown if any problem occurs while binding
     */
    void bindAndAcceptAddress(IoServer server, SocketAddress address) throws IOException;

    /**
     * Stop processing and unbind this server address
     * @param address the local server address to unbind
     * @throws IOException exception thrown if any problem occurs while unbinding
     */
    void unbind(SocketAddress address) throws IOException;

    /**
     * Schedule a session for flushing, will be called after a {@link IoSession#write(Object)}.
     * @param session the session to flush
     */
    void flush(IoSession session);

}
