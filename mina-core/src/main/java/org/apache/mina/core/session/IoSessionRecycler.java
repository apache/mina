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
package org.apache.mina.core.session;

import java.net.SocketAddress;

import org.apache.mina.core.service.IoService;

/**
 * A connectionless transport can recycle existing sessions by assigning an
 * {@link IoSessionRecycler} to an {@link IoService}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoSessionRecycler {
    /**
     * A dummy recycler that doesn't recycle any sessions.  Using this recycler will
     * make all session lifecycle events to be fired for every I/O for all connectionless
     * sessions.
     */
    IoSessionRecycler NOOP = new IoSessionRecycler() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void put(IoSession session) {
            // Do nothing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public IoSession recycle(SocketAddress remoteAddress) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove(IoSession session) {
            // Do nothing
        }
    };

    /**
     * Called when the underlying transport creates or writes a new {@link IoSession}.
     *
     * @param session the new {@link IoSession}.
     */
    void put(IoSession session);

    /**
     * Attempts to retrieve a recycled {@link IoSession}.
     *
     * @param remoteAddress the remote socket address of the {@link IoSession} the transport wants to recycle.
     * @return a recycled {@link IoSession}, or null if one cannot be found.
     */
    IoSession recycle(SocketAddress remoteAddress);

    /**
     * Called when an {@link IoSession} is explicitly closed.
     *
     * @param session the new {@link IoSession}.
     */
    void remove(IoSession session);
}
