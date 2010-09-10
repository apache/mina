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
import org.apache.mina.core.filterchain.IoFilterEvent;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoEventType;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

/**
 * Extend this class when you want to create a filter that
 * wraps the same logic around all 9 IoEvents
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class CommonEventFilter extends IoFilterAdapter {

    public CommonEventFilter() {
        // Do nothing
    }

    protected abstract void filter(IoFilterEvent event) throws Exception;

    @Override
    public final void sessionCreated(NextFilter nextFilter, IoSession session) throws Exception {
        filter(new IoFilterEvent(nextFilter, IoEventType.SESSION_CREATED, session, null));
    }

    @Override
    public final void sessionOpened(NextFilter nextFilter, IoSession session) throws Exception {
        filter(new IoFilterEvent(nextFilter, IoEventType.SESSION_OPENED, session, null));
    }

    @Override
    public final void sessionClosed(NextFilter nextFilter, IoSession session) throws Exception {
        filter(new IoFilterEvent(nextFilter, IoEventType.SESSION_CLOSED, session, null));
    }

    @Override
    public final void sessionIdle(NextFilter nextFilter, IoSession session, IdleStatus status) throws Exception {
        filter(new IoFilterEvent(nextFilter, IoEventType.SESSION_IDLE, session, status));
    }

    @Override
    public final void exceptionCaught(NextFilter nextFilter, IoSession session, Throwable cause) throws Exception {
        filter(new IoFilterEvent(nextFilter, IoEventType.EXCEPTION_CAUGHT, session, cause));
    }

    @Override
    public final void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        filter(new IoFilterEvent(nextFilter, IoEventType.MESSAGE_RECEIVED, session, message));
    }

    @Override
    public final void messageSent(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        filter(new IoFilterEvent(nextFilter, IoEventType.MESSAGE_SENT, session, writeRequest));
    }

    @Override
    public final void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        filter(new IoFilterEvent(nextFilter, IoEventType.WRITE, session, writeRequest));
    }

    @Override
    public final void filterClose(NextFilter nextFilter, IoSession session) throws Exception {
        filter(new IoFilterEvent(nextFilter, IoEventType.CLOSE, session, null));
    }
}
