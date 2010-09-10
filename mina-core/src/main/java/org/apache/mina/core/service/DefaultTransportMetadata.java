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

import java.net.SocketAddress;
import java.util.Collections;
import java.util.Set;

import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.util.IdentityHashSet;


/**
 * A default immutable implementation of {@link TransportMetadata}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultTransportMetadata implements TransportMetadata {

    private final String providerName;
    private final String name;
    private final boolean connectionless;
    private final boolean fragmentation;
    private final Class<? extends SocketAddress> addressType;
    private final Class<? extends IoSessionConfig> sessionConfigType;
    private final Set<Class<? extends Object>> envelopeTypes;

    public DefaultTransportMetadata(
            String providerName,
            String name,
            boolean connectionless,
            boolean fragmentation,
            Class<? extends SocketAddress> addressType,
            Class<? extends IoSessionConfig> sessionConfigType,
            Class<?>... envelopeTypes) {

        if (providerName == null) {
            throw new IllegalArgumentException("providerName");
        }
        if (name == null) {
            throw new IllegalArgumentException("name");
        }

        providerName = providerName.trim().toLowerCase();
        if (providerName.length() == 0) {
            throw new IllegalArgumentException("providerName is empty.");
        }
        name = name.trim().toLowerCase();
        if (name.length() == 0) {
            throw new IllegalArgumentException("name is empty.");
        }
        
        if (addressType == null) {
            throw new IllegalArgumentException("addressType");
        }

        if (envelopeTypes == null) {
            throw new IllegalArgumentException("envelopeTypes");
        }

        if (envelopeTypes.length == 0) {
            throw new IllegalArgumentException("envelopeTypes is empty.");
        }

        if (sessionConfigType == null) {
            throw new IllegalArgumentException("sessionConfigType");
        }

        this.providerName = providerName;
        this.name = name;
        this.connectionless = connectionless;
        this.fragmentation = fragmentation;
        this.addressType = addressType;
        this.sessionConfigType = sessionConfigType;

        Set<Class<? extends Object>> newEnvelopeTypes =
            new IdentityHashSet<Class<? extends Object>>();
        for (Class<? extends Object> c: envelopeTypes) {
            newEnvelopeTypes.add(c);
        }
        this.envelopeTypes = Collections.unmodifiableSet(newEnvelopeTypes);
    }

    public Class<? extends SocketAddress> getAddressType() {
        return addressType;
    }

    public Set<Class<? extends Object>> getEnvelopeTypes() {
        return envelopeTypes;
    }

    public Class<? extends IoSessionConfig> getSessionConfigType() {
        return sessionConfigType;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getName() {
        return name;
    }

    public boolean isConnectionless() {
        return connectionless;
    }

    public boolean hasFragmentation() {
        return fragmentation;
    }

    @Override
    public String toString() {
        return name;
    }
}
