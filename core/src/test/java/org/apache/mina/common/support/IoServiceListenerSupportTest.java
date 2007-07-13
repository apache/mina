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
package org.apache.mina.common.support;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoAcceptorConfig;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoServiceListener;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.TransportType;
import org.easymock.MockControl;

/**
 * Tests {@link IoServiceListenerSupport}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IoServiceListenerSupportTest extends TestCase {
    private static final SocketAddress ADDRESS = new InetSocketAddress(8080);

    public void testServiceLifecycle() throws Exception {
        IoServiceListenerSupport support = new IoServiceListenerSupport();

        MockControl listenerControl = MockControl
                .createStrictControl(IoServiceListener.class);
        IoServiceListener listener = (IoServiceListener) listenerControl
                .getMock();

        // Test activation
        listener.serviceActivated(null, ADDRESS, null, null);

        listenerControl.replay();

        support.add(listener);
        support.fireServiceActivated(null, ADDRESS, null, null);

        listenerControl.verify();

        Assert.assertEquals(1, support.getManagedServiceAddresses().size());
        Assert.assertTrue(support.getManagedServiceAddresses()
                .contains(ADDRESS));

        // Test deactivation & other side effects
        listenerControl.reset();
        listener.serviceDeactivated(null, ADDRESS, null, null);

        listenerControl.replay();
        //// Activate more than once
        support.fireServiceActivated(null, ADDRESS, null, null);
        //// Deactivate
        support.fireServiceDeactivated(null, ADDRESS, null, null);
        //// Deactivate more than once
        support.fireServiceDeactivated(null, ADDRESS, null, null);

        listenerControl.verify();

        Assert.assertEquals(0, support.getManagedServiceAddresses().size());
        Assert.assertFalse(support.getManagedServiceAddresses().contains(
                ADDRESS));
    }

    public void testSessionLifecycle() throws Exception {
        IoServiceListenerSupport support = new IoServiceListenerSupport();

        TestSession session = new TestSession(ADDRESS);

        MockControl chainControl = MockControl
                .createStrictControl(IoFilterChain.class);
        IoFilterChain chain = (IoFilterChain) chainControl.getMock();
        session.setFilterChain(chain);

        MockControl listenerControl = MockControl
                .createStrictControl(IoServiceListener.class);
        IoServiceListener listener = (IoServiceListener) listenerControl
                .getMock();

        // Test creation
        listener.sessionCreated(session);
        chain.fireSessionCreated(session);
        chain.fireSessionOpened(session);

        listenerControl.replay();
        chainControl.replay();

        support.add(listener);
        support.fireSessionCreated(session);

        listenerControl.verify();
        chainControl.verify();

        Assert.assertEquals(1, support.getManagedSessions(ADDRESS).size());
        Assert
                .assertTrue(support.getManagedSessions(ADDRESS).contains(
                        session));

        // Test destruction & other side effects
        listenerControl.reset();
        chainControl.reset();
        chain.fireSessionClosed(session);
        listener.sessionDestroyed(session);

        listenerControl.replay();
        //// Activate more than once
        support.fireSessionCreated(session);
        //// Deactivate
        support.fireSessionDestroyed(session);
        //// Deactivate more than once
        support.fireSessionDestroyed(session);

        listenerControl.verify();

        Assert.assertFalse(session.isClosing());
        Assert.assertEquals(0, support.getManagedSessions(ADDRESS).size());
        Assert.assertFalse(support.getManagedSessions(ADDRESS)
                .contains(session));
    }

    public void testDisconnectOnUnbind() throws Exception {
        final IoServiceListenerSupport support = new IoServiceListenerSupport();

        MockControl acceptorControl = MockControl
                .createStrictControl(IoAcceptor.class);
        IoAcceptor acceptor = (IoAcceptor) acceptorControl.getMock();

        final TestSession session = new TestSession(acceptor, ADDRESS);

        MockControl configControl = MockControl
                .createStrictControl(IoAcceptorConfig.class);
        IoAcceptorConfig config = (IoAcceptorConfig) configControl.getMock();

        MockControl chainControl = MockControl
                .createStrictControl(IoFilterChain.class);
        IoFilterChain chain = (IoFilterChain) chainControl.getMock();
        session.setFilterChain(chain);

        MockControl listenerControl = MockControl
                .createStrictControl(IoServiceListener.class);
        IoServiceListener listener = (IoServiceListener) listenerControl
                .getMock();

        // Activate a service and create a session.
        listener.serviceActivated(acceptor, ADDRESS, null, config);
        listener.sessionCreated(session);
        chain.fireSessionCreated(session);
        chain.fireSessionOpened(session);

        listenerControl.replay();
        chainControl.replay();

        support.add(listener);
        support.fireServiceActivated(acceptor, ADDRESS, null, config);
        support.fireSessionCreated(session);

        listenerControl.verify();
        chainControl.verify();

        // Deactivate a service and make sure the session is closed & destroyed.
        listenerControl.reset();
        chainControl.reset();

        listener.serviceDeactivated(acceptor, ADDRESS, null, config);
        configControl.expectAndReturn(config.isDisconnectOnUnbind(), true);
        listener.sessionDestroyed(session);
        chain.fireSessionClosed(session);

        listenerControl.replay();
        configControl.replay();
        chainControl.replay();

        new Thread() {
            // Emulate I/O service
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace();
                }
                support.fireSessionDestroyed(session);
            }
        }.start();

        support.fireServiceDeactivated(acceptor, ADDRESS, null, config);

        Thread.sleep(1000);

        listenerControl.verify();
        configControl.verify();
        chainControl.verify();

        Assert.assertTrue(session.isClosing());
        Assert.assertEquals(0, support.getManagedSessions(ADDRESS).size());
        Assert.assertFalse(support.getManagedSessions(ADDRESS)
                .contains(session));
    }

    public void testConnectorActivation() throws Exception {
        IoServiceListenerSupport support = new IoServiceListenerSupport();

        MockControl connectorControl = MockControl
                .createStrictControl(IoConnector.class);
        IoConnector connector = (IoConnector) connectorControl.getMock();

        final TestSession session = new TestSession(connector, ADDRESS);

        MockControl chainControl = MockControl
                .createStrictControl(IoFilterChain.class);
        IoFilterChain chain = (IoFilterChain) chainControl.getMock();
        session.setFilterChain(chain);

        MockControl listenerControl = MockControl
                .createStrictControl(IoServiceListener.class);
        IoServiceListener listener = (IoServiceListener) listenerControl
                .getMock();

        // Creating a session should activate a service automatically.
        listener.serviceActivated(connector, ADDRESS, null, null);
        listener.sessionCreated(session);
        chain.fireSessionCreated(session);
        chain.fireSessionOpened(session);

        listenerControl.replay();
        chainControl.replay();

        support.add(listener);
        support.fireSessionCreated(session);

        listenerControl.verify();
        chainControl.verify();

        // Destroying a session should deactivate a service automatically.
        listenerControl.reset();
        chainControl.reset();
        listener.sessionDestroyed(session);
        chain.fireSessionClosed(session);
        listener.serviceDeactivated(connector, ADDRESS, null, null);

        listenerControl.replay();
        chainControl.replay();

        support.fireSessionDestroyed(session);

        listenerControl.verify();
        chainControl.verify();

        Assert.assertEquals(0, support.getManagedSessions(ADDRESS).size());
        Assert.assertFalse(support.getManagedSessions(ADDRESS)
                .contains(session));
    }

    private static class TestSession extends BaseIoSession {
        private final IoService service;

        private final SocketAddress serviceAddress;

        private IoFilterChain filterChain;

        TestSession(SocketAddress serviceAddress) {
            this(null, serviceAddress);
        }

        TestSession(IoService service, SocketAddress serviceAddress) {
            this.service = service;
            this.serviceAddress = serviceAddress;
        }

        @Override
        protected void updateTrafficMask() {
        }

        public IoSessionConfig getConfig() {
            return null;
        }

        public IoFilterChain getFilterChain() {
            return filterChain;
        }

        public void setFilterChain(IoFilterChain filterChain) {
            this.filterChain = filterChain;
        }

        public IoHandler getHandler() {
            return null;
        }

        public SocketAddress getLocalAddress() {
            return null;
        }

        public SocketAddress getRemoteAddress() {
            return null;
        }

        public int getScheduledWriteBytes() {
            return 0;
        }

        public int getScheduledWriteRequests() {
            return 0;
        }

        public IoService getService() {
            return service;
        }

        public SocketAddress getServiceAddress() {
            return serviceAddress;
        }

        public IoServiceConfig getServiceConfig() {
            return null;
        }

        public TransportType getTransportType() {
            return null;
        }
    }
}
