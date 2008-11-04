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
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev:$, $Date:$
 */
public abstract class CommonEventFilter extends IoFilterAdapter {
    // Set the filter's default name
	private static final String DEFAULT_NAME = "commonEvent";
	
	/**
	 * Create a new instance with a default filter name
	 */
    public CommonEventFilter() {
    	super(DEFAULT_NAME);
    }

	/**
	 * Create a new instance with a given filter name
	 * 
	 * @param name the filter's name
	 */
    public CommonEventFilter(String name) {
    	super(name);
    }

    protected abstract void filter(IoFilterEvent event);

    @Override
    public final void sessionCreated(IoSession session) {
        filter(new IoFilterEvent(session.getNextFilterIn(this), IoEventType.SESSION_CREATED, session, null));
    }

    @Override
    public final void sessionOpened(IoSession session) {
        filter(new IoFilterEvent(session.getNextFilterIn(this), IoEventType.SESSION_OPENED, session, null));
    }

    @Override
    public final void sessionClosed(IoSession session) {
        filter(new IoFilterEvent(session.getNextFilterIn(this), IoEventType.SESSION_CLOSED, session, null));
    }

    @Override
    public final void sessionIdle(IoSession session, IdleStatus status) {
        filter(new IoFilterEvent(session.getNextFilterIn(this), IoEventType.SESSION_IDLE, session, status));
    }

    @Override
    public final void exceptionCaught(IoSession session, Throwable cause) {
        filter(new IoFilterEvent(session.getNextFilterIn(this), IoEventType.EXCEPTION_CAUGHT, session, cause));
    }

    @Override
    public final void messageReceived(IoSession session, Object message) {
        filter(new IoFilterEvent(session.getNextFilterIn(this), IoEventType.MESSAGE_RECEIVED, session, message));
    }

    @Override
    public final void messageSent(IoSession session, WriteRequest writeRequest) {
        filter(new IoFilterEvent(session.getNextFilterOut(this), IoEventType.MESSAGE_SENT, session, writeRequest));
    }

    @Override
    public final void filterWrite(IoSession session, WriteRequest writeRequest) {
        filter(new IoFilterEvent(session.getNextFilterOut(this), IoEventType.WRITE, session, writeRequest));
    }

    @Override
    public final void filterClose(IoSession session) {
        filter(new IoFilterEvent(session.getNextFilterIn(this), IoEventType.CLOSE, session, null));
    }
}
