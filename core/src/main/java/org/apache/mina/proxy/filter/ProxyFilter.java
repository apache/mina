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
 * ProxyFilter.java - Proxy {@link IoFilter}. Automatically inserted into the {@link IoFilter} chain by {@link ProxyConnector}.
 * Sends the initial handshake message to the proxy and handles any response
 * to the handshake. Once the handshake has completed and the proxied connection has been
 * established this filter becomes transparent to data flowing through the connection.
 * <p>
 * Based upon SSLFilter from mina-filter-ssl.
 * 
 * @author Edouard De Oliveira <a href="mailto:doe_wanted@yahoo.fr">doe_wanted@yahoo.fr</a>
 * @author James Furness <a href="mailto:james.furness@lehman.com">james.furness@lehman.com</a> 
 * @version $Id: $
 */
public class ProxyFilter extends IoFilterAdapter {
    private final static Logger logger = LoggerFactory
            .getLogger(ProxyFilter.class);

    /**
     * Create a new {@link ProxyFilter}.
     */
    public ProxyFilter() {
    }

    /**
     * Called before the filter is added into the filter chain, creates the {@link ProxyLogicHandler} instance
     * which will handle this session.
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
     * Called when the filter is removed from the filter chain - cleans up the {@link ProxyIoSession} instance.
     */
    @Override
    public void onPreRemove(final IoFilterChain chain, final String name,
            final NextFilter nextFilter) {
        IoSession session = chain.getSession();
        session.removeAttribute(ProxyIoSession.PROXY_SESSION);
    }

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
     */
    private ProxyLogicHandler getProxyHandler(final IoSession session) {
        ProxyLogicHandler handler = ((ProxyIoSession) session
                .getAttribute(ProxyIoSession.PROXY_SESSION)).getHandler();

        if (handler == null) {
            throw new IllegalStateException();
        }

        if (handler.getProxyIoSession().getProxyFilter() != this) {
            throw new IllegalArgumentException("Not managed by this filter.");
        }

        return handler;
    }

    /**
     * Receives data from the remote host, passes to the handler if a handshake is in progress, otherwise
     * passes on transparently.
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
                logger.debug(" Data Read: {} ({})", handler, buf);

                // Keep sending handshake data to the handler until we run out
                // of data or the handshake is finished
                while (buf.hasRemaining() && !handler.isHandshakeComplete()) {
                    logger.debug(" Pre-handshake - passing to handler");

                    int pos = buf.position();
                    handler.messageReceived(nextFilter, buf);

                    // Data not consumed or session closing
                    if (buf.position() == pos || session.isClosing()) {
                        return;
                    }
                }

                // Pass on any remaining data to the next filter
                if (buf.hasRemaining()) {
                    logger.debug(" Passing remaining data to next filter");

                    nextFilter.messageReceived(session, buf);
                }
            }
        }
    }

    /**
     * Filters outgoing writes, queueing them up if necessary whilst a handshake is ongoing.
     */
    @Override
    public void filterWrite(final NextFilter nextFilter,
            final IoSession session, final WriteRequest writeRequest) {
        writeData(nextFilter, session, writeRequest, false);
    }

    /**
     * Actually write data. Queues the data up unless it relates to the handshake or the handshake is done.
     */
    public void writeData(final NextFilter nextFilter, final IoSession session,
            final WriteRequest writeRequest, final boolean isHandshakeData) {
        ProxyLogicHandler handler = getProxyHandler(session);

        synchronized (handler) {
            if (handler.isHandshakeComplete()) {
                // Handshake is done - write data as normal
                nextFilter.filterWrite(session, writeRequest);
            } else if (isHandshakeData) {
                IoBuffer buf = (IoBuffer) writeRequest.getMessage();

                // Writing handshake data - write
                logger.debug("   handshake data: {}", buf);

                nextFilter.filterWrite(session, writeRequest);
            } else {
                // Writing non-handshake data before the handshake finished
                if (!session.isConnected()) {
                    // Not even connected - ignore
                    logger
                            .debug(" Write request on closed session. Request ignored.");
                } else {
                    // Queue the data to be sent as soon as the handshake completes
                    logger
                            .debug(" Handshaking is not complete yet. Buffering write request.");

                    handler.enqueueWriteRequest(nextFilter, writeRequest);
                }
            }
        }
    }

    /**
     * Filter handshake-related messages from reaching the messageSent callbacks of downstream filters.
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

    @Override
    public void sessionCreated(NextFilter nextFilter, IoSession session)
            throws Exception {
        logger.debug("Session created: " + session);
        ProxyIoSession proxyIoSession = (ProxyIoSession) session
                .getAttribute(ProxyIoSession.PROXY_SESSION);
        logger.debug("  get proxyIoSession: " + proxyIoSession);
        proxyIoSession.setProxyFilter(this);

        // Create a HTTP proxy handler and start handshake.		
        ProxyLogicHandler handler = proxyIoSession.getHandler();

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

    @Override
    public void sessionOpened(NextFilter nextFilter, IoSession session)
            throws Exception {
        ProxyIoSession proxyIoSession = (ProxyIoSession) session
                .getAttribute(ProxyIoSession.PROXY_SESSION);
        proxyIoSession.getEventQueue().enqueueEventIfNecessary(
                new IoSessionEvent(nextFilter, session,
                        IoSessionEventType.OPENED));
    }

    @Override
    public void sessionIdle(NextFilter nextFilter, IoSession session,
            IdleStatus status) throws Exception {
        ProxyIoSession proxyIoSession = (ProxyIoSession) session
                .getAttribute(ProxyIoSession.PROXY_SESSION);
        IoSessionEvent evt = new IoSessionEvent(nextFilter, session,
                IoSessionEventType.IDLE);
        evt.setStatus(status);
        proxyIoSession.getEventQueue().enqueueEventIfNecessary(evt);
    }

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