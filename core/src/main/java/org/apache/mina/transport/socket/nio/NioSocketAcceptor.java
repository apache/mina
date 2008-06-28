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

import org.apache.mina.core.polling.AbstractPollingIoAcceptor;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.SimpleIoProcessorPool;
import org.apache.mina.core.service.TransportMetadata;
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
    private boolean reuseAddress = false;

    private volatile Selector selector;

    /**
     * Constructor for {@link NioSocketAcceptor} using default parameters (multiple thread model).
     */
    public NioSocketAcceptor() {
        super(new DefaultSocketSessionConfig(), NioProcessor.class);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
    }

    /**
     * Constructor for {@link NioSocketAcceptor} using default parameters, and 
     * given number of {@link NioProcessor} for multithreading I/O operations.
     * 
     * @param processorCount the number of processor to create and place in a
     * {@link SimpleIoProcessorPool} 
     */
    public NioSocketAcceptor(int processorCount) {
        super(new DefaultSocketSessionConfig(), NioProcessor.class, processorCount);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
    }

    /**
    *  Constructor for {@link NioSocketAcceptor} with default configuration but a
     *  specific {@link IoProcessor}, useful for sharing the same processor over multiple
     *  {@link IoService} of the same type.
     * @param processor the processor to use for managing I/O events
     */
    public NioSocketAcceptor(IoProcessor<NioSession> processor) {
        super(new DefaultSocketSessionConfig(), processor);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
    }

    /**
     *  Constructor for {@link NioSocketAcceptor} with a given {@link Executor} for handling 
     *  connection events and a given {@link IoProcessor} for handling I/O events, useful for 
     *  sharing the same processor and executor over multiple {@link IoService} of the same type.
     * @param executor the executor for connection
     * @param processor the processor for I/O operations
     */
    public NioSocketAcceptor(Executor executor, IoProcessor<NioSession> processor) {
        super(new DefaultSocketSessionConfig(), executor, processor);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init() throws Exception {
        selector = Selector.open();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void destroy() throws Exception {
        if (selector != null) {
            selector.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public TransportMetadata getTransportMetadata() {
        return NioSocketSession.METADATA;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketSessionConfig getSessionConfig() {
        return (SocketSessionConfig) super.getSessionConfig();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) super.getLocalAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InetSocketAddress getDefaultLocalAddress() {
        return (InetSocketAddress) super.getDefaultLocalAddress();
    }

    /**
     * {@inheritDoc}
     */
    public void setDefaultLocalAddress(InetSocketAddress localAddress) {
        setDefaultLocalAddress((SocketAddress) localAddress);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isReuseAddress() {
        return reuseAddress;
    }

    /**
     * {@inheritDoc}
     */
    public void setReuseAddress(boolean reuseAddress) {
        synchronized (bindLock) {
            if (isActive()) {
                throw new IllegalStateException(
                        "reuseAddress can't be set while the acceptor is bound.");
            }

            this.reuseAddress = reuseAddress;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getBacklog() {
        return backlog;
    }

    /**
     * {@inheritDoc}
     */
    public void setBacklog(int backlog) {
        synchronized (bindLock) {
            if (isActive()) {
                throw new IllegalStateException(
                        "backlog can't be set while the acceptor is bound.");
            }

            this.backlog = backlog;
        }
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected SocketAddress localAddress(ServerSocketChannel handle)
            throws Exception {
        return handle.socket().getLocalSocketAddress();
    }

    /**
      * Check if we have at least one key whose corresponding channels is 
      * ready for I/O operations.
      *
      * This method performs a blocking selection operation. 
      * It returns only after at least one channel is selected, 
      * this selector's wakeup method is invoked, or the current thread 
      * is interrupted, whichever comes first.
      * 
      * @return <code>true</code> if one key has its ready-operation set updated
      * @throws IOException If an I/O error occurs
      * @throws ClosedSelectorException If this selector is closed 
      */
    @Override
    protected boolean select() throws Exception {
        return selector.select() > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Iterator<ServerSocketChannel> selectedHandles() {
        return new ServerSocketChannelIterator(selector.selectedKeys());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void close(ServerSocketChannel handle) throws Exception {
        SelectionKey key = handle.keyFor(selector);
        if (key != null) {
            key.cancel();
        }
        handle.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void wakeup() {
        selector.wakeup();
    }

    private static class ServerSocketChannelIterator implements Iterator<ServerSocketChannel> {

        private final Iterator<SelectionKey> i;

        private ServerSocketChannelIterator(Collection<SelectionKey> selectedKeys) {
            i = selectedKeys.iterator();
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return i.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        public ServerSocketChannel next() {
            SelectionKey key = i.next();
            return (ServerSocketChannel) key.channel();
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            i.remove();
        }
    }
}
