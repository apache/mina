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

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionLogger;
import org.apache.mina.common.WriteException;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.SocketSession;
import org.apache.mina.transport.socket.SocketSessionConfig;

/**
 * {@link IoHandler} implementation for echo server.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$,
 */
public class EchoProtocolHandler extends IoHandlerAdapter {
    @Override
    public void sessionCreated(IoSession session) {
        if (session instanceof SocketSession) {
            SocketSessionConfig config = ((SocketSession) session).getConfig();
            config.setReceiveBufferSize(2048);
        }

        session.getConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);

        // We're going to use SSL negotiation notification.
        session.setAttribute(SslFilter.USE_NOTIFICATION);
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {
        IoSessionLogger.getLogger(session).info(
                "*** IDLE #" + session.getIdleCount(IdleStatus.BOTH_IDLE) + " ***");
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) {
        IoSessionLogger.getLogger(session).warn(cause);
        if (cause instanceof WriteException) {
            WriteException e = (WriteException) cause;
            IoSessionLogger.getLogger(session).warn("Failed write requests: {}", e.getRequests());
        }
        session.close();
    }

    @Override
    public void messageReceived(IoSession session, Object message)
            throws Exception {
        if (!(message instanceof IoBuffer)) {
            return;
        }

        IoBuffer rb = (IoBuffer) message;
        // Write the received data back to remote peer
        IoBuffer wb = IoBuffer.allocate(rb.remaining());
        wb.put(rb);
        wb.flip();
        session.write(wb);
    }
}