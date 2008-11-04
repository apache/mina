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
 * An abstract class for {@link IoFilter}.  You can extend
 * this class and selectively override required event filter methods only.  All
 * methods forwards events to the next filter by default.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 591770 $, $Date: 2007-11-04 13:22:44 +0100 (Sun, 04 Nov 2007) $
 */
public class IoFilterAdapter implements IoFilter {
    /** The filter's name */
    private String name;

    /**
     * Creates a new instance of this class, and associate a name
     * to it.
     * @param name The filter's name
     */
    public  IoFilterAdapter(String name) {
    	this.name = name;
    }
    
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
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public IoFilter getNextFilterIn(IoSession session) {
        return session.getNextFilterIn(this);
    }
    
    /**
     * {@inheritDoc}
     */
    public IoFilter getNextFilterOut(IoSession session) {
        return session.getNextFilterOut(this);
    }
    
    /**
     * {@inheritDoc}
     *
    public IoFilter getNextFilterLock(IoSession session) {
        synchronized() {
            return nextFilter;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void onPreAdd(IoFilter parent, String name,
    		IoFilter nextFilter) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void onPostAdd(IoFilter parent, String name,
    		IoFilter nextFilter) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void onPreRemove(IoFilter parent, String name,
    		IoFilter nextFilter) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void onPostRemove(IoFilter parent, String name,
    		IoFilter nextFilter) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void sessionCreated(IoSession session) {
    	getNextFilterIn(session).sessionCreated(session);
    }

    /**
     * {@inheritDoc}
     */
    public void sessionOpened(IoSession session) {
        getNextFilterIn(session).sessionOpened(session);
    }

    /**
     * {@inheritDoc}
     */
    public void sessionClosed(IoSession session) {
    	getNextFilterIn(session).sessionClosed(session);
    }

    /**
     * {@inheritDoc}
     */
    public void sessionIdle(IoSession session,
            IdleStatus status) {
    	getNextFilterIn(session).sessionIdle(session, status);
    }

    /**
     * {@inheritDoc}
     */
    public void exceptionCaught(IoSession session, Throwable cause) {
    	getNextFilterIn(session).exceptionCaught(session, cause);
    }

    /**
     * {@inheritDoc}
     */
    public void messageReceived(IoSession session,
            Object message) {
    	getNextFilterIn(session).messageReceived(session, message);
    }

    /**
     * {@inheritDoc}
     */
    public void messageSent(IoSession session,
            WriteRequest writeRequest) {
    	getNextFilterOut(session).messageSent(session, writeRequest);
    }

    /**
     * {@inheritDoc}
     */
    public void filterWrite(IoSession session, WriteRequest writeRequest) {
    	getNextFilterOut(session).filterWrite(session, writeRequest);
    }

    /**
     * {@inheritDoc}
     */
    public void filterClose(IoSession session) {
    	getNextFilterOut(session).filterClose(session);
    }

    /**
     * {@inheritDoc}
     */
    public void filterSetTrafficMask(IoSession session,
            TrafficMask trafficMask) {
        //getNextFilter().filterSetTrafficMask(session, trafficMask);
    }
    
    public String toString() {
    	return this.getClass().getSimpleName();
    }
}