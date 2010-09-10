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
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Executor;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.polling.AbstractPollingConnectionlessIoAcceptor;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.transport.socket.DatagramAcceptor;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.DefaultDatagramSessionConfig;

/**
 * {@link IoAcceptor} for datagram transport (UDP/IP).
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @org.apache.xbean.XBean
 */
public final class NioDatagramAcceptor
        extends AbstractPollingConnectionlessIoAcceptor<NioSession, DatagramChannel>
        implements DatagramAcceptor {

    private volatile Selector selector;

    /**
     * Creates a new instance.
     */
    public NioDatagramAcceptor() {
        super(new DefaultDatagramSessionConfig());
    }

    /**
     * Creates a new instance.
     */
    public NioDatagramAcceptor(Executor executor) {
        super(new DefaultDatagramSessionConfig(), executor);
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
        return NioDatagramSession.METADATA;
    }

    @Override
    public DatagramSessionConfig getSessionConfig() {
        return (DatagramSessionConfig) super.getSessionConfig();
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

    @Override
    protected DatagramChannel open(SocketAddress localAddress) throws Exception {
        final DatagramChannel c = DatagramChannel.open();
        boolean success = false;
        try {
            new NioDatagramSessionConfig(c).setAll(getSessionConfig());
            c.configureBlocking(false);
            c.socket().bind(localAddress);
            c.register(selector, SelectionKey.OP_READ);
            success = true;
        } finally {
            if (!success) {
                close(c);
            }
        }

        return c;
    }

    @Override
    protected boolean isReadable(DatagramChannel handle) {
        SelectionKey key = handle.keyFor(selector);

        if ((key == null) || (!key.isValid())) {
            return false;
        }

        return key.isReadable();
    }

    @Override
    protected boolean isWritable(DatagramChannel handle) {
        SelectionKey key = handle.keyFor(selector);

        if ((key == null) || (!key.isValid())) {
            return false;
        }

        return key.isWritable();
    }

    @Override
    protected SocketAddress localAddress(DatagramChannel handle)
            throws Exception {
        return handle.socket().getLocalSocketAddress();
    }

    @Override
    protected NioSession newSession(
            IoProcessor<NioSession> processor, DatagramChannel handle,
            SocketAddress remoteAddress) {
        SelectionKey key = handle.keyFor(selector);
        
        if ((key == null) || (!key.isValid())) {
            return null;
        }
        
        NioDatagramSession newSession = new NioDatagramSession(
                this, handle, processor, remoteAddress);
        newSession.setSelectionKey(key);
        
        return newSession;
    }

    @Override
    protected SocketAddress receive(DatagramChannel handle, IoBuffer buffer)
            throws Exception {
        return handle.receive(buffer.buf());
    }

    @Override
    protected int select() throws Exception {
        return selector.select();
    }

    @Override
    protected int select(long timeout) throws Exception {
        return selector.select(timeout);
    }

    @Override
    protected Iterator<DatagramChannel> selectedHandles() {
        return new DatagramChannelIterator(selector.selectedKeys());
    }

    @Override
    protected int send(NioSession session, IoBuffer buffer,
            SocketAddress remoteAddress) throws Exception {
        return ((DatagramChannel) session.getChannel()).send(
                buffer.buf(), remoteAddress);
    }

    @Override
    protected void setInterestedInWrite(NioSession session, boolean isInterested)
            throws Exception {
        SelectionKey key = session.getSelectionKey();

        if (key == null) {
            return;
        }
        
        int newInterestOps = key.interestOps();

        if (isInterested) {
            newInterestOps |= SelectionKey.OP_WRITE;
            //newInterestOps &= ~SelectionKey.OP_READ;
        } else {
            newInterestOps &= ~SelectionKey.OP_WRITE;
            //newInterestOps |= SelectionKey.OP_READ;
        }

        key.interestOps(newInterestOps);
    }

    @Override
    protected void close(DatagramChannel handle) throws Exception {
        SelectionKey key = handle.keyFor(selector);

        if (key != null) {
            key.cancel();
        }
        
        handle.disconnect();
        handle.close();
    }

    @Override
    protected void wakeup() {
        selector.wakeup();
    }
    
    private static class DatagramChannelIterator implements Iterator<DatagramChannel> {
        
        private final Iterator<SelectionKey> i;
        
        private DatagramChannelIterator(Collection<SelectionKey> keys) {
            this.i = keys.iterator();
        }
        
        public boolean hasNext() {
            return i.hasNext();
        }

        public DatagramChannel next() {
            return (DatagramChannel) i.next().channel();
        }

        public void remove() {
            i.remove();
        }
        
    }
}
