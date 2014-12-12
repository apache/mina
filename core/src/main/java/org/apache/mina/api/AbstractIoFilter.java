/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mina.api;

import org.apache.mina.filterchain.ReadFilterChainController;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.apache.mina.session.WriteRequest;

/**
 * A convenient {@link IoFilter} implementation to be sub-classed for easier IoFilter implementation.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractIoFilter implements IoFilter {

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionOpened(final IoSession session) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionClosed(final IoSession session) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionIdle(final IoSession session, final IdleStatus status) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageReceived(final IoSession session, final Object message,
            final ReadFilterChainController controller) {
        controller.callReadNextFilter(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageWriting(IoSession session, WriteRequest message, WriteFilterChainController controller) {
        controller.callWriteNextFilter(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageSent(final IoSession session, final Object message) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handshakeStarted(IoSession session) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handshakeCompleted(IoSession session) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void secureClosed(IoSession session) {
    }
    
}