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
package org.apache.mina.common;

/**
 * An abstract adapter class for {@link IoFilter}.  You can extend
 * this class and selectively override required event filter methods only.  All
 * methods forwards events to the next filter by default.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IoFilterAdapter implements IoFilter {
    public void init() throws Exception {
    }

    public void destroy() throws Exception {
    }

    public void onPreAdd(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
    }

    public void onPostAdd(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
    }

    public void onPreRemove(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
    }

    public void onPostRemove(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
    }

    public void sessionCreated(NextFilter nextFilter, IoSession session)
            throws Exception {
        nextFilter.sessionCreated(session);
    }

    public void sessionOpened(NextFilter nextFilter, IoSession session)
            throws Exception {
        nextFilter.sessionOpened(session);
    }

    public void sessionClosed(NextFilter nextFilter, IoSession session)
            throws Exception {
        nextFilter.sessionClosed(session);
    }

    public void sessionIdle(NextFilter nextFilter, IoSession session,
            IdleStatus status) throws Exception {
        nextFilter.sessionIdle(session, status);
    }

    public void exceptionCaught(NextFilter nextFilter, IoSession session,
            Throwable cause) throws Exception {
        nextFilter.exceptionCaught(session, cause);
    }

    public void messageReceived(NextFilter nextFilter, IoSession session,
            Object message) throws Exception {
        nextFilter.messageReceived(session, message);
    }

    public void messageSent(NextFilter nextFilter, IoSession session,
            Object message) throws Exception {
        nextFilter.messageSent(session, message);
    }

    public void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        nextFilter.filterWrite(session, writeRequest);
    }

    public void filterClose(NextFilter nextFilter, IoSession session)
            throws Exception {
        nextFilter.filterClose(session);
    }
}