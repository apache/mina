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

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.proxy.filter.ProxyFilter;
import org.apache.mina.proxy.filter.ProxyHandshakeIoBuffer;
import org.apache.mina.proxy.session.ProxyIoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractProxyLogicHandler.java - Helper class to handle proxy handshaking logic. Derived classes 
 * implement proxy type specific logic.
 * <p>
 * Based upon SSLHandler from mina-filter-ssl.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * @since MINA 2.0.0-M3
 */
public abstract class AbstractProxyLogicHandler implements ProxyLogicHandler {

    private final static Logger logger = LoggerFactory
            .getLogger(AbstractProxyLogicHandler.class);

    /**
     * Object that contains all the proxy authentication session informations.
     */
    private ProxyIoSession proxyIoSession;

    /**
     * Queue of write events which occurred before the proxy handshake had completed.
     */
    private Queue<Event> writeRequestQueue = null;

    /**
     * Has the handshake been completed.
     */
    private boolean handshakeComplete = false;

    /**
     * Creates a new {@link AbstractProxyLogicHandler}.
     * 
     * @param proxyIoSession	 {@link ProxyIoSession} in use.
     */
    public AbstractProxyLogicHandler(ProxyIoSession proxyIoSession) {
        this.proxyIoSession = proxyIoSession;
    }

    /**
     * Returns the proxyFilter {@link ProxyFilter}.
     */
    protected ProxyFilter getProxyFilter() {
        return proxyIoSession.getProxyFilter();
    }

    /**
     * Returns the session.
     */
    protected IoSession getSession() {
        return proxyIoSession.getSession();
    }

    /**
     * Returns the {@link ProxyIoSession} object.
     */
    public ProxyIoSession getProxyIoSession() {
        return proxyIoSession;
    }

    public void setProxySession(ProxyIoSession proxyIoSession) {
        this.proxyIoSession = proxyIoSession;
    }

    /**
     * Write data to the proxy server.
     * 
     * @param nextFilter	Downstream filter to receive data.
     * @param data			Data buffer to be written.
     */
    protected WriteFuture writeData(int index,
            final IoBuffer data) throws UnsupportedEncodingException {
        // write net data
        ProxyHandshakeIoBuffer writeBuffer = new ProxyHandshakeIoBuffer(data);

        logger.debug("   session write: {}", writeBuffer);

        WriteFuture writeFuture = new DefaultWriteFuture(getSession());
        getProxyFilter().writeData(index+1, getSession(),
                new DefaultWriteRequest(writeBuffer, writeFuture), true);

        return writeFuture;
    }

    /**
     * Returns <code>true</code> if handshaking is complete and
     * data can be sent through the proxy.
     */
    public boolean isHandshakeComplete() {
        synchronized (this) {
            return handshakeComplete;
        }
    }

    /**
     * Signals that the shake has finished.
     */
    protected final void setHandshakeComplete() {
        synchronized (this) {
            handshakeComplete = true;
        }

        ProxyIoSession proxyIoSession = getProxyIoSession();
        proxyIoSession.getConnector()
                .fireConnected(proxyIoSession.getSession())
                .awaitUninterruptibly();

        logger.debug("  handshake completed");

        // Connected OK
        try {
            proxyIoSession.getEventQueue().flushPendingSessionEvents();
            flushPendingWriteRequests();
        } catch (Exception ex) {
            logger.error("Unable to flush pending write requests", ex);
        }
    }

    /**
     * Send any write requests which were queued whilst waiting for handshaking to complete.
     */
    protected synchronized void flushPendingWriteRequests() throws Exception {
        logger.debug(" flushPendingWriteRequests()");

        if (writeRequestQueue == null) {
            return;
        }

        Event scheduledWrite;
        while ((scheduledWrite = writeRequestQueue.poll()) != null) {
            logger.debug(" Flushing buffered write request: {}",
                    scheduledWrite.data);

            getProxyFilter().filterWrite(scheduledWrite.nextFilter,
                    getSession(), (WriteRequest) scheduledWrite.data);
        }

        // Free queue
        writeRequestQueue = null;
    }

    /**
     * Enqueue a message to be written once handshaking is complete.
     */
    public synchronized void enqueueWriteRequest(final NextFilter nextFilter,
            final WriteRequest writeRequest) {
        if (writeRequestQueue == null) {
            writeRequestQueue = new LinkedList<Event>();
        }

        writeRequestQueue.offer(new Event(nextFilter, writeRequest));
    }

    /**
     * Close the session.
     */
    protected void closeSession(final String message, final Throwable t) {
        if (t != null) {
            logger.error(message, t);
            proxyIoSession.setAuthenticationFailed(true);
        } else {
            logger.error(message);
        }

        getSession().close();
    }

    protected void closeSession(final String message) {
        closeSession(message, null);
    }

    /**
     * Event wrapper class for enqueued events.
     */
    private final static class Event {
        private final NextFilter nextFilter;

        private final Object data;

        Event(final NextFilter nextFilter, final Object data) {
            this.nextFilter = nextFilter;
            this.data = data;
        }

        public Object getData() {
            return data;
        }

        public NextFilter getNextFilter() {
            return nextFilter;
        }
    }
}