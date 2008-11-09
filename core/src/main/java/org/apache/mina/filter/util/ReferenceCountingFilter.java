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
package org.apache.mina.filter.util;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.TrafficMask;
import org.apache.mina.core.write.WriteRequest;

/**
 * An {@link IoFilter}s wrapper that keeps track of the number of usages of this filter and will call init/destroy
 * when the filter is not in use.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * @org.apache.xbean.XBean
 */
public class ReferenceCountingFilter extends IoFilterAdapter {
    private final IoFilter filter;

    private int count = 0;

    public ReferenceCountingFilter(IoFilter filter) {
        this.filter = filter;
    }

    public void init() throws Exception {
        // no-op, will init on-demand in pre-add if count == 0
    }

    public void destroy() throws Exception {
        //no-op, will destroy on-demand in post-remove if count == 0
    }

    public synchronized void onPreAdd(IoSession session, int index, String name,
            IoFilter nextFilter) throws Exception {
        if (0 == count) {
            filter.init();

            ++count;
        }

        filter.onPreAdd(session, index, name, nextFilter);
    }

    public synchronized void onPostRemove(IoSession session, int index, String name,
            IoFilter nextFilter) throws Exception {
        filter.onPostRemove(session, index, name, nextFilter);

        --count;

        if (0 == count) {
            filter.destroy();
        }
    }

    public void exceptionCaught(int index, IoSession session,
            Throwable cause) throws Exception {
        filter.exceptionCaught(index+1, session, cause);
    }

    public void filterClose(int index, IoSession session)
            throws Exception {
        filter.filterClose(index+1, session);
    }

    public void filterWrite(int index, IoSession session,
            WriteRequest writeRequest) throws Exception {
        filter.filterWrite(index+1, session, writeRequest);
    }

    public void messageReceived(int index, IoSession session,
            Object message) throws Exception {
        filter.messageReceived(index+1, session, message);
    }

    public void messageSent(int index, IoSession session,
            WriteRequest writeRequest) throws Exception {
        filter.messageSent(index+1, session, writeRequest);
    }

    public void onPostAdd(IoSession session, int index, String name,
            IoFilter nextFilter) throws Exception {
        filter.onPostAdd(session, index, name, nextFilter);
    }

    public void onPreRemove(IoSession session, int index, String name,
            IoFilter nextFilter) throws Exception {
        filter.onPreRemove(session, index, name, nextFilter);
    }

    public void sessionClosed(int index, IoSession session)
            throws Exception {
        filter.sessionClosed(index+1, session);
    }

    public void sessionCreated(int index, IoSession session)
            throws Exception {
        filter.sessionCreated(index+1, session);
    }

    public void sessionIdle(int index, IoSession session,
            IdleStatus status) throws Exception {
        filter.sessionIdle(index+1, session, status);
    }

    public void sessionOpened(int index, IoSession session)
            throws Exception {
        filter.sessionOpened(index+1, session);
    }

    public void filterSetTrafficMask(int index, IoSession session,
            TrafficMask trafficMask) throws Exception {
        filter.filterSetTrafficMask(index+1, session, trafficMask);
    }
}
