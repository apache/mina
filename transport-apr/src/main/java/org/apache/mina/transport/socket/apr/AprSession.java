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
package org.apache.mina.transport.socket.apr;

import java.net.InetSocketAddress;

import org.apache.mina.common.AbstractIoSession;
import org.apache.mina.common.DefaultIoFilterChain;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoProcessor;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.tomcat.jni.Address;
import org.apache.tomcat.jni.Socket;

/**
 * {@link IoSession} for the {@link AprSocketConnector}
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AprSession extends AbstractIoSession {
    private final long descriptor;

    private final IoService service;
    private final IoProcessor<AprSession> processor;

    private final IoFilterChain filterChain = new DefaultIoFilterChain(this);
    private final IoHandler handler;

    private final InetSocketAddress remoteAddress;
    private final InetSocketAddress localAddress;

    private boolean readable = true;
    private boolean writable = true;
    private boolean interestedInRead;
    private boolean interestedInWrite;

    /**
     * Creates a new instance.
     */
    AprSession(
            IoService service, IoProcessor<AprSession> processor, long descriptor) throws Exception {
        this.service = service;
        this.processor = processor;
        this.handler = service.getHandler();
        this.descriptor = descriptor;

        long ra = Address.get(Socket.APR_REMOTE, descriptor);
        long la = Address.get(Socket.APR_LOCAL, descriptor);

        this.remoteAddress = new InetSocketAddress(Address.getip(ra), Address.getInfo(ra).port);
        this.localAddress = new InetSocketAddress(Address.getip(la), Address.getInfo(la).port);
    }

    AprSession(
            IoService service, IoProcessor<AprSession> processor,
            long descriptor, InetSocketAddress remoteAddress) throws Exception {
        this.service = service;
        this.processor = processor;
        this.handler = service.getHandler();
        this.descriptor = descriptor;

        long la = Address.get(Socket.APR_LOCAL, descriptor);

        this.remoteAddress = remoteAddress;
        this.localAddress = new InetSocketAddress(Address.getip(la), Address.getInfo(la).port);
    }

    long getDescriptor() {
        return descriptor;
    }

    @Override
    protected IoProcessor<AprSession> getProcessor() {
        return processor;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public IoFilterChain getFilterChain() {
        return filterChain;
    }

    public IoHandler getHandler() {
        return handler;
    }

    public IoService getService() {
        return service;
    }

    @Override
    public InetSocketAddress getServiceAddress() {
        return (InetSocketAddress) super.getServiceAddress();
    }

    boolean isReadable() {
        return readable;
    }

    void setReadable(boolean readable) {
        this.readable = readable;
    }

    boolean isWritable() {
        return writable;
    }

    void setWritable(boolean writable) {
        this.writable = writable;
    }

    boolean isInterestedInRead() {
        return interestedInRead;
    }

    void setInterestedInRead(boolean isOpRead) {
        this.interestedInRead = isOpRead;
    }

    boolean isInterestedInWrite() {
        return interestedInWrite;
    }

    void setInterestedInWrite(boolean isOpWrite) {
        this.interestedInWrite = isOpWrite;
    }
}