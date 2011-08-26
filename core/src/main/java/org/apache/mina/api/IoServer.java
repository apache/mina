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
package org.apache.mina.api;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Set;

/**
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 */
public interface IoServer extends IoService {

    /**
     * Returns a {@link Set} of the local addresses which are bound currently.
     */
    Set<SocketAddress> getLocalAddresses();

    /**
     * Binds to the specified local addresses and start to accept incoming
     * connections.
     *
     * @throws IOException
     *             if failed to bind
     */
    void bind(SocketAddress... localAddress) throws IOException;

    /**
     * Unbinds from all local addresses that this service is bound to and stops
     * to accept incoming connections. This method returns silently if no local
     * address is bound yet.
     * @throws IOException
     *             if failed to unbind
     */
    void unbindAll() throws IOException;

    /**
     * Unbinds from the specified local addresses and stop to accept incoming
     * connections. This method returns silently if the default local addresses
     * are not bound yet.
     * @throws IOException
     *             if failed to unbind
     */
    void unbind(SocketAddress... localAddresses) throws IOException;
}
