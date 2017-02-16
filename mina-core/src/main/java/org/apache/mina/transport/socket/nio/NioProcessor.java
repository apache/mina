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
import java.nio.channels.ByteChannel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.FileRegion;
import org.apache.mina.core.polling.AbstractPollingIoProcessor;
import org.apache.mina.core.session.SessionState;

/**
 * A processor for incoming and outgoing data get and written on a TCP socket.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public final class NioProcessor extends AbstractPollingIoProcessor<NioSession> {
    /** The selector associated with this processor */
    private Selector selector;
    
    /** A lock used to protect concurent access to the selector */
    private ReadWriteLock selectorLock = new ReentrantReadWriteLock();

    private SelectorProvider selectorProvider = null;

    /**
     *
     * Creates a new instance of NioProcessor.
     *
     * @param executor The executor to use
     */
    public NioProcessor(Executor executor) {
        super(executor);

        try {
            // Open a new selector
            selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeIoException("Failed to open a selector.", e);
        }
    }

    /**
     *
     * Creates a new instance of NioProcessor.
     *
     * @param executor The executor to use
     * @param selectorProvider The Selector provider to use
     */
    public NioProcessor(Executor executor, SelectorProvider selectorProvider) {
        super(executor);

        try {
            // Open a new selector
            if (selectorProvider == null) {
                selector = Selector.open();
            } else {
                this.selectorProvider = selectorProvider;
                selector = selectorProvider.openSelector();
            }
        } catch (IOException e) {
            throw new RuntimeIoException("Failed to open a selector.", e);
        }
    }

    @Override
    protected void doDispose() throws Exception {
        selectorLock.readLock().lock();
        
        try {
            selector.close();
        } finally {
            selectorLock.readLock().unlock();
        }
    }

    @Override
    protected int select(long timeout) throws Exception {
        selectorLock.readLock().lock();
        
        try {
            return selector.select(timeout);
        } finally {
            selectorLock.readLock().unlock();
        }
    }

    @Override
    protected int select() throws Exception {
        selectorLock.readLock().lock();
        
        try {
            return selector.select();
        } finally {
            selectorLock.readLock().unlock();
        }
    }

    @Override
    protected boolean isSelectorEmpty() {
        selectorLock.readLock().lock();
        
        try {
            return selector.keys().isEmpty();
        } finally {
            selectorLock.readLock().unlock();
        }
    }

    @Override
    protected void wakeup() {
        wakeupCalled.getAndSet(true);
        selectorLock.readLock().lock();
        
        try {
            selector.wakeup();
        } finally {
            selectorLock.readLock().unlock();
        }
    }

    @Override
    protected Iterator<NioSession> allSessions() {
        selectorLock.readLock().lock();
        
        try {
            return new IoSessionIterator(selector.keys());
        } finally {
            selectorLock.readLock().unlock();
        }
    }

    @SuppressWarnings("synthetic-access")
    @Override
    protected Iterator<NioSession> selectedSessions() {
        return new IoSessionIterator(selector.selectedKeys());
    }

    @Override
    protected void init(NioSession session) throws Exception {
        SelectableChannel ch = (SelectableChannel) session.getChannel();
        ch.configureBlocking(false);
        selectorLock.readLock().lock();
        
        try {
            session.setSelectionKey(ch.register(selector, SelectionKey.OP_READ, session));
        } finally {
            selectorLock.readLock().unlock();
        }
    }

    @Override
    protected void destroy(NioSession session) throws Exception {
        ByteChannel ch = session.getChannel();
        
        SelectionKey key = session.getSelectionKey();
        
        if (key != null) {
            key.cancel();
        }
        
        if ( ch.isOpen() ) {
            ch.close();
        }
    }

    /**
     * In the case we are using the java select() method, this method is used to
     * trash the buggy selector and create a new one, registering all the
     * sockets on it.
     */
    @Override
    protected void registerNewSelector() throws IOException {
        selectorLock.writeLock().lock();
        
        try {
            Set<SelectionKey> keys = selector.keys();
            Selector newSelector;

            // Open a new selector
            if (selectorProvider == null) {
                newSelector = Selector.open();
            } else {
                newSelector = selectorProvider.openSelector();
            }

            // Loop on all the registered keys, and register them on the new selector
            for (SelectionKey key : keys) {
                SelectableChannel ch = key.channel();

                // Don't forget to attache the session, and back !
                NioSession session = (NioSession) key.attachment();
                SelectionKey newKey = ch.register(newSelector, key.interestOps(), session);
                session.setSelectionKey(newKey);
            }

            // Now we can close the old selector and switch it
            selector.close();
            selector = newSelector;
        } finally {
            selectorLock.writeLock().unlock();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isBrokenConnection() throws IOException {
        // A flag set to true if we find a broken session
        boolean brokenSession = false;

        selectorLock.readLock().lock();
        
        try {
            // Get the selector keys
            Set<SelectionKey> keys = selector.keys();

            // Loop on all the keys to see if one of them
            // has a closed channel
            for (SelectionKey key : keys) {
                SelectableChannel channel = key.channel();

                if (((channel instanceof DatagramChannel) && !((DatagramChannel) channel).isConnected())
                        || ((channel instanceof SocketChannel) && !((SocketChannel) channel).isConnected())) {
                    // The channel is not connected anymore. Cancel
                    // the associated key then.
                    key.cancel();

                    // Set the flag to true to avoid a selector switch
                    brokenSession = true;
                }
            }
        } finally {
            selectorLock.readLock().unlock();
        }

        return brokenSession;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SessionState getState(NioSession session) {
        SelectionKey key = session.getSelectionKey();

        if (key == null) {
            // The channel is not yet registred to a selector
            return SessionState.OPENING;
        }

        if (key.isValid()) {
            // The session is opened
            return SessionState.OPENED;
        } else {
            // The session still as to be closed
            return SessionState.CLOSING;
        }
    }

    @Override
    protected boolean isReadable(NioSession session) {
        SelectionKey key = session.getSelectionKey();

        return (key != null) && key.isValid() && key.isReadable();
    }

    @Override
    protected boolean isWritable(NioSession session) {
        SelectionKey key = session.getSelectionKey();

        return (key != null) && key.isValid() && key.isWritable();
    }

    @Override
    protected boolean isInterestedInRead(NioSession session) {
        SelectionKey key = session.getSelectionKey();

        return (key != null) && key.isValid() && ((key.interestOps() & SelectionKey.OP_READ) != 0);
    }

    @Override
    protected boolean isInterestedInWrite(NioSession session) {
        SelectionKey key = session.getSelectionKey();

        return (key != null) && key.isValid() && ((key.interestOps() & SelectionKey.OP_WRITE) != 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInterestedInRead(NioSession session, boolean isInterested) throws Exception {
        SelectionKey key = session.getSelectionKey();

        if ((key == null) || !key.isValid()) {
            return;
        }

        int oldInterestOps = key.interestOps();
        int newInterestOps = oldInterestOps;

        if (isInterested) {
            newInterestOps |= SelectionKey.OP_READ;
        } else {
            newInterestOps &= ~SelectionKey.OP_READ;
        }

        if (oldInterestOps != newInterestOps) {
            key.interestOps(newInterestOps);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInterestedInWrite(NioSession session, boolean isInterested) throws Exception {
        SelectionKey key = session.getSelectionKey();

        if ((key == null) || !key.isValid()) {
            return;
        }

        int newInterestOps = key.interestOps();

        if (isInterested) {
            newInterestOps |= SelectionKey.OP_WRITE;
        } else {
            newInterestOps &= ~SelectionKey.OP_WRITE;
        }

        key.interestOps(newInterestOps);
    }

    @Override
    protected int read(NioSession session, IoBuffer buf) throws Exception {
        ByteChannel channel = session.getChannel();

        return channel.read(buf.buf());
    }

    @Override
    protected int write(NioSession session, IoBuffer buf, int length) throws IOException {
        if (buf.remaining() <= length) {
            return session.getChannel().write(buf.buf());
        }

        int oldLimit = buf.limit();
        buf.limit(buf.position() + length);
        try {
            return session.getChannel().write(buf.buf());
        } finally {
            buf.limit(oldLimit);
        }
    }

    @Override
    protected int transferFile(NioSession session, FileRegion region, int length) throws Exception {
        try {
            return (int) region.getFileChannel().transferTo(region.getPosition(), length, session.getChannel());
        } catch (IOException e) {
            // Check to see if the IOException is being thrown due to
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5103988
            String message = e.getMessage();
            if ((message != null) && message.contains("temporarily unavailable")) {
                return 0;
            }

            throw e;
        }
    }

    /**
     * An encapsulating iterator around the {@link Selector#selectedKeys()} or
     * the {@link Selector#keys()} iterator;
     */
    protected static class IoSessionIterator<NioSession> implements Iterator<NioSession> {
        private final Iterator<SelectionKey> iterator;

        /**
         * Create this iterator as a wrapper on top of the selectionKey Set.
         *
         * @param keys
         *            The set of selected sessions
         */
        private IoSessionIterator(Set<SelectionKey> keys) {
            iterator = keys.iterator();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NioSession next() {
            SelectionKey key = iterator.next();
            
            return (NioSession) key.attachment();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {
            iterator.remove();
        }
    }
}
