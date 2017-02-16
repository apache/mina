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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Executor;

import org.apache.mina.core.polling.AbstractPollingIoAcceptor;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.apache.mina.transport.socket.SocketAcceptor;

/**
 * {@link IoAcceptor} for socket transport (TCP/IP).  This class
 * handles incoming TCP/IP based socket connections.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public final class NioSocketAcceptor extends AbstractPollingIoAcceptor<NioSession, ServerSocketChannel>
implements SocketAcceptor {

    private volatile Selector selector;
    private volatile SelectorProvider selectorProvider = null;

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
     * Constructor for {@link NioSocketAcceptor} using default parameters, and
     * given number of {@link NioProcessor} for multithreading I/O operations, and
     * a custom SelectorProvider for NIO
     *
     * @param processorCount the number of processor to create and place in a
     * @param selectorProvider teh SelectorProvider to use
     * {@link SimpleIoProcessorPool}
     */
    public NioSocketAcceptor(int processorCount, SelectorProvider selectorProvider) {
        super(new DefaultSocketSessionConfig(), NioProcessor.class, processorCount, selectorProvider);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
        this.selectorProvider = selectorProvider;
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
    protected void init(SelectorProvider selectorProvider) throws Exception {
        this.selectorProvider = selectorProvider;

        if (selectorProvider == null) {
            selector = Selector.open();
        } else {
            selector = selectorProvider.openSelector();
        }
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
    @Override
    protected NioSession accept(IoProcessor<NioSession> processor, ServerSocketChannel handle) throws Exception {

        SelectionKey key = null;

        if (handle != null) {
            key = handle.keyFor(selector);
        }

        if ((key == null) || (!key.isValid()) || (!key.isAcceptable())) {
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
    protected ServerSocketChannel open(SocketAddress localAddress) throws Exception {
        // Creates the listening ServerSocket

        ServerSocketChannel channel = null;

        if (selectorProvider != null) {
            channel = selectorProvider.openServerSocketChannel();
        } else {
            channel = ServerSocketChannel.open();
        }

        boolean success = false;

        try {
            // This is a non blocking socket channel
            channel.configureBlocking(false);

            // Configure the server socket,
            ServerSocket socket = channel.socket();

            // Set the reuseAddress flag accordingly with the setting
            socket.setReuseAddress(isReuseAddress());

            // and bind.
            try {
                socket.bind(localAddress, getBacklog());
            } catch (IOException ioe) {
                // Add some info regarding the address we try to bind to the
                // message
                String newMessage = "Error while binding on " + localAddress + "\n" + "original message : "
                        + ioe.getMessage();
                Exception e = new IOException(newMessage);
                e.initCause(ioe.getCause());

                // And close the channel
                channel.close();

                throw e;
            }

            // Register the channel within the selector for ACCEPT event
            channel.register(selector, SelectionKey.OP_ACCEPT);
            success = true;
        } finally {
            if (!success) {
                close(channel);
            }
        }
        return channel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SocketAddress localAddress(ServerSocketChannel handle) throws Exception {
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
     * @return The number of keys having their ready-operation set updated
     * @throws IOException If an I/O error occurs
     * @throws ClosedSelectorException If this selector is closed
     */
    @Override
    protected int select() throws Exception {
        return selector.select();
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

    /**
     * Defines an iterator for the selected-key Set returned by the
     * selector.selectedKeys(). It replaces the SelectionKey operator.
     */
    private static class ServerSocketChannelIterator implements Iterator<ServerSocketChannel> {
        /** The selected-key iterator */
        private final Iterator<SelectionKey> iterator;

        /**
         * Build a SocketChannel iterator which will return a SocketChannel instead of
         * a SelectionKey.
         * 
         * @param selectedKeys The selector selected-key set
         */
        private ServerSocketChannelIterator(Collection<SelectionKey> selectedKeys) {
            iterator = selectedKeys.iterator();
        }

        /**
         * Tells if there are more SockectChannel left in the iterator
         * @return <tt>true</tt> if there is at least one more
         * SockectChannel object to read
         */
        public boolean hasNext() {
            return iterator.hasNext();
        }

        /**
         * Get the next SocketChannel in the operator we have built from
         * the selected-key et for this selector.
         * 
         * @return The next SocketChannel in the iterator
         */
        public ServerSocketChannel next() {
            SelectionKey key = iterator.next();

            if (key.isValid() && key.isAcceptable()) {
                return (ServerSocketChannel) key.channel();
            }

            return null;
        }

        /**
         * Remove the current SocketChannel from the iterator
         */
        public void remove() {
            iterator.remove();
        }
    }
}
