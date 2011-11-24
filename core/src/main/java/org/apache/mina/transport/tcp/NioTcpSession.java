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

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;

import org.apache.mina.api.IoFuture;
import org.apache.mina.service.SelectorProcessor;
import org.apache.mina.session.AbstractIoSession;
import org.apache.mina.transport.tcp.nio.NioTcpServer;

/**
 * 
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 */
public class NioTcpSession extends AbstractIoSession {

    private SocketChannel channel;

    private final SocketSessionConfig configuration;

    NioTcpSession(NioTcpServer service, SocketChannel channel, SelectorProcessor writeProcessor) {
        super(service, writeProcessor);
        this.channel = channel;
        this.configuration = new ProxySocketSessionConfig(channel.socket());
    }

    public SocketChannel getSocketChannel() {
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

    @Override
    public boolean isConnected() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isClosing() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public IoFuture<Void> close(boolean immediately) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void suspendRead() {
        // TODO Auto-generated method stub

    }

    @Override
    public void suspendWrite() {
        // TODO Auto-generated method stub

    }

    @Override
    public void resumeRead() {
        // TODO Auto-generated method stub

    }

    @Override
    public void resumeWrite() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isReadSuspended() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * @inh
     */
    @Override
    public boolean isWriteSuspended() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public SocketSessionConfig getConfig() {
        return configuration;
    }

    void setConnected() {
        if (getState() != SessionState.CREATED) {
            throw new RuntimeException("Trying to open a non created session");
        }
        state = SessionState.CONNECTED;
    }
}