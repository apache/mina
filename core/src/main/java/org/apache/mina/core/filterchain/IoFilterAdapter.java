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
package org.apache.mina.core.filterchain;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.TrafficMask;
import org.apache.mina.core.write.WriteRequest;

/**
 * An adapter class for {@link IoFilter}.  You can extend
 * this class and selectively override required event filter methods only.  All
 * methods forwards events to the next filter by default.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 591770 $, $Date: 2007-11-04 13:22:44 +0100 (Sun, 04 Nov 2007) $
 */
public class IoFilterAdapter implements IoFilter {
    /**
     * {@inheritDoc}
     */
    public void init() throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void onPreAdd(IoFilterChain parent, String name,
        NextFilter nextFilter) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void onPostAdd(IoFilterChain parent, String name,
        NextFilter nextFilter) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void onPreRemove(IoFilterChain parent, String name,
        NextFilter nextFilter) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void onPostRemove(IoFilterChain parent, String name,
        NextFilter nextFilter) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void sessionCreated(NextFilter nextFilter, IoSession session)
            throws Exception {
        nextFilter.sessionCreated(session);
    }

    /**
     * {@inheritDoc}
     */
    public void sessionOpened(NextFilter nextFilter, IoSession session)
            throws Exception {
        nextFilter.sessionOpened(session);
    }

    /**
     * {@inheritDoc}
     */
    public void sessionClosed(NextFilter nextFilter, IoSession session)
            throws Exception {
        nextFilter.sessionClosed(session);
    }

    /**
     * {@inheritDoc}
     */
    public void sessionIdle(NextFilter nextFilter, IoSession session,
            IdleStatus status) throws Exception {
        nextFilter.sessionIdle(session, status);
    }

    /**
     * {@inheritDoc}
     */
    public void exceptionCaught(NextFilter nextFilter, IoSession session,
            Throwable cause) throws Exception {
        nextFilter.exceptionCaught(session, cause);
    }

    /**
     * {@inheritDoc}
     */
    public void messageReceived(NextFilter nextFilter, IoSession session,
            Object message) throws Exception {
        nextFilter.messageReceived(session, message);
    }

    /**
     * {@inheritDoc}
     */
    public void messageSent(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        nextFilter.messageSent(session, writeRequest);
    }

    /**
     * {@inheritDoc}
     */
    public void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        nextFilter.filterWrite(session, writeRequest);
    }

    /**
     * {@inheritDoc}
     */
    public void filterClose(NextFilter nextFilter, IoSession session)
            throws Exception {
        nextFilter.filterClose(session);
    }

    /**
     * {@inheritDoc}
     */
    public void filterSetTrafficMask(NextFilter nextFilter, IoSession session,
            TrafficMask trafficMask) throws Exception {
        nextFilter.filterSetTrafficMask(session, trafficMask);
    }
}