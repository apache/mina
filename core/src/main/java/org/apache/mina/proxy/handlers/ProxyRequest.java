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
package org.apache.mina.proxy.handlers;

import java.net.InetSocketAddress;

/**
 * ProxyRequest.java - Abstract wrapper class for proxy requests.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public abstract class ProxyRequest {

    /**
     * The address of the request endpoint.
     */
    private InetSocketAddress endpointAddress = null;

    /**
     * Implicit constructor.
     */
    public ProxyRequest() {
        // Do nothing
    }

    /**
     * Basic constructor of a {@link ProxyRequest} that only sets the
     * address of the request endpoint.
     *  
     * @param endpointAddress the address of the request endpoint.
     */
    public ProxyRequest(final InetSocketAddress endpointAddress) {
        this.endpointAddress = endpointAddress;
    }

    /**
     * Returns the address of the request endpoint.
     * 
     * @return the address of the request endpoint
     */
    public InetSocketAddress getEndpointAddress() {
        return endpointAddress;
    }
}