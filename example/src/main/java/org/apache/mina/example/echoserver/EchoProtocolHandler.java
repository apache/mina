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
package org.apache.mina.example.echoserver;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.TransportType;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IoHandler} implementation for echo server. 
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$,
 */
public class EchoProtocolHandler extends IoHandlerAdapter {
    private static final Logger log = LoggerFactory
            .getLogger(EchoProtocolHandler.class);

    @Override
    public void sessionCreated(IoSession session) {
        if (session.getTransportType() == TransportType.SOCKET) {
            ((SocketSessionConfig) session.getConfig())
                    .setReceiveBufferSize(2048);
        }

        session.setIdleTime(IdleStatus.BOTH_IDLE, 10);

        // We're going to use SSL negotiation notification.
        session.setAttribute(SSLFilter.USE_NOTIFICATION);
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {
        log.info("*** IDLE #" + session.getIdleCount(IdleStatus.BOTH_IDLE)
                + " ***");
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) {
        cause.printStackTrace();
        session.close();
    }

    @Override
    public void messageReceived(IoSession session, Object message)
            throws Exception {
        if (!(message instanceof ByteBuffer)) {
            return;
        }

        ByteBuffer rb = (ByteBuffer) message;
        // Write the received data back to remote peer
        ByteBuffer wb = ByteBuffer.allocate(rb.remaining());
        wb.put(rb);
        wb.flip();
        session.write(wb);
    }
}