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
import org.apache.mina.core.session.IoEvent;
import org.apache.mina.core.session.IoEventType;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.TrafficMask;
import org.apache.mina.core.write.WriteRequest;

/**
 * An I/O event or an I/O request that MINA provides for {@link IoFilter}s.
 * Most users won't need to use this class.  It is usually used by internal
 * components to store I/O events.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 591770 $, $Date: 2007-11-04 13:22:44 +0100 (Sun, 04 Nov 2007) $
 */
public class IoFilterEvent extends IoEvent {
	int nextFilterIndex;

    public IoFilterEvent(int nextFilterIndex, IoEventType type,
            IoSession session, Object parameter) {
        super(type, session, parameter);

        this.nextFilterIndex = nextFilterIndex;
    }

    public IoFilter getNextFilter() {
        return getSession().getFilter(nextFilterIndex);
    }

    public void fire() throws Exception {
        switch (getType()) {
        case MESSAGE_RECEIVED:
            getNextFilter().messageReceived(nextFilterIndex+1, getSession(), getParameter());
            break;
        case MESSAGE_SENT:
            getNextFilter().messageSent(nextFilterIndex+1, getSession(), (WriteRequest) getParameter());
            break;
        case WRITE:
            getNextFilter().filterWrite(nextFilterIndex+1, getSession(), (WriteRequest) getParameter());
            break;
        case SET_TRAFFIC_MASK:
            getNextFilter().filterSetTrafficMask(nextFilterIndex+1, getSession(), (TrafficMask) getParameter());
            break;
        case CLOSE:
            getNextFilter().filterClose(nextFilterIndex+1, getSession());
            break;
        case EXCEPTION_CAUGHT:
            getNextFilter().exceptionCaught(nextFilterIndex+1, getSession(), (Throwable) getParameter());
            break;
        case SESSION_IDLE:
            getNextFilter().sessionIdle(nextFilterIndex+1, getSession(), (IdleStatus) getParameter());
            break;
        case SESSION_OPENED:
            getNextFilter().sessionOpened(nextFilterIndex+1, getSession());
            break;
        case SESSION_CREATED:
            getNextFilter().sessionCreated(nextFilterIndex+1, getSession());
            break;
        case SESSION_CLOSED:
            getNextFilter().sessionClosed(nextFilterIndex+1, getSession());
            break;
        default:
            throw new IllegalArgumentException("Unknown event type: " + getType());
        }
    }
}
