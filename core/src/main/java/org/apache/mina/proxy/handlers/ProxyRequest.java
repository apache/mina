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
 * ProxyRequest.java - Wrapper class for proxy requests.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * @since MINA 2.0.0-M3
 */
public abstract class ProxyRequest {

    private InetSocketAddress endpointAddress = null;

    public ProxyRequest() {
    }

    public ProxyRequest(final InetSocketAddress endpointAddress) {
        this.endpointAddress = endpointAddress;
    }

    /**
     * The request endpoint.
     */
    public InetSocketAddress getEndpointAddress() {
        return endpointAddress;
    }

    /**
     * Sets the request endpoint.
     */
    public void setEndpointAddress(InetSocketAddress endpointAddress) {
        this.endpointAddress = endpointAddress;
    }
}