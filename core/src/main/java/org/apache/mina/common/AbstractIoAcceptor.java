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


/**
 * A base implementation of {@link IoAcceptor}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractIoAcceptor extends AbstractIoService implements
        IoAcceptor {
    private SocketAddress localAddress;

    private boolean disconnectOnUnbind = true;

    private boolean bound;

    /**
     * The lock object which is acquired while bind or unbind operation is performed.
     * Acquire this lock in your property setters which shouldn't be changed while
     * the service is bound.
     */
    protected final Object bindLock = new Object();

    protected AbstractIoAcceptor(IoSessionConfig sessionConfig) {
        super(sessionConfig);
    }

    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(SocketAddress localAddress) {
        if (localAddress != null
                && !getTransportMetadata().getAddressType().isAssignableFrom(
                        localAddress.getClass())) {
            throw new IllegalArgumentException("localAddress type: "
                    + localAddress.getClass() + " (expected: "
                    + getTransportMetadata().getAddressType() + ")");
        }

        synchronized (bindLock) {
            if (bound) {
                throw new IllegalStateException(
                        "localAddress can't be set while the acceptor is bound.");
            }

            this.localAddress = localAddress;
        }
    }

    public boolean isDisconnectOnUnbind() {
        return disconnectOnUnbind;
    }

    public void setDisconnectOnUnbind(boolean disconnectClientsOnUnbind) {
        this.disconnectOnUnbind = disconnectClientsOnUnbind;
    }

    public final void bind() throws IOException {
        synchronized (bindLock) {
            if (bound) {
                throw new IllegalStateException("Already bound to: "
                        + getLocalAddress());
            }

            if (getHandler() == null) {
                throw new IllegalStateException("handler is not set.");
            }

            try {
                doBind();
            } catch (IOException e) {
                throw e;
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeIoException(
                        "Failed to bind to: " + getLocalAddress(), e);
            }
            bound = true;
        }
        getListeners().fireServiceActivated();
    }

    public final void unbind() {
        synchronized (bindLock) {
            if (!bound) {
                return;
            }

            try {
                doUnbind();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeIoException(
                        "Failed to unbind from: " + getLocalAddress(), e);
            }
            bound = false;
        }

        getListeners().fireServiceDeactivated();
    }

    @Override
    public boolean isActive() {
        synchronized (bindLock) {
            return bound;
        }
    }
    
    @Override
    protected void doDispose() throws Exception {
        unbind();
    }

    /**
     * Implement this method to perform the actual bind operation.
     */
    protected abstract void doBind() throws Exception;

    /**
     * Implement this method to perform the actual unbind operation.
     */
    protected abstract void doUnbind() throws Exception;
}
