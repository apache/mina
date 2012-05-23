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

import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSession;
import org.apache.mina.session.AbstractIoSession;
import org.apache.mina.transport.tcp.AbstractTcpServer;
import org.apache.mina.transport.udp.AbstractUdpServer;

/**
 * A processor in charge of a group of client session and server sockets.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 */
public interface SelectorProcessor<TCP_SERVER extends AbstractTcpServer,UDP_SERVER extends AbstractUdpServer> {

	/**
	 * The strategy to use for assigning selectors to newly created sessions 
	 * @param strategy
	 */
	void setStrategy(SelectorStrategy<?> strategy);
	
    /**
     * create a session for a freshly accepted client socket
     * @param service
     * @param clientChannel
     */
    void createSession(IoService service, Object clientSocket) throws IOException;

    /**
     * Bind and start processing this newly bound TCP server
     * @param server the server to be processed
     */
    void addServer(TCP_SERVER server);

    /**
     * Start processing this newly bound UDP
     * @param server the server to be processed
     */
    void addServer(UDP_SERVER server);

    /**
     * Stop processing this TCP server
     * @param server the server to be removed of processing
     */
    void removeServer(TCP_SERVER server);
    
    /**
     * Stop processing this UDP server
     * @param server the server to be processed
     */
    void removeServer(UDP_SERVER server);
    
    /**
     * Schedule a session for flushing, will be called after a {@link IoSession#write(Object)}.
     * @param session the session to flush
     */
    void flush(AbstractIoSession session);

}
