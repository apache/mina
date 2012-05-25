/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.mina.transport.nio;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSessionConfig;
import org.apache.mina.api.RuntimeIoException;
import org.apache.mina.service.idlechecker.IdleChecker;
import org.apache.mina.session.AbstractIoSession;
import org.apache.mina.util.AbstractIoFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A UDP session based on NIO
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NioUdpSession extends AbstractIoSession {

    private static final Logger LOG = LoggerFactory.getLogger(NioUdpSession.class);

    private final SocketAddress localAddress;

    private final SocketAddress remoteAddress;

    /** we pre-allocate a close future for lock-less {@link #close(boolean)} */
    private final IoFuture<Void> closeFuture = new AbstractIoFuture<Void>() {

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean cancelOwner(boolean mayInterruptIfRunning) {
            // we don't cancel close
            return false;
        }
    };

    /**
     * @param service
     * @param writeProcessor
     * @param idleChecker
     */
    public NioUdpSession(IoService service, NioSelectorProcessor writeProcessor, IdleChecker idleChecker,
            SocketAddress localAddress, SocketAddress remoteAddress) {
        super(service, writeProcessor, idleChecker);
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoFuture<Void> close(boolean immediately) {
        switch (state) {
        case CREATED:
            LOG.error("Session {} not opened", this);
            throw new RuntimeIoException("cannot close an not opened session");
        case CONNECTED:
        case CLOSING:
            if (immediately) {
                state = SessionState.CLOSED;
            } else {
                // we wait for the write queue to be depleted
                state = SessionState.CLOSING;
            }
            break;
        case CLOSED:
            LOG.warn("Already closed session {}", this);
            break;
        default:
            throw new IllegalStateException("not implemented session state : " + state);
        }
        return closeFuture;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void suspendRead() {
        // TODO
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void suspendWrite() {
        // TODO
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resumeRead() {
        // TODO
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resumeWrite() {
        // TODO
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadSuspended() {
        // TODO
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWriteSuspended() {
        // TODO
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoSessionConfig getConfig() {
        return null;
    }

    /**
     * Called when the session received a datagram.
     * @param readBuffer the received datagram
     */
    void receivedDatagram(ByteBuffer readBuffer) {
        processMessageReceived(readBuffer);
        idleChecker.sessionRead(this, System.currentTimeMillis());
    }
}
