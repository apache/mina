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
package org.apache.mina.proxy.event;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.mina.proxy.handlers.socks.SocksProxyRequest;
import org.apache.mina.proxy.session.ProxyIoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IoSessionEventQueue.java - Queue that contains filtered session events while handshake isn't done.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * @since MINA 2.0.0-M3
 */
public class IoSessionEventQueue {
    private final static Logger logger = LoggerFactory
            .getLogger(IoSessionEventQueue.class);

    private ProxyIoSession proxyIoSession;

    /**
     * Queue of session events which occurred before the proxy handshake had completed.
     */
    private Queue<IoSessionEvent> sessionEventsQueue = null;

    public IoSessionEventQueue(ProxyIoSession proxyIoSession) {
        this.proxyIoSession = proxyIoSession;
    }

    private void freeSessionQueue() {
        logger.debug("Event queue CLEARED");

        // Free queue
        sessionEventsQueue = null;
    }

    /**
     * Event is enqueued only if necessary.
     */
    public synchronized void enqueueEventIfNecessary(final IoSessionEvent evt) {
        logger.debug("??? >> Enqueue {}", evt);

        if (proxyIoSession.getRequest() instanceof SocksProxyRequest) {
            // No reconnection used
            evt.deliverEvent();
            return;
        }

        if (proxyIoSession.getHandler().isHandshakeComplete()) {
            evt.deliverEvent();
        } else {
            if (evt.getType() == IoSessionEventType.CLOSED) {
                if (proxyIoSession.isAuthenticationFailed()) {
                    proxyIoSession.getConnector().cancelConnectFuture();
                    freeSessionQueue();
                    evt.deliverEvent();
                } else {
                    freeSessionQueue();
                }
            } else if (evt.getType() == IoSessionEventType.OPENED) {
                // Enqueue event cause it will not reach IoHandler but deliver it to enable session creation.
                enqueueSessionEvent(evt);
                evt.deliverEvent();
            } else {
                enqueueSessionEvent(evt);
            }
        }
    }

    /**
     * Send any session event which were queued whilst waiting for handshaking to complete.
     * 
     * Please note this is an internal method. DO NOT USE it in your code.
     */
    public synchronized void flushPendingSessionEvents() throws Exception {
        IoSessionEvent evt;

        logger.debug(" flushPendingSessionEvents()");

        if (sessionEventsQueue == null) {
            return;
        }

        while ((evt = sessionEventsQueue.poll()) != null) {
            logger.debug(" Flushing buffered event: {}", evt);

            evt.deliverEvent();
        }

        // Free queue
        sessionEventsQueue = null;
    }

    /**
     * Enqueue an event to be delivered once handshaking is complete.
     */
    private void enqueueSessionEvent(final IoSessionEvent evt) {
        if (sessionEventsQueue == null) {
            sessionEventsQueue = new LinkedList<IoSessionEvent>();
        }

        logger.debug("Enqueuing event: {}", evt);

        sessionEventsQueue.offer(evt);
    }
}