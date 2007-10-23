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
package org.apache.mina.filter;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;

/**
 * An {@link IoFilter}s wrapper that keeps track of the number of usages of this filter and will call init/destroy
 * when the filter is not in use.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ReferenceCountingIoFilter implements IoFilter {
    private final IoFilter filter;

    private int count = 0;

    public ReferenceCountingIoFilter(IoFilter filter) {
        this.filter = filter;
    }

    public void init() throws Exception {
        // no-op, will init on-demand in pre-add if count == 0
    }

    public void destroy() throws Exception {
        //no-op, will destroy on-demand in post-remove if count == 0
    }

    public synchronized void onPreAdd(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
        if (0 == count) {
            filter.init();

            ++count;
        }

        filter.onPreAdd(parent, name, nextFilter);
    }

    public synchronized void onPostRemove(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
        filter.onPostRemove(parent, name, nextFilter);

        --count;

        if (0 == count) {
            filter.destroy();
        }
    }

    public void exceptionCaught(NextFilter nextFilter, IoSession session,
            Throwable cause) throws Exception {
        filter.exceptionCaught(nextFilter, session, cause);
    }

    public void filterClose(NextFilter nextFilter, IoSession session)
            throws Exception {
        filter.filterClose(nextFilter, session);
    }

    public void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        filter.filterWrite(nextFilter, session, writeRequest);
    }

    public void messageReceived(NextFilter nextFilter, IoSession session,
            Object message) throws Exception {
        filter.messageReceived(nextFilter, session, message);
    }

    public void messageSent(NextFilter nextFilter, IoSession session,
            Object message) throws Exception {
        filter.messageSent(nextFilter, session, message);
    }

    public void onPostAdd(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
        filter.onPostAdd(parent, name, nextFilter);
    }

    public void onPreRemove(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
        filter.onPreRemove(parent, name, nextFilter);
    }

    public void sessionClosed(NextFilter nextFilter, IoSession session)
            throws Exception {
        filter.sessionClosed(nextFilter, session);
    }

    public void sessionCreated(NextFilter nextFilter, IoSession session)
            throws Exception {
        filter.sessionCreated(nextFilter, session);
    }

    public void sessionIdle(NextFilter nextFilter, IoSession session,
            IdleStatus status) throws Exception {
        filter.sessionIdle(nextFilter, session, status);
    }

    public void sessionOpened(NextFilter nextFilter, IoSession session)
            throws Exception {
        filter.sessionOpened(nextFilter, session);
    }
}
