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
package org.apache.mina.transport.socket.nio;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Executor;

import org.apache.mina.common.AbstractPollingIoConnector;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoProcessor;
import org.apache.mina.common.TransportMetadata;
import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.SocketSessionConfig;

/**
 * {@link IoConnector} for socket transport (TCP/IP).
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 389042 $, $Date: 2006-03-27 07:49:41Z $
 */
public final class NioSocketConnector
        extends AbstractPollingIoConnector<NioSession, SocketChannel>
        implements SocketConnector {

    private volatile Selector selector;

    public NioSocketConnector() {
        super(new DefaultSocketSessionConfig(), NioProcessor.class);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
    }

    public NioSocketConnector(int processorCount) {
        super(new DefaultSocketSessionConfig(), NioProcessor.class, processorCount);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
    }

    public NioSocketConnector(IoProcessor<NioSession> processor) {
        super(new DefaultSocketSessionConfig(), processor);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
    }

    public NioSocketConnector(Executor executor, IoProcessor<NioSession> processor) {
        super(new DefaultSocketSessionConfig(), executor, processor);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
    }

    @Override
    protected void init() throws Exception {
        this.selector = Selector.open();
    }

    @Override
    protected void destroy() throws Exception {
        if (selector != null) {
            selector.close();
        }
    }

    public TransportMetadata getTransportMetadata() {
        return NioSocketSession.METADATA;
    }

    @Override
    public SocketSessionConfig getSessionConfig() {
        return (SocketSessionConfig) super.getSessionConfig();
    }

    @Override
    public InetSocketAddress getDefaultRemoteAddress() {
        return (InetSocketAddress) super.getDefaultRemoteAddress();
    }

    public void setDefaultRemoteAddress(InetSocketAddress defaultRemoteAddress) {
        super.setDefaultRemoteAddress(defaultRemoteAddress);
    }

    @Override
    protected Iterator<SocketChannel> allHandles() {
        return new SocketChannelIterator(selector.keys());
    }

    @Override
    protected boolean connect(SocketChannel handle, SocketAddress remoteAddress)
            throws Exception {
        return handle.connect(remoteAddress);
    }

    @Override
    protected ConnectionRequest connectionRequest(SocketChannel handle) {
        SelectionKey key = handle.keyFor(selector);
        if (key == null) {
            return null;
        }

        return (ConnectionRequest) key.attachment();
    }

    @Override
    protected void close(SocketChannel handle) throws Exception {
        SelectionKey key = handle.keyFor(selector);
        if (key != null) {
            key.cancel();
        }
        handle.close();
    }

    @Override
    protected boolean finishConnect(SocketChannel handle) throws Exception {
        SelectionKey key = handle.keyFor(selector);
        if (handle.finishConnect()) {
            if (key != null) {
                key.cancel();
            }
            return true;
        }

        return false;
    }

    @Override
    protected SocketChannel newHandle(SocketAddress localAddress)
            throws Exception {
        SocketChannel ch = SocketChannel.open();

        int receiveBufferSize =
            (getSessionConfig()).getReceiveBufferSize();
        if (receiveBufferSize > 65535) {
            ch.socket().setReceiveBufferSize(receiveBufferSize);
        }

        if (localAddress != null) {
            ch.socket().bind(localAddress);
        }
        ch.configureBlocking(false);
        return ch;
    }

    @Override
    protected NioSession newSession(IoProcessor<NioSession> processor, SocketChannel handle) {
        return new NioSocketSession(this, processor, handle);
    }

    @Override
    protected void register(SocketChannel handle, ConnectionRequest request)
            throws Exception {
        handle.register(selector, SelectionKey.OP_CONNECT, request);
    }

    @Override
    protected boolean select(int timeout) throws Exception {
        return selector.select(timeout) > 0;
    }

    @Override
    protected Iterator<SocketChannel> selectedHandles() {
        return new SocketChannelIterator(selector.selectedKeys());
    }

    @Override
    protected void wakeup() {
        selector.wakeup();
    }

    private static class SocketChannelIterator implements Iterator<SocketChannel> {

        private final Iterator<SelectionKey> i;

        private SocketChannelIterator(Collection<SelectionKey> selectedKeys) {
            this.i = selectedKeys.iterator();
        }

        public boolean hasNext() {
            return i.hasNext();
        }

        public SocketChannel next() {
            SelectionKey key = i.next();
            return (SocketChannel) key.channel();
        }

        public void remove() {
            i.remove();
        }
    }
}
