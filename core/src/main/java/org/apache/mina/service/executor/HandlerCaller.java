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

package org.apache.mina.service.executor;

import org.apache.mina.api.IoHandler;
import org.apache.mina.api.IoSession;

/**
 * In charge of calling the {@link IoHandler} for a given {@link Event}
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
class HandlerCaller implements EventVisitor {

    @Override
    public void visit(CloseEvent event) {
        IoSession session = event.getSession();
        try {
            session.getService().getIoHandler().sessionClosed(session);
        } catch (Exception e) {
            session.getService().getIoHandler().exceptionCaught(session, e);
        }
    }

    @Override
    public void visit(IdleEvent event) {
        IoSession session = event.getSession();
        try {
            session.getService().getIoHandler().sessionIdle(session, event.getIdleStatus());
        } catch (Exception e) {
            session.getService().getIoHandler().exceptionCaught(session, e);
        }

    }

    @Override
    public void visit(OpenEvent event) {
        IoSession session = event.getSession();
        try {
            session.getService().getIoHandler().sessionOpened(session);
        } catch (Exception e) {
            session.getService().getIoHandler().exceptionCaught(session, e);
        }

    }

    @Override
    public void visit(ReceiveEvent event) {
        IoSession session = event.getSession();
        try {
            session.getService().getIoHandler().messageReceived(session, event.getMessage());
        } catch (Exception e) {
            session.getService().getIoHandler().exceptionCaught(session, e);
        }
    }

    @Override
    public void visit(SentEvent event) {
        IoSession session = event.getSession();
        try {
            session.getService().getIoHandler().messageSent(session, event.getMessage());
        } catch (Exception e) {
            session.getService().getIoHandler().exceptionCaught(session, e);
        }
    }

    @Override
    public void visit(HandshakeStartedEvent event) {
        IoSession session = event.getSession();
        try {
            session.getService().getIoHandler().handshakeStarted(session);
        } catch (Exception e) {
            session.getService().getIoHandler().exceptionCaught(session, e);
        }
    }

    @Override
    public void visit(HandshakeCompletedEvent event) {
        IoSession session = event.getSession();
        try {
            session.getService().getIoHandler().handshakeCompleted(session);
        } catch (Exception e) {
            session.getService().getIoHandler().exceptionCaught(session, e);
        }
    }

    @Override
    public void visit(SecureClosedEvent event) {
        IoSession session = event.getSession();
        try {
            session.getService().getIoHandler().secureClosed(session);
        } catch (Exception e) {
            session.getService().getIoHandler().exceptionCaught(session, e);
        }
    }
}