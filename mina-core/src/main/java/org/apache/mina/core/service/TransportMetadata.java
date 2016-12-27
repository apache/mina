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
import java.util.Set;

import org.apache.mina.core.session.IoSessionConfig;

/**
 * Provides meta-information that describes an {@link IoService}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface TransportMetadata {

    /**
     * @return the name of the service provider (e.g. "nio", "apr" and "rxtx").
     */
    String getProviderName();

    /**
     * @return the name of the service.
     */
    String getName();

    /**
     * @return <tt>true</tt> if the session of this transport type is
     * <a href="http://en.wikipedia.org/wiki/Connectionless">connectionless</a>.
     */
    boolean isConnectionless();

    /**
     * @return {@code true} if the messages exchanged by the service can be
     * <a href="http://en.wikipedia.org/wiki/IPv4#Fragmentation_and_reassembly">fragmented
     * or reassembled</a> by its underlying transport.
     */
    boolean hasFragmentation();

    /**
     * @return the address type of the service.
     */
    Class<? extends SocketAddress> getAddressType();

    /**
     * @return the set of the allowed message type when you write to an
     * {@link IoSession} that is managed by the service.
     */
    Set<Class<? extends Object>> getEnvelopeTypes();

    /**
     * @return the type of the {@link IoSessionConfig} of the service
     */
    Class<? extends IoSessionConfig> getSessionConfigType();
}
