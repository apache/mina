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

import org.apache.mina.core.filterchain.DefaultIoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.IoSession;
import org.apache.tomcat.jni.Address;
import org.apache.tomcat.jni.Socket;

/**
 * An abstract {@link IoSession} serving of base for APR based sessions.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AprSession extends AbstractIoSession {

    // good old socket descriptor
    private long descriptor;

    // the processor processing this session
    private final IoProcessor<AprSession> processor;

    // the mandatory filter chain of this session
    private final IoFilterChain filterChain = new DefaultIoFilterChain(this);

    // the two endpoint addresses
    private final InetSocketAddress remoteAddress;

    private final InetSocketAddress localAddress;

    // current polling results
    private boolean readable = true;

    private boolean writable = true;

    private boolean interestedInRead;

    private boolean interestedInWrite;

    /**
     * Creates a new instance of {@link AprSession}. Need to be called by extending types
     * @param service the {@link IoService} creating this session. Can be {@link AprSocketAcceptor} or 
     *         {@link AprSocketConnector}
     * @param processor the {@link AprIoProcessor} managing this session.
     * @param descriptor the low level APR socket descriptor for this socket. {@see Socket#create(int, int, int, long)}
     * @throws Exception exception produced during the setting of all the socket parameters. 
     */
    AprSession(IoService service, IoProcessor<AprSession> processor, long descriptor) throws Exception {
        super(service);
        this.processor = processor;
        this.descriptor = descriptor;

        long ra = Address.get(Socket.APR_REMOTE, descriptor);
        long la = Address.get(Socket.APR_LOCAL, descriptor);

        this.remoteAddress = new InetSocketAddress(Address.getip(ra), Address.getInfo(ra).port);
        this.localAddress = new InetSocketAddress(Address.getip(la), Address.getInfo(la).port);
    }

    /**
     * Creates a new instance of {@link AprSession}. Need to be called by extending types. 
     * The constructor add remote address for UDP based sessions. 
     * @param service the {@link IoService} creating this session. Can be {@link AprSocketAcceptor} or 
     *         {@link AprSocketConnector}
     * @param processor the {@link AprIoProcessor} managing this session.
     * @param descriptor the low level APR socket descriptor for this socket. {@see Socket#create(int, int, int, long)}
     * @param remoteAddress the remote end-point
     * @throws Exception exception produced during the setting of all the socket parameters. 
     */
    AprSession(IoService service, IoProcessor<AprSession> processor, long descriptor, InetSocketAddress remoteAddress)
            throws Exception {
        super(service);
        this.processor = processor;
        this.descriptor = descriptor;

        long la = Address.get(Socket.APR_LOCAL, descriptor);

        this.remoteAddress = remoteAddress;
        this.localAddress = new InetSocketAddress(Address.getip(la), Address.getInfo(la).port);
    }

    /**
     * Get the socket descriptor {@see Socket#create(int, int, int, long)}.
     * @return the low level APR socket descriptor
     */
    long getDescriptor() {
        return descriptor;
    }

    /**
     * Set the socket descriptor.
     * @param desc the low level APR socket descriptor created by {@see Socket#create(int, int, int, long)}
     */
    void setDescriptor(long desc) {
        this.descriptor = desc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoProcessor<AprSession> getProcessor() {
        return processor;
    }

    /**
     * {@inheritDoc}
     */
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    /**
     * {@inheritDoc}
     */
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * {@inheritDoc}
     */
    public IoFilterChain getFilterChain() {
        return filterChain;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InetSocketAddress getServiceAddress() {
        return (InetSocketAddress) super.getServiceAddress();
    }

    /**
     * Is this session was tagged are readable after a call to {@link Socket#pool(long)}.
     * @return true if this session is ready for read operations
     */
    boolean isReadable() {
        return readable;
    }

    /**
     * Set if this session is readable after a call to {@link Socket#pool(long)}.
     * @param readable  true for set this session ready for read operations
     */
    void setReadable(boolean readable) {
        this.readable = readable;
    }

    /**
     * Is this session is tagged writable after a call to {@link Socket#pool(long)}.
     * @return true if this session is ready for write operations
     */
    boolean isWritable() {
        return writable;
    }

    /**
     * Set if this session is writable after a call to {@link Socket#pool(long)}.
     * @param writable true for set this session ready for write operations
     */
    void setWritable(boolean writable) {
        this.writable = writable;
    }

    /**
     * Does this session needs to be registered for read events.
     * Used for building poll set {@see Poll}. 
     * @return true if registered
     */
    boolean isInterestedInRead() {
        return interestedInRead;
    }

    /**
     * Set if this session needs to be registered for read events. 
     * Used for building poll set {@see Poll}.
     * @param isOpRead true if need to be registered
     */
    void setInterestedInRead(boolean isOpRead) {
        this.interestedInRead = isOpRead;
    }

    /**
     * Does this session needs to be registered for write events.
     * Used for building poll set {@see Poll}. 
     * @return true if registered
     */
    boolean isInterestedInWrite() {
        return interestedInWrite;
    }

    /**
     * Set if this session needs to be registered for write events.
     * Used for building poll set {@see Poll}.
     * @param isOpWrite true if need to be registered
     */
    void setInterestedInWrite(boolean isOpWrite) {
        this.interestedInWrite = isOpWrite;
    }
}