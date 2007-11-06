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
package org.apache.mina.example.tapedeck;

import java.net.InetSocketAddress;

import org.apache.mina.common.IoHandler;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineEncoder;
import org.apache.mina.statemachine.StateMachine;
import org.apache.mina.statemachine.StateMachineFactory;
import org.apache.mina.statemachine.StateMachineProxyFactory;
import org.apache.mina.statemachine.annotation.IoHandlerTransition;
import org.apache.mina.statemachine.context.IoSessionStateContextLookup;
import org.apache.mina.statemachine.context.StateContext;
import org.apache.mina.statemachine.context.StateContextFactory;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 * Simple example demonstrating how to build a state machine for MINA's 
 * {@link IoHandler} interface.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class Main {
    /** Choose your favorite port number. */
    private static final int PORT = 12345;
    
    private static IoHandler createIoHandler() {
        StateMachine sm = StateMachineFactory.getInstance(IoHandlerTransition.class)
                                .create(TapeDeckServer.EMPTY, new TapeDeckServer());
        
        return (IoHandler) StateMachineProxyFactory.create(IoHandler.class, sm, 
                new IoSessionStateContextLookup(new StateContextFactory() {
                    public StateContext create() {
                        return new TapeDeckServer.TapeDeckContext();
                    }
                }));
    }
    
    public static void main(String[] args) throws Exception {
        SocketAcceptor acceptor = new NioSocketAcceptor();
        acceptor.setReuseAddress(true);
        ProtocolCodecFilter pcf = new ProtocolCodecFilter(
                new TextLineEncoder(), new CommandDecoder());
        acceptor.getFilterChain().addLast("codec", pcf);
        acceptor.setHandler(createIoHandler());
        acceptor.setLocalAddress(new InetSocketAddress(PORT));
        acceptor.bind();
    }
}
