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
package org.apache.mina.proxy;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.FileRegion;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.DefaultConnectFuture;
import org.apache.mina.core.service.AbstractIoConnector;
import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.proxy.filter.ProxyFilter;
import org.apache.mina.proxy.handlers.socks.SocksProxyRequest;
import org.apache.mina.proxy.session.ProxyIoSession;
import org.apache.mina.proxy.session.ProxyIoSessionInitializer;
import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.SocketSessionConfig;

/**
 * ProxyConnector.java - Decorator for {@link SocketConnector} to provide proxy support, 
 * as suggested by MINA list discussions.
 * <p>
 * Operates by intercepting connect requests and replacing the endpoint address with the 
 * proxy address, then adding a {@link ProxyFilter} as the first {@link IoFilter} which 
 * performs any necessary handshaking with the proxy before allowing data to flow 
 * normally. During the handshake, any outgoing write requests are buffered.
 * 
 * @see        http://www.nabble.com/Meta-Transport%3A-an-idea-on-implementing-reconnection-and-proxy-td12969001.html
 * @see        http://issues.apache.org/jira/browse/DIRMINA-415
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class ProxyConnector extends AbstractIoConnector {
    private static final TransportMetadata METADATA = new DefaultTransportMetadata(
            "proxy", "proxyconnector", false, true, InetSocketAddress.class,
            SocketSessionConfig.class, IoBuffer.class, FileRegion.class);

    /**
     * Wrapped connector to use for outgoing TCP connections.
     */
    private SocketConnector connector = null;

    /**
     * Proxy filter instance.
     */
    private final ProxyFilter proxyFilter = new ProxyFilter();

    /**
     * The {@link ProxyIoSession} in use.
     */
    private ProxyIoSession proxyIoSession;

    /**
     * This future will notify it's listeners when really connected to the target
     */
    private DefaultConnectFuture future;

    /**
     * Creates a new proxy connector.
     */
    public ProxyConnector() {
        super(new DefaultSocketSessionConfig(), null);
    }

    /**
     * Creates a new proxy connector.
     * 
     * @param connector Connector used to establish proxy connections.
     */
    public ProxyConnector(final SocketConnector connector) {        
        this(connector, new DefaultSocketSessionConfig(), null);
    }

    /**
     * Creates a new proxy connector. 
     * @see AbstractIoConnector(IoSessionConfig, Executor).
     */
    public ProxyConnector(final SocketConnector connector, IoSessionConfig config, Executor executor) {
        super(config, executor);
        setConnector(connector);
    }    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public IoSessionConfig getSessionConfig() {
        return connector.getSessionConfig();
    }

    /**
     * Returns the {@link ProxyIoSession} linked with this connector.
     */
    public ProxyIoSession getProxyIoSession() {
        return proxyIoSession;
    }

    /**
     * Sets the proxy session object of this connector.
     * @param proxyIoSession the configuration of this connector.
     */
    public void setProxyIoSession(ProxyIoSession proxyIoSession) {
        if (proxyIoSession == null) {
            throw new IllegalArgumentException("proxySession object cannot be null");
        }

        if (proxyIoSession.getProxyAddress() == null) {
            throw new IllegalArgumentException(
                    "proxySession.proxyAddress cannot be null");
        }

        proxyIoSession.setConnector(this);
        setDefaultRemoteAddress(proxyIoSession.getProxyAddress());
        this.proxyIoSession = proxyIoSession;
    }

    /**
     * Connects to the specified <code>address</code>.  If communication starts
     * successfully, events are fired to the connector's <code>handler</code>.
     * 
     * @param remoteAddress the remote address to connect to
     * @param localAddress the local address
     * @param sessionInitializer the session initializer
     * @return {@link ConnectFuture} that will tell the result of the connection attempt
     */
    @SuppressWarnings("unchecked")
    @Override
    protected ConnectFuture connect0(
            final SocketAddress remoteAddress,
            final SocketAddress localAddress,
            final IoSessionInitializer<? extends ConnectFuture> sessionInitializer) {
        if (!proxyIoSession.isReconnectionNeeded()) {
            // First connection
            IoHandler handler = getHandler();
            if (!(handler instanceof AbstractProxyIoHandler)) {
                throw new IllegalArgumentException(
                        "IoHandler must be an instance of AbstractProxyIoHandler");
            }

            connector.setHandler(handler);
            future = new DefaultConnectFuture();
        }

        ConnectFuture conFuture = connector.connect(proxyIoSession
                .getProxyAddress(), new ProxyIoSessionInitializer(
                sessionInitializer, proxyIoSession));

        // If proxy does not use reconnection like socks the connector's 
        // future is returned. If we're in the middle of a reconnection
        // then we send back the connector's future which is only used
        // internally while <code>future</code> will be used to notify
        // the user of the connection state.
        if (proxyIoSession.getRequest() instanceof SocksProxyRequest
                || proxyIoSession.isReconnectionNeeded()) {
            return conFuture;
        }

        return future;
    }

    /**
     * Cancels the real connection when reconnection is in use.
     */
    public void cancelConnectFuture() {
        future.cancel();
    }

    /**
     * Fires the connection event on the real future to notify the client.
     * 
     * @param session the current session
     * @return the future holding the connected session
     */
    protected ConnectFuture fireConnected(final IoSession session) {
        future.setSession(session);
        return future;
    }

    /**
     * Get the {@link SocketConnector} to be used for connections
     * to the proxy server.
     */
    public final SocketConnector getConnector() {
        return connector;
    }

    /**
     * Sets the {@link SocketConnector} to be used for connections
     * to the proxy server.
     * 
     * @param connector the connector to use
     */
    private final void setConnector(final SocketConnector connector) {
        if (connector == null) {
            throw new IllegalArgumentException("connector cannot be null");
        }

        this.connector = connector;
        String className = ProxyFilter.class.getName();

        // Removes an old ProxyFilter instance from the chain
        if (connector.getFilterChain().contains(className)) {
            connector.getFilterChain().remove(className);
        }

        // Insert the ProxyFilter as the first filter in the filter chain builder        
        connector.getFilterChain().addFirst(className, proxyFilter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void dispose0() throws Exception {
        if (connector != null) {
            connector.dispose();
        }
    }

    /**
     * {@inheritDoc}
     */
    public TransportMetadata getTransportMetadata() {
        return METADATA;
    }
}