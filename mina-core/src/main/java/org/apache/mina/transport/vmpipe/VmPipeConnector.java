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
import java.util.concurrent.Executor;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.DefaultConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.AbstractIoConnector;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IdleStatusChecker;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.util.ExceptionMonitor;

/**
 * Connects to {@link IoHandler}s which is bound on the specified
 * {@link VmPipeAddress}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public final class VmPipeConnector extends AbstractIoConnector {

    // object used for checking session idle
    private IdleStatusChecker idleChecker;
    
    /**
     * Creates a new instance.
     */
    public VmPipeConnector() {
        this(null);
    }
    
    /**
     * Creates a new instance.
     */
    public VmPipeConnector(Executor executor) {
        super(new DefaultVmPipeSessionConfig(), executor);
        idleChecker = new IdleStatusChecker();
        // we schedule the idle status checking task in this service exceutor
        // it will be woke up every seconds
        executeWorker(idleChecker.getNotifyingTask(), "idleStatusChecker");
    }

    public TransportMetadata getTransportMetadata() {
        return VmPipeSession.METADATA;
    }

    @Override
    public VmPipeSessionConfig getSessionConfig() {
        return (VmPipeSessionConfig) super.getSessionConfig();
    }

    @Override
    protected ConnectFuture connect0(SocketAddress remoteAddress,
                                      SocketAddress localAddress,
                                      IoSessionInitializer<? extends ConnectFuture> sessionInitializer) {
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

        VmPipeSession localSession = new VmPipeSession(this,
                getListeners(), actualLocalAddress, getHandler(), entry);

        initSession(localSession, future, sessionInitializer);

        // and reclaim the local address when the connection is closed.
        localSession.getCloseFuture().addListener(LOCAL_ADDRESS_RECLAIMER);

        // initialize connector session
        try {
            IoFilterChain filterChain = localSession.getFilterChain();
            this.getFilterChainBuilder().buildFilterChain(filterChain);

            // The following sentences don't throw any exceptions.
            getListeners().fireSessionCreated(localSession);
            idleChecker.addSession(localSession);
        } catch (Throwable t) {
            future.setException(t);
            return future;
        }

        // initialize acceptor session
        VmPipeSession remoteSession = localSession.getRemoteSession();
        ((VmPipeAcceptor) remoteSession.getService()).doFinishSessionInitialization(remoteSession, null);
        try {
            IoFilterChain filterChain = remoteSession.getFilterChain();
            entry.getAcceptor().getFilterChainBuilder().buildFilterChain(
                    filterChain);

            // The following sentences don't throw any exceptions.
            entry.getListeners().fireSessionCreated(remoteSession);
            idleChecker.addSession(remoteSession);
        } catch (Throwable t) {
            ExceptionMonitor.getInstance().exceptionCaught(t);
            remoteSession.close(true);
        }

        // Start chains, and then allow and messages read/written to be processed. This is to ensure that
        // sessionOpened gets received before a messageReceived
        ((VmPipeFilterChain) localSession.getFilterChain()).start();
        ((VmPipeFilterChain) remoteSession.getFilterChain()).start();

        return future;
    }

    @Override
    protected void dispose0() throws Exception {
        // stop the idle checking task
        idleChecker.getNotifyingTask().cancel();
    }

    private static final Set<VmPipeAddress> TAKEN_LOCAL_ADDRESSES = new HashSet<VmPipeAddress>();

    private static int nextLocalPort = -1;

    private static final IoFutureListener<IoFuture> LOCAL_ADDRESS_RECLAIMER = new LocalAddressReclaimer();

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

    private static class LocalAddressReclaimer implements IoFutureListener<IoFuture> {
        public void operationComplete(IoFuture future) {
            synchronized (TAKEN_LOCAL_ADDRESSES) {
                TAKEN_LOCAL_ADDRESSES.remove(future.getSession()
                        .getLocalAddress());
            }
        }
    }
}