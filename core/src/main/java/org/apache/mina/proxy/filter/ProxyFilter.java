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
package org.apache.mina.proxy.filter;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.proxy.ProxyAuthException;
import org.apache.mina.proxy.ProxyConnector;
import org.apache.mina.proxy.ProxyLogicHandler;
import org.apache.mina.proxy.event.IoSessionEvent;
import org.apache.mina.proxy.event.IoSessionEventQueue;
import org.apache.mina.proxy.event.IoSessionEventType;
import org.apache.mina.proxy.handlers.ProxyRequest;
import org.apache.mina.proxy.handlers.http.HttpSmartProxyHandler;
import org.apache.mina.proxy.handlers.socks.Socks4LogicHandler;
import org.apache.mina.proxy.handlers.socks.Socks5LogicHandler;
import org.apache.mina.proxy.handlers.socks.SocksProxyConstants;
import org.apache.mina.proxy.handlers.socks.SocksProxyRequest;
import org.apache.mina.proxy.session.ProxyIoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProxyFilter.java - Proxy {@link IoFilter}. 
 * Automatically inserted into the {@link IoFilter} chain by {@link ProxyConnector}.
 * Sends the initial handshake message to the proxy and handles any response
 * to the handshake. Once the handshake has completed and the proxied connection has been
 * established this filter becomes transparent to data flowing through the connection.
 * <p>
 * Based upon SSLFilter from mina-filter-ssl.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class ProxyFilter extends IoFilterAdapter {
    private final static Logger LOGGER = LoggerFactory
            .getLogger(ProxyFilter.class);

    /**
     * Create a new {@link ProxyFilter}.
     */
    public ProxyFilter() {
        // Do nothing
    }

    /**
     * Called before the filter is added into the filter chain.
     * Checks if chain already holds an {@link ProxyFilter} instance. 
     * 
     * @param chain the filter chain
     * @param name the name assigned to this filter
     * @param nextFilter the next filter
     * @throws IllegalStateException if chain already contains an instance of 
     * {@link ProxyFilter}
     */
    @Override
    public void onPreAdd(final IoFilterChain chain, final String name,
            final NextFilter nextFilter) {
        if (chain.contains(ProxyFilter.class)) {
            throw new IllegalStateException(
                    "A filter chain cannot contain more than one ProxyFilter.");
        }
    }

    /**
     * Called when the filter is removed from the filter chain.
     * Cleans the {@link ProxyIoSession} instance from the session.
     * 
     * @param chain the filter chain
     * @param name the name assigned to this filter
     * @param nextFilter the next filter
     */
    @Override
    public void onPreRemove(final IoFilterChain chain, final String name,
            final NextFilter nextFilter) {
        IoSession session = chain.getSession();
        session.removeAttribute(ProxyIoSession.PROXY_SESSION);
    }

    /**
     * Called when an exception occurs in the chain. A flag is set in the
     * {@link ProxyIoSession} session's instance to signal that handshake
     * failed.  
     * 
     * @param chain the filter chain
     * @param name the name assigned to this filter
     * @param nextFilter the next filter
     */
    @Override
    public void exceptionCaught(NextFilter nextFilter, IoSession session,
            Throwable cause) throws Exception {
        ProxyIoSession proxyIoSession = (ProxyIoSession) session
                .getAttribute(ProxyIoSession.PROXY_SESSION);
        proxyIoSession.setAuthenticationFailed(true);
        super.exceptionCaught(nextFilter, session, cause);
    }

    /**
     * Get the {@link ProxyLogicHandler} for a given session.
     * 
     * @param session the session object
     * @return the handler which will handle handshaking with the proxy
     */
    private ProxyLogicHandler getProxyHandler(final IoSession session) {
        ProxyLogicHandler handler = ((ProxyIoSession) session
                .getAttribute(ProxyIoSession.PROXY_SESSION)).getHandler();

        if (handler == null) {
            throw new IllegalStateException();
        }

        // Sanity check
        if (handler.getProxyIoSession().getProxyFilter() != this) {
            throw new IllegalArgumentException("Not managed by this filter.");
        }

        return handler;
    }

    /**
     * Receives data from the remote host, passes to the handler if a handshake is in progress, 
     * otherwise passes on transparently.
     * 
     * @param nextFilter the next filter in filter chain
     * @param session the session object
     * @param message the object holding the received data
     */
    @Override
    public void messageReceived(final NextFilter nextFilter,
            final IoSession session, final Object message)
            throws ProxyAuthException {
        ProxyLogicHandler handler = getProxyHandler(session);

        synchronized (handler) {
            IoBuffer buf = (IoBuffer) message;

            if (handler.isHandshakeComplete()) {
                // Handshake done - pass data on as-is
                nextFilter.messageReceived(session, buf);

            } else {
                LOGGER.debug(" Data Read: {} ({})", handler, buf);

                // Keep sending handshake data to the handler until we run out
                // of data or the handshake is finished
                while (buf.hasRemaining() && !handler.isHandshakeComplete()) {
                    LOGGER.debug(" Pre-handshake - passing to handler");

                    int pos = buf.position();
                    handler.messageReceived(nextFilter, buf);

                    // Data not consumed or session closing
                    if (buf.position() == pos || session.isClosing()) {
                        return;
                    }
                }

                // Pass on any remaining data to the next filter
                if (buf.hasRemaining()) {
                    LOGGER.debug(" Passing remaining data to next filter");

                    nextFilter.messageReceived(session, buf);
                }
            }
        }
    }

    /**
     * Filters outgoing writes, queueing them up if necessary while a handshake 
     * is ongoing.
     * 
     * @param nextFilter the next filter in filter chain
     * @param session the session object
     * @param writeRequest the data to write
     */
    @Override
    public void filterWrite(final NextFilter nextFilter,
            final IoSession session, final WriteRequest writeRequest) {
        writeData(nextFilter, session, writeRequest, false);
    }

    /**
     * Actually write data. Queues the data up unless it relates to the handshake or the 
     * handshake is done.
     * 
     * @param nextFilter the next filter in filter chain
     * @param session the session object
     * @param writeRequest the data to write
     * @param isHandshakeData true if writeRequest is written by the proxy classes.
     */
    public void writeData(final NextFilter nextFilter, final IoSession session,
            final WriteRequest writeRequest, final boolean isHandshakeData) {
        ProxyLogicHandler handler = getProxyHandler(session);

        synchronized (handler) {
            if (handler.isHandshakeComplete()) {
                // Handshake is done - write data as normal
                nextFilter.filterWrite(session, writeRequest);
            } else if (isHandshakeData) {
                LOGGER.debug("   handshake data: {}", writeRequest.getMessage());
                
                // Writing handshake data
                nextFilter.filterWrite(session, writeRequest);
            } else {
                // Writing non-handshake data before the handshake finished
                if (!session.isConnected()) {
                    // Not even connected - ignore
                    LOGGER.debug(" Write request on closed session. Request ignored.");
                } else {
                    // Queue the data to be sent as soon as the handshake completes
                    LOGGER.debug(" Handshaking is not complete yet. Buffering write request.");
                    handler.enqueueWriteRequest(nextFilter, writeRequest);
                }
            }
        }
    }

    /**
     * Filter handshake related messages from reaching the messageSent callbacks of 
     * downstream filters.
     * 
     * @param nextFilter the next filter in filter chain
     * @param session the session object
     * @param writeRequest the data written
     */
    @Override
    public void messageSent(final NextFilter nextFilter,
            final IoSession session, final WriteRequest writeRequest)
            throws Exception {
        if (writeRequest.getMessage() != null
                && writeRequest.getMessage() instanceof ProxyHandshakeIoBuffer) {
            // Ignore buffers used in handshaking
            return;
        }

        nextFilter.messageSent(session, writeRequest);
    }

    /**
     * Called when the session is created. Will create the handler able to handle
     * the {@link ProxyIoSession#getRequest()} request stored in the session. Event
     * is stored in an {@link IoSessionEventQueue} for later delivery to the next filter
     * in the chain when the handshake would have succeed. This will prevent the rest of 
     * the filter chain from being affected by this filter internals. 
     * 
     * Please note that this event can occur multiple times because of some http 
     * proxies not handling keep-alive connections thus needing multiple sessions 
     * during the handshake.
     * 
     * @param nextFilter the next filter in filter chain
     * @param session the session object
     */
    @Override
    public void sessionCreated(NextFilter nextFilter, IoSession session)
            throws Exception {
        LOGGER.debug("Session created: " + session);
        ProxyIoSession proxyIoSession = (ProxyIoSession) session
                .getAttribute(ProxyIoSession.PROXY_SESSION);
        LOGGER.debug("  get proxyIoSession: " + proxyIoSession);
        proxyIoSession.setProxyFilter(this);

        // Create a HTTP proxy handler and start handshake.
        ProxyLogicHandler handler = proxyIoSession.getHandler();

        // This test prevents from loosing handler conversationnal state when
        // reconnection occurs during an http handshake.
        if (handler == null) {
            ProxyRequest request = proxyIoSession.getRequest();

            if (request instanceof SocksProxyRequest) {
                SocksProxyRequest req = (SocksProxyRequest) request;
                if (req.getProtocolVersion() == SocksProxyConstants.SOCKS_VERSION_4) {
                    handler = new Socks4LogicHandler(proxyIoSession);
                } else {
                    handler = new Socks5LogicHandler(proxyIoSession);
                }
            } else {
                handler = new HttpSmartProxyHandler(proxyIoSession);
            }

            proxyIoSession.setHandler(handler);
            handler.doHandshake(nextFilter);
        }

        proxyIoSession.getEventQueue().enqueueEventIfNecessary(
                new IoSessionEvent(nextFilter, session,
                        IoSessionEventType.CREATED));
    }

    /**
     * Event is stored in an {@link IoSessionEventQueue} for later delivery to the next filter
     * in the chain when the handshake would have succeed. This will prevent the rest of 
     * the filter chain from being affected by this filter internals.
     * 
     * @param nextFilter the next filter in filter chain
     * @param session the session object
     */
    @Override
    public void sessionOpened(NextFilter nextFilter, IoSession session)
            throws Exception {
        ProxyIoSession proxyIoSession = (ProxyIoSession) session
                .getAttribute(ProxyIoSession.PROXY_SESSION);
        proxyIoSession.getEventQueue().enqueueEventIfNecessary(
                new IoSessionEvent(nextFilter, session,
                        IoSessionEventType.OPENED));
    }

    /**
     * Event is stored in an {@link IoSessionEventQueue} for later delivery to the next filter
     * in the chain when the handshake would have succeed. This will prevent the rest of 
     * the filter chain from being affected by this filter internals.
     * 
     * @param nextFilter the next filter in filter chain
     * @param session the session object
     */    
    @Override
    public void sessionIdle(NextFilter nextFilter, IoSession session,
            IdleStatus status) throws Exception {
        ProxyIoSession proxyIoSession = (ProxyIoSession) session
                .getAttribute(ProxyIoSession.PROXY_SESSION);
        proxyIoSession.getEventQueue().enqueueEventIfNecessary(
                new IoSessionEvent(nextFilter, session, status));
    }

    /**
     * Event is stored in an {@link IoSessionEventQueue} for later delivery to the next filter
     * in the chain when the handshake would have succeed. This will prevent the rest of 
     * the filter chain from being affected by this filter internals.
     * 
     * @param nextFilter the next filter in filter chain
     * @param session the session object
     */    
    @Override
    public void sessionClosed(NextFilter nextFilter, IoSession session)
            throws Exception {
        ProxyIoSession proxyIoSession = (ProxyIoSession) session
                .getAttribute(ProxyIoSession.PROXY_SESSION);
        proxyIoSession.getEventQueue().enqueueEventIfNecessary(
                new IoSessionEvent(nextFilter, session,
                        IoSessionEventType.CLOSED));
    }
}