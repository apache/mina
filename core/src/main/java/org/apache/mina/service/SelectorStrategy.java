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

import org.apache.mina.api.IoSession;


/**
 * Strategy for balancing server socket and client socket to different selecting/polling threads.
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface SelectorStrategy {

    /**
     * Provide a {@link SelectorProcessor} for a newly accepted {@link IoSession}.
     * @param acceptingProcessor the selector which accepted the {@link IoSession}
     * @return a processor for processing the new session
     */
    SelectorProcessor getSelectorForNewSession(SelectorProcessor acceptingProcessor);
    
    /**
     * Provide a {@link SelectorProcessor} for a {@link IoSession} which need to write data.
     * This processor will be in charge of selecting the socket for write ready events.
     * 
     * @param session the session in need of writing
     * @return the selector processor for handling this session write events
     */
    SelectorProcessor getSelectorForWrite(IoSession session);
    
    /**
     * Provide a {@link SelectorProcessor} for processing a newly bound address.
     * The processor will accept the incoming connections.
     * @return a {@link SelectorProcessor} for processing a newly bound address
     */
    SelectorProcessor getSelectorForBindNewAddress();
    
    /**
     * Unbind an address and remove it from its {@link SelectorProcessor} 
     * @param address the address to be unbound and removed
     * @throws IOException thrown if any problem occurs while unbinding
     */
    void unbind(SocketAddress address) throws IOException;
}
