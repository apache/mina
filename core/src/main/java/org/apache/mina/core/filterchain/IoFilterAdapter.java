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
    /** The next filter in the chain */
    private IoFilter nextFilter;
    
    /** The filter's name */
    protected static String name;

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
    public IoFilter getNextFilter() {
        return nextFilter;
    }
    
    /**
     * {@inheritDoc}
     */
    public IoFilter getNextFilterLock() {
        synchronized(nextFilter) {
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
    	getNextFilter().sessionCreated(session);
    }

    /**
     * {@inheritDoc}
     */
    public void sessionOpened(IoSession session) {
        getNextFilter().sessionOpened(session);
    }

    /**
     * {@inheritDoc}
     */
    public void sessionClosed(IoSession session) {
    	getNextFilter().sessionClosed(session);
    }

    /**
     * {@inheritDoc}
     */
    public void sessionIdle(IoSession session,
            IdleStatus status) {
    	getNextFilter().sessionIdle(session, status);
    }

    /**
     * {@inheritDoc}
     */
    public void exceptionCaught(IoSession session, Throwable cause) {
    	getNextFilter().exceptionCaught(session, cause);
    }

    /**
     * {@inheritDoc}
     */
    public void messageReceived(IoSession session,
            Object message) {
    	getNextFilter().messageReceived(session, message);
    }

    /**
     * {@inheritDoc}
     */
    public void messageSent(IoSession session,
            WriteRequest writeRequest) {
    	getNextFilter().messageSent(session, writeRequest);
    }

    /**
     * {@inheritDoc}
     */
    public void filterWrite(IoSession session, WriteRequest writeRequest) {
    	getNextFilter().filterWrite(session, writeRequest);
    }

    /**
     * {@inheritDoc}
     */
    public void filterClose(IoSession session) {
    	getNextFilter().filterClose(session);
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