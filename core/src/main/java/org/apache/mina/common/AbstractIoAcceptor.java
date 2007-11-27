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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * A base implementation of {@link IoAcceptor}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractIoAcceptor 
        extends AbstractIoService implements IoAcceptor {
    
    private final List<SocketAddress> localAddresses = new ArrayList<SocketAddress>();
    private final List<SocketAddress> unmodifiableLocalAddresses =
        Collections.unmodifiableList(localAddresses);
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
        if (localAddresses.isEmpty()) {
            return null;
        }
        return localAddresses.iterator().next();
    }

    public final void setLocalAddress(SocketAddress localAddress) {
        setLocalAddresses(new SocketAddress[] { localAddress });
    }

    public final List<SocketAddress> getLocalAddresses() {
        return unmodifiableLocalAddresses;
    }

    public final void setLocalAddresses(Iterable<SocketAddress> localAddresses) {
        if (localAddresses == null) {
            throw new NullPointerException("localAddresses");
        }
        
        List<SocketAddress> list = new ArrayList<SocketAddress>();
        for (SocketAddress a: localAddresses) {
            list.add(a);
        }
        
        setLocalAddresses(list.toArray(new SocketAddress[list.size()]));
    }

    public final void setLocalAddresses(SocketAddress... localAddresses) {
        if (localAddresses == null) {
            throw new NullPointerException("localAddresses");
        }
        
        synchronized (bindLock) {
            if (bound) {
                throw new IllegalStateException(
                        "localAddress can't be set while the acceptor is bound.");
            }

            Collection<SocketAddress> newLocalAddresses = 
                new ArrayList<SocketAddress>();
            for (SocketAddress a: localAddresses) {
                if (a != null &&
                    !getTransportMetadata().getAddressType().isAssignableFrom(
                                a.getClass())) {
                    throw new IllegalArgumentException("localAddress type: "
                            + a.getClass().getSimpleName() + " (expected: "
                            + getTransportMetadata().getAddressType().getSimpleName() + ")");
                }
                newLocalAddresses.add(a);
            }
            
            if (newLocalAddresses.isEmpty()) {
                throw new IllegalArgumentException("empty localAddresses");
            }
            
            this.localAddresses.clear();
            this.localAddresses.addAll(newLocalAddresses);
        }
    }

    public final boolean isDisconnectOnUnbind() {
        return disconnectOnUnbind;
    }

    public final void setDisconnectOnUnbind(boolean disconnectClientsOnUnbind) {
        this.disconnectOnUnbind = disconnectClientsOnUnbind;
    }

    public final void bind() throws IOException {
        if (isDisposing()) {
            throw new IllegalStateException("Already disposed.");
        }

        synchronized (bindLock) {
            if (bound) {
                throw new IllegalStateException("Already bound to: "
                        + getLocalAddresses());
            }

            if (getHandler() == null) {
                throw new IllegalStateException("handler is not set.");
            }
            
            if (localAddresses.isEmpty()) {
                throw new IllegalStateException(
                        "no local addresses were specified.");
            }

            try {
                bind0();
            } catch (IOException e) {
                throw e;
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeIoException(
                        "Failed to bind to: " + getLocalAddresses(), e);
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
                unbind0();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeIoException(
                        "Failed to unbind from: " + getLocalAddresses(), e);
            }
            bound = false;
        }

        getListeners().fireServiceDeactivated();
    }

    /**
     * Implement this method to perform the actual bind operation.
     */
    protected abstract void bind0() throws Exception;

    /**
     * Implement this method to perform the actual unbind operation.
     */
    protected abstract void unbind0() throws Exception;
    
    @Override
    public String toString() {
        TransportMetadata m = getTransportMetadata();
        return '(' + m.getProviderName() + ' ' + m.getName() + " acceptor: " + 
               (isActive()?
                       "localAddress: " + getLocalAddresses() +
                       ", managedSessionCount: " + getManagedSessionCount() :
                           "not bound") + ')'; 
    }
}
