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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;
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
public final class NioDatagramAcceptor extends AbstractPollingConnectionlessIoAcceptor<NioSession, DatagramChannel> {

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

    /**
     * {@inheritDoc}
     */
    public DatagramSessionConfig getSessionConfig() {
        return (DatagramSessionConfig) sessionConfig;
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
    protected SocketAddress localAddress(DatagramChannel handle) throws Exception {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) handle.socket().getLocalSocketAddress();
        InetAddress inetAddress = inetSocketAddress.getAddress();

        if ((inetAddress instanceof Inet6Address) && (((Inet6Address) inetAddress).isIPv4CompatibleAddress())) {
            // Ugly hack to workaround a problem on linux : the ANY address is always converted to IPV6
            // even if the original address was an IPV4 address. We do store the two IPV4 and IPV6
            // ANY address in the map.
            byte[] ipV6Address = ((Inet6Address) inetAddress).getAddress();
            byte[] ipV4Address = new byte[4];

            for (int i = 0; i < 4; i++) {
                ipV4Address[i] = ipV6Address[12 + i];
            }

            InetAddress inet4Adress = Inet4Address.getByAddress(ipV4Address);
            return new InetSocketAddress(inet4Adress, inetSocketAddress.getPort());
        } else {
            return inetSocketAddress;
        }
    }

    @Override
    protected NioSession newSession(IoProcessor<NioSession> processor, DatagramChannel handle,
            SocketAddress remoteAddress) {
        SelectionKey key = handle.keyFor(selector);

        if ((key == null) || (!key.isValid())) {
            return null;
        }

        NioDatagramSession newSession = new NioDatagramSession(this, handle, processor, remoteAddress);
        newSession.setSelectionKey(key);

        return newSession;
    }

    @Override
    protected SocketAddress receive(DatagramChannel handle, IoBuffer buffer) throws Exception {
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
    protected Set<SelectionKey> selectedHandles() {
        return selector.selectedKeys();
    }

    @Override
    protected int send(NioSession session, IoBuffer buffer, SocketAddress remoteAddress) throws Exception {
        return ((DatagramChannel) session.getChannel()).send(buffer.buf(), remoteAddress);
    }

    @Override
    protected void setInterestedInWrite(NioSession session, boolean isInterested) throws Exception {
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
}