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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoHandler;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSession;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link HandlerCaller}
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class HandlerCallerTest {
    private HandlerCaller caller = new HandlerCaller();

    private IoSession session;

    private IoService service;

    private IoHandler handler;

    @Before
    public void setup() {
        session = mock(IoSession.class);
        service = mock(IoService.class);
        handler = mock(IoHandler.class);
        when(session.getService()).thenReturn(service);
        when(service.getIoHandler()).thenReturn(handler);
    }

    @Test
    public void call_open() {
        // prepare
        OpenEvent event = mock(OpenEvent.class);
        when(event.getSession()).thenReturn(session);

        // run
        caller.visit(event);

        // verify
        verify(event).getSession();
        verify(session).getService();
        verify(service).getIoHandler();
        verify(handler).sessionOpened(session);

        verifyNoMoreInteractions(session, event, handler);
    }

    @Test
    public void call_close() {
        // prepare
        CloseEvent event = mock(CloseEvent.class);
        when(event.getSession()).thenReturn(session);

        // run
        caller.visit(event);

        // verify
        verify(event).getSession();
        verify(session).getService();
        verify(service).getIoHandler();
        verify(handler).sessionClosed(session);

        verifyNoMoreInteractions(session, event, handler);
    }

    @Test
    public void call_idle() {
        // prepare
        IdleEvent event = mock(IdleEvent.class);
        IdleStatus status = IdleStatus.READ_IDLE;
        when(event.getIdleStatus()).thenReturn(status);
        when(event.getSession()).thenReturn(session);

        // run
        caller.visit(event);

        // verify
        verify(event).getSession();
        verify(event).getIdleStatus();
        verify(session).getService();
        verify(service).getIoHandler();
        verify(handler).sessionIdle(session, status);

        verifyNoMoreInteractions(session, event, handler);
    }

    @Test
    public void call_receive() {
        // prepare
        ReceiveEvent event = mock(ReceiveEvent.class);
        Object msg = mock(Object.class);
        when(event.getMessage()).thenReturn(msg);
        when(event.getSession()).thenReturn(session);

        // run
        caller.visit(event);

        // verify
        verify(event).getSession();
        verify(event).getMessage();
        verify(session).getService();
        verify(service).getIoHandler();
        verify(handler).messageReceived(session, msg);

        verifyNoMoreInteractions(session, event, handler);
    }

    @Test
    public void call_msg_sent() {
        // prepare
        SentEvent event = mock(SentEvent.class);
        Object msg = mock(Object.class);
        when(event.getMessage()).thenReturn(msg);
        when(event.getSession()).thenReturn(session);

        // run
        caller.visit(event);

        // verify
        verify(event).getSession();
        verify(event).getMessage();
        verify(session).getService();
        verify(service).getIoHandler();
        verify(handler).messageSent(session, msg);

        verifyNoMoreInteractions(session, event, handler);
    }

}
