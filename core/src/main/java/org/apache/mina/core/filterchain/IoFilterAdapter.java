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
	/** The filter's name */
	private String name;
	
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
    public void onPreAdd(IoSession session, int pos, String name,
        IoFilter filter) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void onPostAdd(IoSession session, int pos, String name,
        IoFilter filter) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void onPreRemove(IoSession session, int pos, String name,
        IoFilter filter) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void onPostRemove(IoSession session, int pos, String name,
        IoFilter filter) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void sessionCreated(int index, IoSession session)
            throws Exception {
    	IoFilter nextFilter = session.getFilterIn(index);
        nextFilter.sessionCreated(index+1, session);
    }

    /**
     * {@inheritDoc}
     */
    public void sessionOpened(int index, IoSession session)
            throws Exception {
    	IoFilter nextFilter = session.getFilterIn(index);
    	nextFilter.sessionOpened(index+1, session);
    }

    /**
     * {@inheritDoc}
     */
    public void sessionClosed(int index, IoSession session)
            throws Exception {
    	IoFilter nextFilter = session.getFilterIn(index);
    	nextFilter.sessionClosed(index+1, session);
    }

    /**
     * {@inheritDoc}
     */
    public void sessionIdle(int index, IoSession session,
            IdleStatus status) throws Exception {
    	IoFilter nextFilter = session.getFilterIn(index);
    	nextFilter.sessionIdle(index+1, session, status);
    }

    /**
     * {@inheritDoc}
     */
    public void exceptionCaught(int index, IoSession session,
            Throwable cause) throws Exception {
    	IoFilter nextFilter = session.getFilterIn(index);
    	nextFilter.exceptionCaught(index+1, session, cause);
    }

    /**
     * {@inheritDoc}
     */
    public void messageReceived(int index, IoSession session,
            Object message) throws Exception {
    	IoFilter nextFilter = session.getFilterIn(index);
    	nextFilter.messageReceived(index+1, session, message);
    }

    /**
     * {@inheritDoc}
     */
    public void messageSent(int index, IoSession session,
            WriteRequest writeRequest) throws Exception {
    	IoFilter nextFilter = session.getFilterIn(index);
    	nextFilter.messageSent(index+1, session, writeRequest);
    }

    /**
     * {@inheritDoc}
     */
    public void filterWrite(int index, IoSession session,
            WriteRequest writeRequest) throws Exception {
    	IoFilter nextFilter = session.getFilterOut(index+1);
    	nextFilter.filterWrite(index+1, session, writeRequest);
    }

    /**
     * {@inheritDoc}
     */
    public void filterClose(int index, IoSession session)
            throws Exception {
    	IoFilter nextFilter = session.getFilterIn(index);
    	nextFilter.filterClose(index+1, session);
    }

    /**
     * {@inheritDoc}
     */
    public void filterSetTrafficMask(int index, IoSession session,
            TrafficMask trafficMask) throws Exception {
    	IoFilter nextFilter = session.getFilterIn(index);
    	nextFilter.filterSetTrafficMask(index+1, session, trafficMask);
    }
    
    public String toString() {
    	return this.getClass().getSimpleName();
    }
}