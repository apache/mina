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
package org.apache.mina.transport.vmpipe;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.support.AbstractIoFilterChain;
import org.apache.mina.common.support.BaseIoConnector;
import org.apache.mina.common.support.DefaultConnectFuture;
import org.apache.mina.common.support.SessionIdleStatusChecker;
import org.apache.mina.transport.vmpipe.support.VmPipe;
import org.apache.mina.transport.vmpipe.support.VmPipeFilterChain;
import org.apache.mina.transport.vmpipe.support.VmPipeSessionImpl;

/**
 * Connects to {@link IoHandler}s which is bound on the specified
 * {@link VmPipeAddress}.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class VmPipeConnector extends BaseIoConnector {

    /**
     * Creates a new instance.
     */
    public VmPipeConnector() {
        super(new DefaultVmPipeSessionConfig());
    }

    public TransportType getTransportType() {
        return TransportType.VM_PIPE;
    }

    @Override
    public VmPipeSessionConfig getSessionConfig() {
        return (VmPipeSessionConfig) super.getSessionConfig();
    }

    @Override
    protected ConnectFuture doConnect(SocketAddress remoteAddress,
            SocketAddress localAddress) {
        VmPipe entry = VmPipeAcceptor.boundHandlers.get(remoteAddress);
        if (entry == null) {
            return DefaultConnectFuture.newFailedFuture(new IOException(
                    "Endpoint unavailable: " + remoteAddress));
        }

        DefaultConnectFuture future = new DefaultConnectFuture();

        // Assign the local address dynamically,
        VmPipeAddress actualLocalAddress;
        try {
            actualLocalAddress = nextLocalAddress();
        } catch (IOException e) {
            return DefaultConnectFuture.newFailedFuture(e);
        }

        VmPipeSessionImpl localSession = new VmPipeSessionImpl(this,
                getListeners(), actualLocalAddress, getHandler(), entry);

        // and reclaim the local address when the connection is closed.
        localSession.getCloseFuture().addListener(LOCAL_ADDRESS_RECLAIMER);

        // initialize connector session
        try {
            IoFilterChain filterChain = localSession.getFilterChain();
            this.getFilterChainBuilder().buildFilterChain(filterChain);

            // The following sentences don't throw any exceptions.
            localSession.setAttribute(AbstractIoFilterChain.CONNECT_FUTURE,
                    future);
            getListeners().fireSessionCreated(localSession);
            SessionIdleStatusChecker.getInstance().addSession(localSession);
        } catch (Throwable t) {
            future.setException(t);
            return future;
        }

        // initialize acceptor session
        VmPipeSessionImpl remoteSession = localSession.getRemoteSession();
        try {
            IoFilterChain filterChain = remoteSession.getFilterChain();
            entry.getAcceptor().getFilterChainBuilder().buildFilterChain(
                    filterChain);

            // The following sentences don't throw any exceptions.
            entry.getListeners().fireSessionCreated(remoteSession);
            SessionIdleStatusChecker.getInstance().addSession(remoteSession);
        } catch (Throwable t) {
            ExceptionMonitor.getInstance().exceptionCaught(t);
            remoteSession.close();
        }

        ((VmPipeFilterChain) localSession.getFilterChain()).start();
        ((VmPipeFilterChain) remoteSession.getFilterChain()).start();

        return future;
    }

    private static final Set<VmPipeAddress> TAKEN_LOCAL_ADDRESSES = new HashSet<VmPipeAddress>();

    private static int nextLocalPort = -1;

    private static final IoFutureListener LOCAL_ADDRESS_RECLAIMER = new LocalAddressReclaimer();

    private static VmPipeAddress nextLocalAddress() throws IOException {
        synchronized (TAKEN_LOCAL_ADDRESSES) {
            if (nextLocalPort >= 0) {
                nextLocalPort = -1;
            }
            for (int i = 0; i < Integer.MAX_VALUE; i++) {
                VmPipeAddress answer = new VmPipeAddress(nextLocalPort--);
                if (!TAKEN_LOCAL_ADDRESSES.contains(answer)) {
                    TAKEN_LOCAL_ADDRESSES.add(answer);
                    return answer;
                }
            }
        }

        throw new IOException("Can't assign a local VM pipe port.");
    }

    private static class LocalAddressReclaimer implements IoFutureListener {
        public void operationComplete(IoFuture future) {
            synchronized (TAKEN_LOCAL_ADDRESSES) {
                TAKEN_LOCAL_ADDRESSES.remove(future.getSession()
                        .getLocalAddress());
            }
        }
    }
}