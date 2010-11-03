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
package org.apache.mina.core.service;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;


/**
 * A base implementation of {@link IoAcceptor}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @org.apache.xbean.XBean
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

    /**
     * Constructor for {@link AbstractIoAcceptor}. You need to provide a default
     * session configuration and an {@link Executor} for handling I/O events. If
     * null {@link Executor} is provided, a default one will be created using
     * {@link Executors#newCachedThreadPool()}.
     *
     * {@see AbstractIoService#AbstractIoService(IoSessionConfig, Executor)}
     * 
     * @param sessionConfig
     *            the default configuration for the managed {@link IoSession}
     * @param executor
     *            the {@link Executor} used for handling execution of I/O
     *            events. Can be <code>null</code>.
     */
    protected AbstractIoAcceptor(IoSessionConfig sessionConfig, Executor executor) {
        super(sessionConfig, executor);
        defaultLocalAddresses.add(null);
    }

    /**
     * {@inheritDoc}
     */
    public SocketAddress getLocalAddress() {
        Set<SocketAddress> localAddresses = getLocalAddresses();
        if (localAddresses.isEmpty()) {
            return null;
        }

        return localAddresses.iterator().next();
    }

    /**
     * {@inheritDoc}
     */
    public final Set<SocketAddress> getLocalAddresses() {
        Set<SocketAddress> localAddresses = new HashSet<SocketAddress>();
        
        synchronized (boundAddresses){
            localAddresses.addAll(boundAddresses);
        }
        
        return localAddresses;
    }

    /**
     * {@inheritDoc}
     */
    public SocketAddress getDefaultLocalAddress() {
        if (defaultLocalAddresses.isEmpty()) {
            return null;
        }
        return defaultLocalAddresses.iterator().next();
    }

    /**
     * {@inheritDoc}
     */
    public final void setDefaultLocalAddress(SocketAddress localAddress) {
        setDefaultLocalAddresses(localAddress);
    }

    /**
     * {@inheritDoc}
     */
    public final List<SocketAddress> getDefaultLocalAddresses() {
        return unmodifiableDefaultLocalAddresses;
    }

    /**
     * {@inheritDoc}
     * @org.apache.xbean.Property nestedType="java.net.SocketAddress"
     */
    public final void setDefaultLocalAddresses(List<? extends SocketAddress> localAddresses) {
        if (localAddresses == null) {
            throw new IllegalArgumentException("localAddresses");
        }
        setDefaultLocalAddresses((Iterable<? extends SocketAddress>) localAddresses);
    }

    /**
     * {@inheritDoc}
     */
    public final void setDefaultLocalAddresses(Iterable<? extends SocketAddress> localAddresses) {
        if (localAddresses == null) {
            throw new IllegalArgumentException("localAddresses");
        }
        
        synchronized (bindLock) {
            synchronized (boundAddresses) {
                if (!boundAddresses.isEmpty()) {
                    throw new IllegalStateException(
                            "localAddress can't be set while the acceptor is bound." );
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
                this.defaultLocalAddresses.addAll( newLocalAddresses );
            }
        }
    }

    /**
     * {@inheritDoc}
     * @org.apache.xbean.Property nestedType="java.net.SocketAddress"
     */
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

    /**
     * {@inheritDoc}
     */
    public final boolean isCloseOnDeactivation() {
        return disconnectOnUnbind;
    }

    /**
     * {@inheritDoc}
     */
    public final void setCloseOnDeactivation(boolean disconnectClientsOnUnbind) {
        this.disconnectOnUnbind = disconnectClientsOnUnbind;
    }

    /**
     * {@inheritDoc}
     */
    public final void bind() throws IOException {
        bind(getDefaultLocalAddresses());
    }

    /**
     * {@inheritDoc}
     */
    public final void bind(SocketAddress localAddress) throws IOException {
        if (localAddress == null) {
            throw new IllegalArgumentException("localAddress");
        }
        
        List<SocketAddress> localAddresses = new ArrayList<SocketAddress>(1);
        localAddresses.add(localAddress);
        bind(localAddresses);
    }


    /**
     * {@inheritDoc}
     */
    public final void bind(SocketAddress firstLocalAddress, SocketAddress... otherLocalAddresses) throws IOException {
        if (firstLocalAddress == null) {
            bind(getDefaultLocalAddresses());
            return;
        }
        
        List<SocketAddress> localAddresses = new ArrayList<SocketAddress>(2);
        localAddresses.add(firstLocalAddress);

        if (otherLocalAddresses != null) {
            for (SocketAddress address:otherLocalAddresses) {
                localAddresses.add(address);
            }
        }

        bind(localAddresses);
    }

    /**
     * {@inheritDoc}
     */
    public final void bind(Iterable<? extends SocketAddress> localAddresses) throws IOException {
        if (isDisposing()) {
            throw new IllegalStateException("Already disposed.");
        }
        
        if (localAddresses == null) {
            throw new IllegalArgumentException("localAddresses");
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
            synchronized (boundAddresses) {
                if (boundAddresses.isEmpty()) {
                    activate = true;
                }
            }

            if (getHandler() == null) {
                throw new IllegalStateException("handler is not set.");
            }
            
            try {
                Set<SocketAddress> addresses = bindInternal( localAddressesCopy );
                
                synchronized (boundAddresses) {
                    boundAddresses.addAll(addresses);
                }
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

    /**
     * {@inheritDoc}
     */
    public final void unbind() {
        unbind(getLocalAddresses());
    }

    /**
     * {@inheritDoc}
     */
    public final void unbind(SocketAddress localAddress) {
        if (localAddress == null) {
            throw new IllegalArgumentException("localAddress");
        }
        
        List<SocketAddress> localAddresses = new ArrayList<SocketAddress>(1);
        localAddresses.add(localAddress);
        unbind(localAddresses);
    }

    /**
     * {@inheritDoc}
     */
    public final void unbind(SocketAddress firstLocalAddress,
            SocketAddress... otherLocalAddresses) {
        if (firstLocalAddress == null) {
            throw new IllegalArgumentException("firstLocalAddress");
        }
        if (otherLocalAddresses == null) {
            throw new IllegalArgumentException("otherLocalAddresses");
        }
        
        List<SocketAddress> localAddresses = new ArrayList<SocketAddress>();
        localAddresses.add(firstLocalAddress);
        Collections.addAll(localAddresses, otherLocalAddresses);
        unbind(localAddresses);
    }

    /**
     * {@inheritDoc}
     */
    public final void unbind(Iterable<? extends SocketAddress> localAddresses) {
        if (localAddresses == null) {
            throw new IllegalArgumentException("localAddresses");
        }
        
        boolean deactivate = false;
        synchronized (bindLock) {
            synchronized (boundAddresses) {
                if (boundAddresses.isEmpty()) {
                    return;
                }

                List<SocketAddress> localAddressesCopy = new ArrayList<SocketAddress>();
                int specifiedAddressCount = 0;
                
                for (SocketAddress a: localAddresses ) {
                    specifiedAddressCount++;

                    if ((a != null) && boundAddresses.contains(a) ) {
                        localAddressesCopy.add(a);
                    }
                }
                
                if (specifiedAddressCount == 0) {
                    throw new IllegalArgumentException( "localAddresses is empty." );
                }
                
                if (!localAddressesCopy.isEmpty()) {
                    try {
                        unbind0(localAddressesCopy);
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new RuntimeIoException(
                                "Failed to unbind from: " + getLocalAddresses(), e );
                    }

                    boundAddresses.removeAll(localAddressesCopy);
                    
                    if (boundAddresses.isEmpty()) {
                        deactivate = true;
                    }
                }
            }
        }

        if (deactivate) {
            getListeners().fireServiceDeactivated();
        }
    }

    /**
     * Starts the acceptor, and register the given addresses
     * @return the {@link Set} of the local addresses which is bound actually
     */
    protected abstract Set<SocketAddress> bindInternal(
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
    
    public static class AcceptorOperationFuture extends ServiceOperationFuture {
        private final List<SocketAddress> localAddresses;
        
        public AcceptorOperationFuture(List<? extends SocketAddress> localAddresses) {
            this.localAddresses = new ArrayList<SocketAddress>(localAddresses);
        }
        
        public final List<SocketAddress> getLocalAddresses() {
            return Collections.unmodifiableList(localAddresses);
        }
        
        /**
         * @see Object#toString()
         */
        public String toString() {
            StringBuilder sb = new StringBuilder();
            
            sb.append( "Acceptor operation : " );
            
            if (localAddresses != null) {
                boolean isFirst = true;
                
                for (SocketAddress address:localAddresses) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        sb.append(", ");
                    }
                    
                    sb.append(address);
                }
            }
            return sb.toString(); 
        }
    }
}
