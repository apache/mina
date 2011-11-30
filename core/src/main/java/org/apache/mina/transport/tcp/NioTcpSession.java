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
package org.apache.mina.transport.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;

import org.apache.mina.api.IoFuture;
import org.apache.mina.api.RuntimeIoException;
import org.apache.mina.service.SelectorProcessor;
import org.apache.mina.session.AbstractIoSession;
import org.apache.mina.transport.tcp.nio.NioTcpClient;
import org.apache.mina.transport.tcp.nio.NioTcpServer;
import org.apache.mina.util.AbstractIoFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A NIO based TCP session, should be used by {@link NioTcpServer} and {@link NioTcpClient}.
 * A TCP session is a connection between a our server/client and the remote end-point.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 */
public class NioTcpSession extends AbstractIoSession {

    private static final Logger LOG = LoggerFactory.getLogger(NioTcpSession.class);

    /** the NIO socket channel for this TCP session */
    private SocketChannel channel;

    /** the socket configuration */
    private final SocketSessionConfig configuration;

    // this session requested to close
    private volatile boolean closeRequested = false;

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

    NioTcpSession(NioTcpServer service, SocketChannel channel, SelectorProcessor writeProcessor) {
        super(service, writeProcessor);
        this.channel = channel;
        this.configuration = new ProxySocketSessionConfig(channel.socket());
    }

    /**
     * Get the underlying {@link SocketChannel} of this session
     * @return the socket channel used by this session
     */
    SocketChannel getSocketChannel() {
        return channel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InetSocketAddress getRemoteAddress() {
        if (channel == null) {
            return null;
        }
        Socket socket = channel.socket();

        if (socket == null) {
            return null;
        }

        return (InetSocketAddress) socket.getRemoteSocketAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InetSocketAddress getLocalAddress() {
        if (channel == null) {
            return null;
        }

        Socket socket = channel.socket();

        if (socket == null) {
            return null;
        }

        return (InetSocketAddress) socket.getLocalSocketAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConnected() {
        return state == SessionState.CONNECTED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecuring() {
        return state == SessionState.SECURING;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecured() {
        return state == SessionState.CONNECTED_SECURED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClosed() {
        return state == SessionState.CLOSED;
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
            closeRequested = true;
            state = SessionState.CLOSING;
            if (immediately) {
                try {
                    channel.close();
                } catch (IOException e) {
                    throw new RuntimeIoException(e);
                }
            } else {
                // flush this session the flushing code will close the session
                writeProcessor.flush(this);
            }
            break;
        case CLOSING:
            // return the same future
            LOG.warn("Already closing session {}", this);
            break;
        case CLOSED:
            LOG.warn("Already closed session {}", this);
            break;
        default:
            throw new RuntimeIoException("not implemented session state : " + state);
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
    public SocketSessionConfig getConfig() {
        return configuration;
    }

    /**
     * Set this session status as connected. To be called by the processor selecting/polling this session.
     */
    void setConnected() {
        if (getState() != SessionState.CREATED) {
            throw new RuntimeException("Trying to open a non created session");
        }
        state = SessionState.CONNECTED;
    }
}
