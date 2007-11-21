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
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Executor;

import org.apache.mina.common.AbstractPollingConnectionlessIoAcceptor;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.RuntimeIoException;
import org.apache.mina.common.TransportMetadata;
import org.apache.mina.transport.socket.DatagramAcceptor;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.DefaultDatagramSessionConfig;

/**
 * {@link IoAcceptor} for datagram transport (UDP/IP).
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class NioDatagramAcceptor
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
    protected void doInit() {
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeIoException("Failed to open a selector.", e);
        }
    }

    @Override
    protected void doDispose0() {
        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                ExceptionMonitor.getInstance().exceptionCaught(e);
            }
        }
    }

    public TransportMetadata getTransportMetadata() {
        return NioDatagramSession.METADATA;
    }

    @Override
    public DatagramSessionConfig getSessionConfig() {
        return super.getSessionConfig();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return super.getLocalAddress();
    }

    @Override
    public void setLocalAddress(InetSocketAddress localAddress) {
        setLocalAddress((SocketAddress) localAddress);
    }

    @Override
    protected DatagramChannel bind(SocketAddress localAddress) throws Exception {
        DatagramChannel c = DatagramChannel.open();
        boolean success = false;
        try {
            DatagramSessionConfig cfg = getSessionConfig();
            c.socket().setReuseAddress(cfg.isReuseAddress());
            c.socket().setBroadcast(cfg.isBroadcast());
            c.socket().setReceiveBufferSize(cfg.getReceiveBufferSize());
            c.socket().setSendBufferSize(cfg.getSendBufferSize());
    
            if (c.socket().getTrafficClass() != cfg.getTrafficClass()) {
                c.socket().setTrafficClass(cfg.getTrafficClass());
            }
    
            c.configureBlocking(false);
            c.socket().bind(localAddress);
            c.register(selector, SelectionKey.OP_READ);
            success = true;
        } finally {
            if (!success) {
                unbind(c);
            }
        }

        return c;
    }

    @Override
    protected boolean isReadable(DatagramChannel handle) {
        SelectionKey key = handle.keyFor(selector);
        if (key == null) {
            return false;
        }
        if (!key.isValid()) {
            return false;
        }
        return key.isReadable();
    }

    @Override
    protected boolean isWritable(DatagramChannel handle) {
        SelectionKey key = handle.keyFor(selector);
        if (key == null) {
            return false;
        }
        if (!key.isValid()) {
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
    protected NioSession newSession(DatagramChannel handle,
            SocketAddress remoteAddress) {
        SelectionKey key = handle.keyFor(selector);
        if (key == null) {
            return null;
        }
        NioDatagramSession newSession = new NioDatagramSession(
                this, handle, getProcessor(), remoteAddress);
        newSession.setSelectionKey(key);
        
        return newSession;
    }

    @Override
    protected SocketAddress receive(DatagramChannel handle, IoBuffer buffer)
            throws Exception {
        return handle.receive(buffer.buf());
    }

    @Override
    protected boolean select(int timeout) throws Exception {
        return selector.select(timeout) > 0;
    }

    @Override
    protected boolean selectable() {
        return selector.isOpen();
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
    protected void setInterestedInWrite(NioSession session, boolean interested)
            throws Exception {
        SelectionKey key = session.getSelectionKey();
        if (key == null) {
            return;
        }
        
        if (interested) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        } else {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    @Override
    protected void unbind(DatagramChannel handle) throws Exception {
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
