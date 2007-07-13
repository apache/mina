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
package org.apache.mina.integration.spring;

import java.net.SocketAddress;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Defines an address to {@link IoHandler} binding.
 * This is used when specifying the addresses to accept new connections on when
 * creating {@link org.apache.mina.common.IoAcceptor} objects using 
 * {@link IoAcceptorFactoryBean}.
 * <p>
 * Note that the <code>address</code> property is of {@link java.net.SocketAddress}
 * type. Use {@link InetSocketAddressEditor} or {@link VmPipeAddressEditor} in
 * your Spring configuration file to simply the creation of 
 * {@link java.net.SocketAddress} instances using Spring.
 * </p>
 * <p>
 * This class also allows for an optional service configuration using
 * {@link #setServiceConfig(IoServiceConfig)} to be specified. If the binding
 * specifies an {@link IoServiceConfig} {@link IoAcceptorFactoryBean} will
 * use {@link IoAcceptor#bind(SocketAddress, IoHandler, IoServiceConfig)} instead
 * of {@link IoAcceptor#bind(SocketAddress, IoHandler)} when binding. The
 * {@link IoServiceConfig} object lets you specify transport specific
 * confiuration options and define port specific filters. This makes it possible
 * to specify different filters depending on the port the client is connecting
 * on (e.g. using an {@link org.apache.mina.filter.SSLFilter} when connecting
 * on port 443 but not on port 80). 
 * </p>
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class Binding implements InitializingBean {
    private SocketAddress address = null;

    private IoHandler handler = null;

    private IoServiceConfig serviceConfig = null;

    /**
     * Creates a new empty instance.
     */
    public Binding() {
    }

    /**
     * Creates a new instance using the specified values.
     * 
     * @param address the address.
     * @param handler the handler.
     * @throws IllegalArgumentException if the any of the specified values are 
     *         <code>null</code>.
     */
    public Binding(SocketAddress address, IoHandler handler) {
        setAddress(address);
        setHandler(handler);
    }

    /**
     * Creates a new instance using the specified values.
     * 
     * @param address the address.
     * @param handler the handler.
     * @param serviceConfig the service configuration.
     * @throws IllegalArgumentException if the any of the specified values are 
     *         <code>null</code>.
     */
    public Binding(SocketAddress address, IoHandler handler,
            IoServiceConfig serviceConfig) {
        setAddress(address);
        setHandler(handler);
        setServiceConfig(serviceConfig);
    }

    /**
     * Returns the address the handler of this object will be bound to.
     *  
     * @return the address.
     */
    public SocketAddress getAddress() {
        return address;
    }

    /**
     * Sets the address the handler of this object will be bound to.
     * 
     * @param address the address.
     * @throws IllegalArgumentException if the specified value is 
     *         <code>null</code>.
     */
    public void setAddress(SocketAddress address) {
        Assert.notNull(address, "Property 'address' may not be null");
        this.address = address;
    }

    /**
     * Returns the handler of this binding object.
     * 
     * @return the handler.
     */
    public IoHandler getHandler() {
        return handler;
    }

    /**
     * Sets the handler of this binding object.
     *
     * @param handler the handler.
     * @throws IllegalArgumentException if the specified value is 
     *         <code>null</code>.
     */
    public void setHandler(IoHandler handler) {
        Assert.notNull(handler, "Property 'handler' may not be null");
        this.handler = handler;
    }

    public IoServiceConfig getServiceConfig() {
        return serviceConfig;
    }

    public void setServiceConfig(IoServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(address, "Property 'address' may not be null");
        Assert.notNull(handler, "Property 'handler' may not be null");
    }

}
