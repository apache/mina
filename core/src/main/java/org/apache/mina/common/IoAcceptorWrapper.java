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
package org.apache.mina.common;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Set;


/**
 * An {@link IoAcceptor} that wraps the other {@link IoAcceptor}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class IoAcceptorWrapper implements IoAcceptor {
    protected IoAcceptor acceptor;

    /**
     * Creates a new instance.
     */
    protected IoAcceptorWrapper() {
    }

    /**
     * Sets the acceptor to be wrapped.  This method should be invoked before any operations
     * is requested.
     */
    protected void init(IoAcceptor acceptor) {
        this.acceptor = acceptor;
    }

    public IoFilterChainBuilder getFilterChainBuilder() {
        return acceptor.getFilterChainBuilder();
    }

    public void setFilterChainBuilder(IoFilterChainBuilder builder) {
        acceptor.setFilterChainBuilder(builder);
    }

    public DefaultIoFilterChainBuilder getFilterChain() {
        return acceptor.getFilterChain();
    }

    public void addListener(IoServiceListener listener) {
        acceptor.addListener(listener);
    }

    public void removeListener(IoServiceListener listener) {
        acceptor.removeListener(listener);
    }

    public void bind() throws IOException {
        acceptor.bind();
    }

    public SocketAddress getLocalAddress() {
        return acceptor.getLocalAddress();
    }

    public boolean isDisconnectOnUnbind() {
        return acceptor.isDisconnectOnUnbind();
    }

    public IoSession newSession(SocketAddress remoteAddress) {
        return acceptor.newSession(remoteAddress);
    }

    public void setDisconnectOnUnbind(boolean disconnectOnUnbind) {
        acceptor.setDisconnectOnUnbind(disconnectOnUnbind);
    }

    public void setLocalAddress(SocketAddress localAddress) {
        acceptor.setLocalAddress(localAddress);
    }

    public void unbind() {
        acceptor.unbind();
    }

    public IoHandler getHandler() {
        return acceptor.getHandler();
    }

    public Set<IoSession> getManagedSessions() {
        return acceptor.getManagedSessions();
    }

    public IoSessionConfig getSessionConfig() {
        return acceptor.getSessionConfig();
    }

    public void setHandler(IoHandler handler) {
        acceptor.setHandler(handler);
    }

    public boolean isBound() {
        return acceptor.isBound();
    }

    public TransportMetadata getTransportMetadata() {
        return acceptor.getTransportMetadata();
    }
}
