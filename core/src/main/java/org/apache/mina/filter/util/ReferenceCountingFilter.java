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

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilter;
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
    // Set the filter's default name
    private static final String DEFAULT_NAME = "referenceCounting";
    
    private final IoFilter filter;

    private int count = 0;

    public ReferenceCountingFilter(String name, IoFilter filter) {
    	super(name);
        this.filter = filter;
    }

    public ReferenceCountingFilter(IoFilter filter) {
    	super(DEFAULT_NAME);
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

    public void exceptionCaught( IoSession session, Throwable cause) {
        filter.exceptionCaught(session, cause);
    }

    public void filterClose(IoSession session) {
        filter.filterClose(session);
    }

    public void filterWrite(IoSession session, WriteRequest writeRequest) {
        filter.filterWrite(session, writeRequest);
    }

    public void messageReceived(IoSession session, Object message) {
        filter.messageReceived(session, message);
    }

    public void messageSent(IoSession session,
            WriteRequest writeRequest) {
        filter.messageSent(session, writeRequest);
    }

    public void onPostAdd(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
        filter.onPostAdd(parent, name, nextFilter);
    }

    public void onPreRemove(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
    	getNextFilter().onPreRemove(parent, name, nextFilter);
    }

    public void sessionClosed(IoSession session) {
        getNextFilter().sessionClosed(session);
    }

    public void sessionCreated(IoSession session) {
    	getNextFilter().sessionCreated(session);
    }

    public void sessionIdle(IoSession session, IdleStatus status) {
    	getNextFilter().sessionIdle(session, status);
    }

    public void sessionOpened(IoSession session) {
    	getNextFilter().sessionOpened(session);
    }

    public void filterSetTrafficMask(IoSession session, TrafficMask trafficMask) {
    	//getNextFilter().setTrafficMask(session, trafficMask);
    }
}
