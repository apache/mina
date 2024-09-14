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
package org.apache.mina.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.IoServiceListener;
import org.apache.mina.core.service.IoServiceListenerSupport;
import org.apache.mina.core.session.DummySession;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

/**
 * Tests {@link IoServiceListenerSupport}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class IoServiceListenerSupportTest {
    private static final SocketAddress ADDRESS = new InetSocketAddress(8080);

    private final IoService mockService = mock(IoService.class);

    @Test
    public void testServiceLifecycle() throws Exception {
        IoServiceListenerSupport support = new IoServiceListenerSupport(mockService);

        IoServiceListener listener = mock(IoServiceListener.class);

        // Test direct activation
        listener.serviceActivated(mockService);

        // Check the serviceActivated method has been called
        verify(listener).serviceActivated(mockService);

        // Reset the mock now.
        reset(listener);

        // Use a IoServiceListener support
        // The listener.serviceActivated() method should be called
        support.add(listener);
        support.fireServiceActivated();

        // Check the serviceActivated method has been called for the listener through the support call
        verify(listener).serviceActivated(mockService);

        // Test deactivation & other side effects
        // First reset the functions calles
        reset(listener);

        listener.serviceDeactivated(mockService);

        // Check the serviceDeactivated method has been called
        verify(listener).serviceDeactivated(mockService);

        // Try to active the service which has been deactivated. Should not be possible 
        support.fireServiceActivated();
        
        // Should do nothing as the service has been deactivated
        verify(listener, never()).serviceActivated(mockService);

        // Deactivate through the support again
        support.fireServiceDeactivated();

        // The listener method should be called a second time
        verify(listener, times(2)).serviceDeactivated(mockService);

        // Deactivate more than once. Should do nothing
        support.fireServiceDeactivated();

        // Check the serviceActivated method has not been called again
        verify(listener, never()).serviceActivated(mockService);

        // The serviceDeactivated method should not have been called again either 
        verify(listener, times(2)).serviceDeactivated(mockService);
    }

    @Test
    public void testSessionLifecycle() throws Exception {
        IoServiceListenerSupport support = new IoServiceListenerSupport(mockService);

        DummySession session = new DummySession();
        session.setService(mockService);
        session.setLocalAddress(ADDRESS);

        IoHandler handler = mock(IoHandler.class);
        session.setHandler(handler);

        IoServiceListener listener = mock(IoServiceListener.class);

        // Inject the listener 
        support.add(listener);
        
        // This call will call the following methods:
        // * handler.sessionCreated()
        // * handler.sessionOpened()
        // * for each listener, listener.sessionCreated(
        support.fireSessionCreated(session);

        verify(handler).sessionCreated(session);
        verify(handler).sessionOpened(session);
        verify(listener).sessionCreated(session);;

        // We now should have 1 managed session
        assertEquals(1, support.getManagedSessions().size());
        assertSame(session, support.getManagedSessions().get(session.getId()));

        // Test destruction & other side effects
        // First reset the method calls
        reset(listener);
        reset(handler);

        // Activate more than once, should do nothing, as the session has already been managed
        support.fireSessionCreated(session);

        assertEquals(1, support.getManagedSessions().size());
        assertSame(session, support.getManagedSessions().get(session.getId()));

        // Deactivate. This should call the following methods:
        // * handler.sessionClosed() 
        // * for each listener, listener.sessionDestroyed(session)
        support.fireSessionDestroyed(session);
        
        verify(handler).sessionClosed(session);
        verify(listener).sessionDestroyed(session);
        assertEquals(0, support.getManagedSessions().size());

        // Deactivate more than once, should do nothing
        // First, reset the function calls 
        reset(listener);
        reset(handler);

        // Destroy again
        support.fireSessionDestroyed(session);

        // Check that the methods aren't called
        verify(handler, never()).sessionClosed(session);
        verify(listener, never()).sessionDestroyed(session);

        assertTrue(session.isClosing());
        assertEquals(0, support.getManagedSessions().size());
        assertNull(support.getManagedSessions().get(session.getId()));
    }

    @Test
    public void testDisconnectOnUnbind() throws Exception {
        IoAcceptor acceptor = mock(IoAcceptor.class);

        final IoServiceListenerSupport support = new IoServiceListenerSupport(acceptor);

        final DummySession session = new DummySession();
        session.setService(acceptor);
        session.setLocalAddress(ADDRESS);

        IoHandler handler = mock(IoHandler.class);
        session.setHandler(handler);

        final IoServiceListener listener = mock(IoServiceListener.class);

        // Activate a service and create a session.
        support.add(listener);
        
        // The listener.serviceActivated method should be called
        support.fireServiceActivated();
        verify(listener).serviceActivated(acceptor);

        // Now create a session. The following methods should be called:
        // * handler.sessionCreated()
        // * handler.sessionOpened()
        // * for each listener, listener.sessionCreated and serviceActivated
        support.fireSessionCreated(session);

        verify(handler).sessionCreated(session);
        verify(handler).sessionOpened(session);
        verify(listener).serviceActivated(acceptor);
        verify(listener).sessionCreated(session);

        // Deactivate a service and make sure the session is closed & destroyed.
        reset(listener);
        reset(handler);

        when(acceptor.isCloseOnDeactivation()).thenReturn(true);
        
        support.fireSessionDestroyed(session);
        support.fireServiceDeactivated();

        verify(listener).sessionDestroyed(session);
        verify(acceptor).isCloseOnDeactivation();
        verify(handler).sessionClosed(session);

        assertTrue(session.isClosing());
        assertEquals(0, support.getManagedSessions().size());
        assertNull(support.getManagedSessions().get(session.getId()));
    }

    @Test
    public void testConnectorActivation() throws Exception {
        IoConnector connector = mock(IoConnector.class);

        IoServiceListenerSupport support = new IoServiceListenerSupport(connector);

        final DummySession session = new DummySession();
        session.setService(connector);
        session.setRemoteAddress(ADDRESS);

        IoHandler handler = mock(IoHandler.class);
        session.setHandler(handler);

        IoServiceListener listener = mock(IoServiceListener.class);

        // Creating a session should activate a service automatically.
        support.add(listener);

        // This call will call the following methods:
        // * handler.sessionCreated()
        // * handler.sessionOpened()
        // * for each listener, listener.sessionCreated(
        support.fireSessionCreated(session);

        verify(handler).sessionCreated(session);
        verify(handler).sessionOpened(session);
        verify(listener).serviceActivated(connector);
        verify(listener).sessionCreated(session);

        assertEquals(1, support.getManagedSessions().size());

        // Destroy the session. The following methods should be called:
        // * handler.sessionClosed() 
        // * for each listener, listener.sessionDestroyed(session)
        support.fireSessionDestroyed(session);

        verify(handler).sessionClosed(session);
        verify(listener).serviceDeactivated(connector);
        verify(listener).sessionDestroyed(session);

        assertEquals(0, support.getManagedSessions().size());
        assertNull(support.getManagedSessions().get(session.getId()));
    }
}
