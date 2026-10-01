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
package org.apache.mina.transport;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * A test IoHandler that just return what it receives to teh client
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class EchoProtocolHandler extends IoHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(EchoProtocolHandler.class);

    /**
     * Default constructor
     */
    public EchoProtocolHandler() {
        super();
    }

    @Override
    public void sessionCreated(IoSession session) {
        if (session.getConfig() instanceof SocketSessionConfig) {
            ((SocketSessionConfig) session.getConfig()).setReceiveBufferSize(2048);
        }

        session.getConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {
        LOG.info("*** IDLE #" + session.getIdleCount(IdleStatus.BOTH_IDLE) + " ***");
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) {
        //cause.printStackTrace();
        session.closeNow();
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        if (!(message instanceof IoBuffer)) {
            return;
        }

        IoBuffer rb = (IoBuffer) message;
        // Write the received data back to remote peer
        IoBuffer wb = rb.duplicate();
        session.write(wb);
    }
}
