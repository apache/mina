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
import java.util.concurrent.Executor;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoSessionRecycler;
import org.apache.mina.common.support.DelegatedIoAcceptor;
import org.apache.mina.transport.socket.nio.support.DatagramAcceptorDelegate;
import org.apache.mina.util.NewThreadExecutor;

/**
 * {@link IoAcceptor} for datagram transport (UDP/IP).
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DatagramAcceptor extends DelegatedIoAcceptor {
    /**
     * Creates a new instance using a NewThreadExecutor
     */
    public DatagramAcceptor() {
        init(new DatagramAcceptorDelegate(this, new NewThreadExecutor()));
    }

    /**
     * Creates a new instance.
     * 
     * @param executor Executor to use for launching threads
     */
    public DatagramAcceptor(Executor executor) {
        init(new DatagramAcceptorDelegate(this, executor));
    }

    @Override
    public DatagramSessionConfig getSessionConfig() {
        return ((DatagramAcceptorDelegate) delegate).getSessionConfig();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return ((DatagramAcceptorDelegate) delegate).getLocalAddress();
    }

    // This method is overriden to work around a problem with
    // bean property access mechanism.

    @Override
    public void setLocalAddress(SocketAddress localAddress) {
        super.setLocalAddress(localAddress);
    }

    /**
     * Returns the {@link IoSessionRecycler} for this service.
     */
    public IoSessionRecycler getSessionRecycler() {
        return ((DatagramAcceptorDelegate) delegate).getSessionRecycler();
    }

    /**
     * Sets the {@link IoSessionRecycler} for this service.
     * 
     * @param sessionRecycler <tt>null</tt> to use the default recycler
     */
    public void setSessionRecycler(IoSessionRecycler sessionRecycler) {
        ((DatagramAcceptorDelegate) delegate)
                .setSessionRecycler(sessionRecycler);
    }
}
