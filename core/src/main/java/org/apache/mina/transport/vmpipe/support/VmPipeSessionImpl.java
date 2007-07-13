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
package org.apache.mina.transport.vmpipe.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.WriteRequest;
import org.apache.mina.common.support.BaseIoSession;
import org.apache.mina.common.support.IoServiceListenerSupport;
import org.apache.mina.transport.vmpipe.DefaultVmPipeSessionConfig;
import org.apache.mina.transport.vmpipe.VmPipeAddress;
import org.apache.mina.transport.vmpipe.VmPipeSession;
import org.apache.mina.transport.vmpipe.VmPipeSessionConfig;

/**
 * A {@link IoSession} for in-VM transport (VM_PIPE).
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class VmPipeSessionImpl extends BaseIoSession implements VmPipeSession {
    private static final VmPipeSessionConfig CONFIG = new DefaultVmPipeSessionConfig();

    private final IoService service;

    private final IoServiceListenerSupport serviceListeners;

    private final VmPipeAddress localAddress;

    private final VmPipeAddress remoteAddress;

    private final VmPipeAddress serviceAddress;

    private final IoHandler handler;

    private final VmPipeFilterChain filterChain;

    private final VmPipeSessionImpl remoteSession;

    final Object lock;

    final BlockingQueue<Object> pendingDataQueue;

    /**
     * Constructor for client-side session.
     */
    public VmPipeSessionImpl(IoService service,
            IoServiceListenerSupport serviceListeners,
            VmPipeAddress localAddress, IoHandler handler, VmPipe remoteEntry) {
        this.service = service;
        this.serviceListeners = serviceListeners;
        this.lock = new Object();
        this.localAddress = localAddress;
        this.remoteAddress = this.serviceAddress = remoteEntry.getAddress();
        this.handler = handler;
        this.filterChain = new VmPipeFilterChain(this);
        this.pendingDataQueue = new LinkedBlockingQueue<Object>();

        remoteSession = new VmPipeSessionImpl(this, remoteEntry);
    }

    /**
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
        this.pendingDataQueue = new LinkedBlockingQueue<Object>();
    }

    public IoService getService() {
        return service;
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

    @Override
    protected void close0() {
        filterChain.fireFilterClose(this);
    }

    @Override
    protected void write0(WriteRequest writeRequest) {
        this.filterChain.fireFilterWrite(this, writeRequest);
    }

    public int getScheduledWriteMessages() {
        return 0;
    }

    public int getScheduledWriteBytes() {
        return 0;
    }

    public TransportType getTransportType() {
        return TransportType.VM_PIPE;
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
    protected void updateTrafficMask() {
        if (getTrafficMask().isReadable() || getTrafficMask().isWritable()) {
            List<Object> data = new ArrayList<Object>();

            pendingDataQueue.drainTo(data);

            for (Object aData : data) {
                if (aData instanceof WriteRequest) {
                    // TODO Optimize unefficient data transfer.
                    // Data will be returned to pendingDataQueue
                    // if getTraffic().isWritable() is false.
                    WriteRequest wr = (WriteRequest) aData;
                    filterChain.doWrite(this, wr);
                } else {
                    // TODO Optimize unefficient data transfer.
                    // Data will be returned to pendingDataQueue
                    // if getTraffic().isReadable() is false.
                    filterChain.fireMessageReceived(this, aData);
                }
            }
        }
    }
}
