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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.AbstractIoAcceptor;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IdleStatusChecker;
import org.apache.mina.core.session.IoSession;

/**
 * Binds the specified {@link IoHandler} to the specified
 * {@link VmPipeAddress}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public final class VmPipeAcceptor extends AbstractIoAcceptor {
    
    // object used for checking session idle
    private IdleStatusChecker idleChecker;
    
    static final Map<VmPipeAddress, VmPipe> boundHandlers = new HashMap<VmPipeAddress, VmPipe>();

    /**
     * Creates a new instance.
     */
    public VmPipeAcceptor() {
        this(null);
    }
    
    /**
     * Creates a new instance.
     */
    public VmPipeAcceptor(Executor executor) {
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
    public VmPipeAddress getLocalAddress() {
        return (VmPipeAddress) super.getLocalAddress();
    }

    @Override
    public VmPipeAddress getDefaultLocalAddress() {
        return (VmPipeAddress) super.getDefaultLocalAddress();
    }

    // This method is overriden to work around a problem with
    // bean property access mechanism.

    public void setDefaultLocalAddress(VmPipeAddress localAddress) {
        super.setDefaultLocalAddress(localAddress);
    }

    @Override
    protected void dispose0() throws Exception {
        // stop the idle checking task
        idleChecker.getNotifyingTask().cancel();
        unbind();
    }

    @Override
    protected Set<SocketAddress> bindInternal(List<? extends SocketAddress> localAddresses) throws IOException {
        Set<SocketAddress> newLocalAddresses = new HashSet<SocketAddress>();

        synchronized (boundHandlers) {
            for (SocketAddress a: localAddresses) {
                VmPipeAddress localAddress = (VmPipeAddress) a;
                if (localAddress == null || localAddress.getPort() == 0) {
                    localAddress = null;
                    for (int i = 10000; i < Integer.MAX_VALUE; i++) {
                        VmPipeAddress newLocalAddress = new VmPipeAddress(i);
                        if (!boundHandlers.containsKey(newLocalAddress) &&
                            !newLocalAddresses.contains(newLocalAddress)) {
                            localAddress = newLocalAddress;
                            break;
                        }
                    }
    
                    if (localAddress == null) {
                        throw new IOException("No port available.");
                    }
                } else if (localAddress.getPort() < 0) {
                    throw new IOException("Bind port number must be 0 or above.");
                } else if (boundHandlers.containsKey(localAddress)) {
                    throw new IOException("Address already bound: " + localAddress);
                }
                
                newLocalAddresses.add(localAddress);
            }

            for (SocketAddress a: newLocalAddresses) {
                VmPipeAddress localAddress = (VmPipeAddress) a;
                if (!boundHandlers.containsKey(localAddress)) {
                    boundHandlers.put(localAddress, new VmPipe(this, localAddress,
                            getHandler(), getListeners()));
                } else {
                    for (SocketAddress a2: newLocalAddresses) {
                        boundHandlers.remove(a2);
                    }
                    throw new IOException("Duplicate local address: " + a);
                }
            }
        }

        return newLocalAddresses;
    }

    @Override
    protected void unbind0(List<? extends SocketAddress> localAddresses) {
        synchronized (boundHandlers) {
            for (SocketAddress a: localAddresses) {
                boundHandlers.remove(a);
            }
        }
    }

    public IoSession newSession(SocketAddress remoteAddress, SocketAddress localAddress) {
        throw new UnsupportedOperationException();
    }

    void doFinishSessionInitialization(IoSession session, IoFuture future) {
        initSession(session, future, null);
    }
}
