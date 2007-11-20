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

import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.Iterator;

import org.apache.mina.common.AbstractPollingIoConnector;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoProcessor;
import org.apache.mina.common.TransportMetadata;
import org.apache.mina.transport.socket.DatagramConnector;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.DefaultDatagramSessionConfig;

/**
 * {@link IoConnector} for datagram transport (UDP/IP).
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class NioDatagramConnector
        extends AbstractPollingIoConnector<NioSession, DatagramChannel>
        implements DatagramConnector {

    /**
     * Creates a new instance.
     */
    public NioDatagramConnector() {
        super(new DefaultDatagramSessionConfig(), NioProcessor.class);
    }

    /**
     * Creates a new instance.
     */
    public NioDatagramConnector(int processorCount) {
        super(new DefaultDatagramSessionConfig(), NioProcessor.class, processorCount);
    }

    /**
     * Creates a new instance.
     */
    public NioDatagramConnector(IoProcessor<NioSession> processor) {
        super(new DefaultDatagramSessionConfig(), processor);
    }
    
    public TransportMetadata getTransportMetadata() {
        return NioDatagramSession.METADATA;
    }
    
    @Override
    public DatagramSessionConfig getSessionConfig() {
        return (DatagramSessionConfig) super.getSessionConfig();
    }

    @Override
    protected void doInit() {
    }

    @Override
    protected DatagramChannel newHandle(SocketAddress localAddress)
            throws Exception {
        DatagramChannel ch = DatagramChannel.open();
        ch.socket().setReuseAddress(getSessionConfig().isReuseAddress());
        ch.socket().setReuseAddress(true);
        ch.socket().setBroadcast(getSessionConfig().isBroadcast());

        if (localAddress != null) {
            ch.socket().bind(localAddress);
        }
        
        return ch;
    }

    @Override
    protected boolean connect(DatagramChannel handle,
            SocketAddress remoteAddress) throws Exception {
        handle.connect(remoteAddress);
        return true;
    }

    @Override
    protected NioSession newSession(IoProcessor<NioSession> processor,
            DatagramChannel handle) throws Exception {
        return new NioDatagramSession(this, handle, processor);
    }

    @Override
    protected void destroy(DatagramChannel handle) throws Exception {
        handle.disconnect();
        handle.close();
    }
    
    // Unused extension points.
    @Override
    protected Iterator<DatagramChannel> allHandles() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected ConnectionRequest connectionRequest(DatagramChannel handle) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doDispose0() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void finishConnect(DatagramChannel handle) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void register(DatagramChannel handle, ConnectionRequest request)
            throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean select(int timeout) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean selectable() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Iterator<DatagramChannel> selectedHandles() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void wakeup() {
        throw new UnsupportedOperationException();
    }
}
