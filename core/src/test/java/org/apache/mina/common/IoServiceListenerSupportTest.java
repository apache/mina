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
package org.apache.mina.common;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.easymock.EasyMock;

/**
 * Tests {@link IoServiceListenerSupport}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class IoServiceListenerSupportTest extends TestCase {
    private static final SocketAddress ADDRESS = new InetSocketAddress(8080);

    private final IoService mockService = EasyMock.createMock(IoService.class);

    public void testServiceLifecycle() throws Exception {
        IoServiceListenerSupport support = new IoServiceListenerSupport(
                mockService);

        IoServiceListener listener = EasyMock.createStrictMock(IoServiceListener.class);

        // Test activation
        listener.serviceActivated(mockService);

        EasyMock.replay(listener);

        support.add(listener);
        support.fireServiceActivated();

        EasyMock.verify(listener);

        // Test deactivation & other side effects
        EasyMock.reset(listener);
        listener.serviceDeactivated(mockService);

        EasyMock.replay(listener);
        //// Activate more than once
        support.fireServiceActivated();
        //// Deactivate
        support.fireServiceDeactivated();
        //// Deactivate more than once
        support.fireServiceDeactivated();

        EasyMock.verify(listener);
    }

    public void testSessionLifecycle() throws Exception {
        IoServiceListenerSupport support = new IoServiceListenerSupport(
                mockService);

        DummySession session = new DummySession();
        session.setService(mockService);
        session.setLocalAddress(ADDRESS);

        IoHandler handler = EasyMock.createStrictMock( IoHandler.class );
        session.setHandler(handler);

        IoServiceListener listener = EasyMock.createStrictMock(IoServiceListener.class);

        // Test creation
        listener.sessionCreated(session);
        handler.sessionCreated(session);
        handler.sessionOpened(session);

        EasyMock.replay(listener);
        EasyMock.replay(handler);

        support.add(listener);
        support.fireSessionCreated(session);

        EasyMock.verify(listener);
        EasyMock.verify(handler);

        Assert.assertEquals(1, support.getManagedSessions().size());
        Assert.assertSame(session, support.getManagedSessions().get(session.getId()));

        // Test destruction & other side effects
        EasyMock.reset(listener);
        EasyMock.reset(handler);
        handler.sessionClosed(session);
        listener.sessionDestroyed(session);

        EasyMock.replay(listener);
        //// Activate more than once
        support.fireSessionCreated(session);
        //// Deactivate
        support.fireSessionDestroyed(session);
        //// Deactivate more than once
        support.fireSessionDestroyed(session);

        EasyMock.verify(listener);

        Assert.assertTrue(session.isClosing());
        Assert.assertEquals(0, support.getManagedSessions().size());
        Assert.assertNull(support.getManagedSessions().get(session.getId()));
    }

    public void testDisconnectOnUnbind() throws Exception {
        IoAcceptor acceptor = EasyMock.createStrictMock(IoAcceptor.class);

        final IoServiceListenerSupport support = new IoServiceListenerSupport(
                acceptor);

        final DummySession session = new DummySession();
        session.setService(acceptor);
        session.setLocalAddress(ADDRESS);

        IoHandler handler = EasyMock.createStrictMock(IoHandler.class);
        session.setHandler(handler);

        final IoServiceListener listener = EasyMock.createStrictMock(IoServiceListener.class);

        // Activate a service and create a session.
        listener.serviceActivated(acceptor);
        listener.sessionCreated(session);
        handler.sessionCreated(session);
        handler.sessionOpened(session);

        EasyMock.replay(listener);
        EasyMock.replay(handler);

        support.add(listener);
        support.fireServiceActivated();
        support.fireSessionCreated(session);

        EasyMock.verify(listener);
        EasyMock.verify(handler);

        // Deactivate a service and make sure the session is closed & destroyed.
        EasyMock.reset(listener);
        EasyMock.reset(handler);

        listener.serviceDeactivated(acceptor);
        EasyMock.expect(acceptor.isCloseOnDeactivation()).andReturn(true);
        listener.sessionDestroyed(session);
        handler.sessionClosed(session);

        EasyMock.replay(listener);
        EasyMock.replay(acceptor);
        EasyMock.replay(handler);

        new Thread() {
            // Emulate I/O service
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // This synchronization block is a workaround for
                // the visibility problem of simultaneous EasyMock
                // state update. (not sure if it fixes the failing test yet.)
                synchronized (listener) {
                    support.fireSessionDestroyed(session);
                }
            }
        }.start();
        support.fireServiceDeactivated();

        synchronized (listener) {
            EasyMock.verify(listener);
        }
        EasyMock.verify(acceptor);
        EasyMock.verify(handler);

        Assert.assertTrue(session.isClosing());
        Assert.assertEquals(0, support.getManagedSessions().size());
        Assert.assertNull(support.getManagedSessions().get(session.getId()));
    }

    public void testConnectorActivation() throws Exception {
        IoConnector connector = EasyMock.createStrictMock(IoConnector.class);

        IoServiceListenerSupport support = new IoServiceListenerSupport(
                connector);

        final DummySession session = new DummySession();
        session.setService(connector);
        session.setRemoteAddress(ADDRESS);

        IoHandler handler = EasyMock.createStrictMock(IoHandler.class);
        session.setHandler(handler);

        IoServiceListener listener = EasyMock.createStrictMock(IoServiceListener.class);

        // Creating a session should activate a service automatically.
        listener.serviceActivated(connector);
        listener.sessionCreated(session);
        handler.sessionCreated(session);
        handler.sessionOpened(session);

        EasyMock.replay(listener);
        EasyMock.replay(handler);

        support.add(listener);
        support.fireSessionCreated(session);

        EasyMock.verify(listener);
        EasyMock.verify(handler);

        // Destroying a session should deactivate a service automatically.
        EasyMock.reset(listener);
        EasyMock.reset(handler);
        listener.sessionDestroyed(session);
        handler.sessionClosed(session);
        listener.serviceDeactivated(connector);

        EasyMock.replay(listener);
        EasyMock.replay(handler);

        support.fireSessionDestroyed(session);

        EasyMock.verify(listener);
        EasyMock.verify(handler);

        Assert.assertEquals(0, support.getManagedSessions().size());
        Assert.assertNull(support.getManagedSessions().get(session.getId()));
    }
}
