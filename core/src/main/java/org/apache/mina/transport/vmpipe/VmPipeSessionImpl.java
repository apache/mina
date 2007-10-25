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

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.mina.common.AbstractIoSession;
import org.apache.mina.common.DefaultTransportMetadata;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoProcessor;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceListenerSupport;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.TransportMetadata;
import org.apache.mina.common.WriteRequest;

/**
 * A {@link IoSession} for in-VM transport (VM_PIPE).
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
class VmPipeSessionImpl extends AbstractIoSession implements VmPipeSession {

    static final TransportMetadata METADATA =
            new DefaultTransportMetadata(
                    "vmpipe", false, false,
                    VmPipeAddress.class,
                    VmPipeSessionConfig.class,
                    Object.class);

    private static final VmPipeSessionConfig CONFIG = new DefaultVmPipeSessionConfig();

    private final IoService service;

    private final IoServiceListenerSupport serviceListeners;

    private final VmPipeAddress localAddress;

    private final VmPipeAddress remoteAddress;

    private final VmPipeAddress serviceAddress;

    private final IoHandler handler;

    private final VmPipeFilterChain filterChain;

    private final VmPipeSessionImpl remoteSession;

    private final Lock lock;

    final BlockingQueue<Object> receivedMessageQueue;

    /*
     * Constructor for client-side session.
     */
    VmPipeSessionImpl(IoService service,
                      IoServiceListenerSupport serviceListeners,
                      VmPipeAddress localAddress, IoHandler handler, VmPipe remoteEntry) {
        this.service = service;
        this.serviceListeners = serviceListeners;
        this.lock = new ReentrantLock();
        this.localAddress = localAddress;
        this.remoteAddress = this.serviceAddress = remoteEntry.getAddress();
        this.handler = handler;
        this.filterChain = new VmPipeFilterChain(this);
        this.receivedMessageQueue = new LinkedBlockingQueue<Object>();

        remoteSession = new VmPipeSessionImpl(this, remoteEntry);
    }

    /*
     * Constructor for server-side session.
     */
    private VmPipeSessionImpl(VmPipeSessionImpl remoteSession, VmPipe entry) {
        this.service = entry.getAcceptor();
        this.serviceListeners = entry.getListeners();
        this.lock = remoteSession.lock;
        this.localAddress = this.serviceAddress = remoteSession.remoteAddress;
        this.remoteAddress = remoteSession.localAddress;
        this.handler = entry.getHandler();
        this.filterChain = new VmPipeFilterChain(this);
        this.remoteSession = remoteSession;
        this.receivedMessageQueue = new LinkedBlockingQueue<Object>();
    }

    public IoService getService() {
        return service;
    }

    @Override
    protected IoProcessor getProcessor() {
        return filterChain.getProcessor();
    }

    IoServiceListenerSupport getServiceListeners() {
        return serviceListeners;
    }

    public VmPipeSessionConfig getConfig() {
        return CONFIG;
    }

    public IoFilterChain getFilterChain() {
        return filterChain;
    }

    public VmPipeSessionImpl getRemoteSession() {
        return remoteSession;
    }

    public IoHandler getHandler() {
        return handler;
    }

    public TransportMetadata getTransportMetadata() {
        return METADATA;
    }

    public VmPipeAddress getRemoteAddress() {
        return remoteAddress;
    }

    public VmPipeAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public VmPipeAddress getServiceAddress() {
        return serviceAddress;
    }

    @Override
    protected Queue<WriteRequest> getWriteRequestQueue() {
        return super.getWriteRequestQueue();
    }

    Lock getLock() {
        return lock;
    }
}
