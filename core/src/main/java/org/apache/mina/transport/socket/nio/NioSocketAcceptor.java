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
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Executor;

import org.apache.mina.common.AbstractPollingIoAcceptor;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoProcessor;
import org.apache.mina.common.TransportMetadata;
import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.SocketSessionConfig;

/**
 * {@link IoAcceptor} for socket transport (TCP/IP).  This class
 * handles incoming TCP/IP based socket connections.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 389042 $, $Date: 2006-03-27 07:49:41Z $
 */
public final class NioSocketAcceptor
        extends AbstractPollingIoAcceptor<NioSession, ServerSocketChannel> 
        implements SocketAcceptor {

    private int backlog = 50;
    private boolean reuseAddress = true;

    private volatile Selector selector;

    /**
     * Create an acceptor with a single processing thread using a NewThreadExecutor
     */
    public NioSocketAcceptor() {
        super(new DefaultSocketSessionConfig(), NioProcessor.class);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
    }

    public NioSocketAcceptor(int processorCount) {
        super(new DefaultSocketSessionConfig(), NioProcessor.class, processorCount);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
    }
    
    public NioSocketAcceptor(IoProcessor<NioSession> processor) {
        super(new DefaultSocketSessionConfig(), processor);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
    }

    public NioSocketAcceptor(Executor executor, IoProcessor<NioSession> processor) {
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
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) super.getLocalAddress();
    }
    
    @Override
    public InetSocketAddress getDefaultLocalAddress() {
        return (InetSocketAddress) super.getDefaultLocalAddress();
    }

    public void setDefaultLocalAddress(InetSocketAddress localAddress) {
        setDefaultLocalAddress((SocketAddress) localAddress);
    }

    public boolean isReuseAddress() {
        return reuseAddress;
    }

    public void setReuseAddress(boolean reuseAddress) {
        synchronized (bindLock) {
            if (isActive()) {
                throw new IllegalStateException(
                        "reuseAddress can't be set while the acceptor is bound.");
            }

            this.reuseAddress = reuseAddress;
        }
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        synchronized (bindLock) {
            if (isActive()) {
                throw new IllegalStateException(
                        "backlog can't be set while the acceptor is bound.");
            }

            this.backlog = backlog;
        }
    }

    
    @Override
    protected NioSession accept(IoProcessor<NioSession> processor,
            ServerSocketChannel handle) throws Exception {

        SelectionKey key = handle.keyFor(selector);
        if (!key.isAcceptable()) {
            return null;
        }

        // accept the connection from the client
        SocketChannel ch = handle.accept();
        if (ch == null) {
            return null;
        }

        return new NioSocketSession(this, processor, ch);
    }

    @Override
    protected ServerSocketChannel open(SocketAddress localAddress)
            throws Exception {
        ServerSocketChannel c = ServerSocketChannel.open();
        boolean success = false;
        try {
            c.configureBlocking(false);
            // Configure the server socket,
            c.socket().setReuseAddress(isReuseAddress());
            // XXX: Do we need to provide this property? (I think we need to remove it.)
            c.socket().setReceiveBufferSize(
                    getSessionConfig().getReceiveBufferSize());
            // and bind.
            c.socket().bind(localAddress, getBacklog());
            c.register(selector, SelectionKey.OP_ACCEPT);
            success = true;
        } finally {
            if (!success) {
                close(c);
            }
        }
        return c;
    }

    @Override
    protected SocketAddress localAddress(ServerSocketChannel handle)
            throws Exception {
        return handle.socket().getLocalSocketAddress();
    }

    @Override
    protected boolean select() throws Exception {
        return selector.select() > 0;
    }

    @Override
    protected Iterator<ServerSocketChannel> selectedHandles() {
        return new ServerSocketChannelIterator(selector.selectedKeys());
    }

    @Override
    protected void close(ServerSocketChannel handle) throws Exception {
        SelectionKey key = handle.keyFor(selector);
        if (key != null) {
            key.cancel();
        }
        handle.close();
    }

    @Override
    protected void wakeup() {
        selector.wakeup();
    }

    private static class ServerSocketChannelIterator implements Iterator<ServerSocketChannel> {
        
        private final Iterator<SelectionKey> i;
        
        private ServerSocketChannelIterator(Collection<SelectionKey> selectedKeys) {
            this.i = selectedKeys.iterator();
        }

        public boolean hasNext() {
            return i.hasNext();
        }

        public ServerSocketChannel next() {
            SelectionKey key = i.next();
            return (ServerSocketChannel) key.channel();
        }

        public void remove() {
            i.remove();
        }
    }
}
