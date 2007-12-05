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
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * A base implementation of {@link IoAcceptor}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractIoAcceptor 
        extends AbstractIoService implements IoAcceptor {
    
    private final List<SocketAddress> defaultLocalAddresses =
        new ArrayList<SocketAddress>();
    private final List<SocketAddress> unmodifiableDefaultLocalAddresses =
        Collections.unmodifiableList(defaultLocalAddresses);
    private final Set<SocketAddress> boundAddresses =
        new HashSet<SocketAddress>();

    private boolean disconnectOnUnbind = true;

    /**
     * The lock object which is acquired while bind or unbind operation is performed.
     * Acquire this lock in your property setters which shouldn't be changed while
     * the service is bound.
     */
    protected final Object bindLock = new Object();

    protected AbstractIoAcceptor(IoSessionConfig sessionConfig) {
        super(sessionConfig);
        defaultLocalAddresses.add(null);
    }

    public SocketAddress getLocalAddress() {
        Set<SocketAddress> localAddresses = getLocalAddresses();
        if (localAddresses.isEmpty()) {
            return null;
        } else {
            return localAddresses.iterator().next();
        }
    }

    public final Set<SocketAddress> getLocalAddresses() {
        Set<SocketAddress> localAddresses = new HashSet<SocketAddress>();
        synchronized (bindLock) {
            localAddresses.addAll(boundAddresses);
        }
        return localAddresses;
    }

    public SocketAddress getDefaultLocalAddress() {
        if (defaultLocalAddresses.isEmpty()) {
            return null;
        }
        return defaultLocalAddresses.iterator().next();
    }

    public final void setDefaultLocalAddress(SocketAddress localAddress) {
        setDefaultLocalAddresses(localAddress);
    }

    public final List<SocketAddress> getDefaultLocalAddresses() {
        return unmodifiableDefaultLocalAddresses;
    }

    public final void setDefaultLocalAddresses(List<? extends SocketAddress> localAddresses) {
        if (localAddresses == null) {
            throw new NullPointerException("localAddresses");
        }
        setDefaultLocalAddresses((Iterable<? extends SocketAddress>) localAddresses);
    }

    public final void setDefaultLocalAddresses(Iterable<? extends SocketAddress> localAddresses) {
        if (localAddresses == null) {
            throw new NullPointerException("localAddresses");
        }
        
        synchronized (bindLock) {
            if (!boundAddresses.isEmpty()) {
                throw new IllegalStateException(
                        "localAddress can't be set while the acceptor is bound.");
            }

            Collection<SocketAddress> newLocalAddresses = 
                new ArrayList<SocketAddress>();
            for (SocketAddress a: localAddresses) {
                checkAddressType(a);
                newLocalAddresses.add(a);
            }
            
            if (newLocalAddresses.isEmpty()) {
                throw new IllegalArgumentException("empty localAddresses");
            }
            
            this.defaultLocalAddresses.clear();
            this.defaultLocalAddresses.addAll(newLocalAddresses);
        }
    }

    public final void setDefaultLocalAddresses(SocketAddress firstLocalAddress, SocketAddress... otherLocalAddresses) {
        if (otherLocalAddresses == null) {
            otherLocalAddresses = new SocketAddress[0];
        }
        
        Collection<SocketAddress> newLocalAddresses =
            new ArrayList<SocketAddress>(otherLocalAddresses.length + 1);
        
        newLocalAddresses.add(firstLocalAddress);
        for (SocketAddress a: otherLocalAddresses) {
            newLocalAddresses.add(a);
        }
        
        setDefaultLocalAddresses(newLocalAddresses);
    }

    public final boolean isCloseOnDeactivation() {
        return disconnectOnUnbind;
    }

    public final void setCloseOnDeactivation(boolean disconnectClientsOnUnbind) {
        this.disconnectOnUnbind = disconnectClientsOnUnbind;
    }

    public final void bind() throws IOException {
        bind(getDefaultLocalAddresses());
    }

    public final void bind(SocketAddress localAddress) throws IOException {
        if (localAddress == null) {
            throw new NullPointerException("localAddress");
        }
        
        List<SocketAddress> localAddresses = new ArrayList<SocketAddress>(1);
        localAddresses.add(localAddress);
        bind(localAddresses);
    }

    public final void bind(
            SocketAddress firstLocalAddress,
            SocketAddress... otherLocalAddresses) throws IOException {
        if (firstLocalAddress == null) {
            throw new NullPointerException("firstLocalAddress");
        }
        if (otherLocalAddresses == null) {
            throw new NullPointerException("otherLocalAddresses");
        }
        
        List<SocketAddress> localAddresses = new ArrayList<SocketAddress>();
        localAddresses.add(firstLocalAddress);
        Collections.addAll(localAddresses, otherLocalAddresses);
        bind(localAddresses);
    }

    public final void bind(Iterable<? extends SocketAddress> localAddresses) throws IOException {
        if (isDisposing()) {
            throw new IllegalStateException("Already disposed.");
        }
        if (localAddresses == null) {
            throw new NullPointerException("localAddresses");
        }
        
        List<SocketAddress> localAddressesCopy = new ArrayList<SocketAddress>();
        for (SocketAddress a: localAddresses) {
            checkAddressType(a);
            localAddressesCopy.add(a);
        }
        if (localAddressesCopy.isEmpty()) {
            throw new IllegalArgumentException("localAddresses is empty.");
        }
        
        boolean activate = false;
        synchronized (bindLock) {
            if (boundAddresses.isEmpty()) {
                activate = true;
            }

            if (getHandler() == null) {
                throw new IllegalStateException("handler is not set.");
            }
            
            try {
                boundAddresses.addAll(bind0(localAddressesCopy));
            } catch (IOException e) {
                throw e;
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeIoException(
                        "Failed to bind to: " + getLocalAddresses(), e);
            }
        }
        
        if (activate) {
            getListeners().fireServiceActivated();
        }
    }

    public final void unbind() {
        unbind(getLocalAddresses());
    }

    public final void unbind(SocketAddress localAddress) {
        if (localAddress == null) {
            throw new NullPointerException("localAddress");
        }
        
        List<SocketAddress> localAddresses = new ArrayList<SocketAddress>(1);
        localAddresses.add(localAddress);
        unbind(localAddresses);
    }

    public final void unbind(SocketAddress firstLocalAddress,
            SocketAddress... otherLocalAddresses) {
        if (firstLocalAddress == null) {
            throw new NullPointerException("firstLocalAddress");
        }
        if (otherLocalAddresses == null) {
            throw new NullPointerException("otherLocalAddresses");
        }
        
        List<SocketAddress> localAddresses = new ArrayList<SocketAddress>();
        localAddresses.add(firstLocalAddress);
        Collections.addAll(localAddresses, otherLocalAddresses);
        unbind(localAddresses);
    }

    public final void unbind(Iterable<? extends SocketAddress> localAddresses) {
        if (localAddresses == null) {
            throw new NullPointerException("localAddresses");
        }
        
        boolean deactivate = false;
        synchronized (bindLock) {
            if (boundAddresses.isEmpty()) {
                return;
            }

            List<SocketAddress> localAddressesCopy = new ArrayList<SocketAddress>();
            int specifiedAddressCount = 0;
            for (SocketAddress a: localAddresses) {
                specifiedAddressCount ++;
                if (a != null && boundAddresses.contains(a)) {
                    localAddressesCopy.add(a);
                }
            }
            if (specifiedAddressCount == 0) {
                throw new IllegalArgumentException("localAddresses is empty.");
            }
            
            if (!localAddressesCopy.isEmpty()) {
                try {
                    unbind0(localAddressesCopy);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeIoException(
                            "Failed to unbind from: " + getLocalAddresses(), e);
                }
                
                boundAddresses.removeAll(boundAddresses);
                if (boundAddresses.isEmpty()) {
                    deactivate = true;
                }
            }
        }

        if (deactivate) {
            getListeners().fireServiceDeactivated();
        }
    }

    /**
     * Implement this method to perform the actual bind operation.
     * @return the {@link Set} of the local addresses which is bound actually
     */
    protected abstract Set<SocketAddress> bind0(
            List<? extends SocketAddress> localAddresses) throws Exception;

    /**
     * Implement this method to perform the actual unbind operation.
     */
    protected abstract void unbind0(
            List<? extends SocketAddress> localAddresses) throws Exception;
    
    @Override
    public String toString() {
        TransportMetadata m = getTransportMetadata();
        return '(' + m.getProviderName() + ' ' + m.getName() + " acceptor: " + 
               (isActive()?
                       "localAddress(es): " + getLocalAddresses() +
                       ", managedSessionCount: " + getManagedSessionCount() :
                           "not bound") + ')'; 
    }

    private void checkAddressType(SocketAddress a) {
        if (a != null &&
            !getTransportMetadata().getAddressType().isAssignableFrom(
                        a.getClass())) {
            throw new IllegalArgumentException("localAddress type: "
                    + a.getClass().getSimpleName() + " (expected: "
                    + getTransportMetadata().getAddressType().getSimpleName() + ")");
        }
    }
    
    protected static class AcceptorOperationFuture extends ServiceOperationFuture {
        private final List<SocketAddress> localAddresses;
        
        public AcceptorOperationFuture(List<? extends SocketAddress> localAddresses) {
            this.localAddresses = new ArrayList<SocketAddress>(localAddresses);
        }
        
        public final List<SocketAddress> getLocalAddresses() {
            return Collections.unmodifiableList(localAddresses);
        }
    }
}
