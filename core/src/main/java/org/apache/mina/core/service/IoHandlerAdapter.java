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
package org.apache.mina.core.service;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An abstract adapter class for {@link IoHandler}.  You can extend this
 * class and selectively override required event handler methods only.  All
 * methods do nothing by default.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class IoHandlerAdapter implements IoHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    public static final AttributeKey SESSION_CREATED_FUTURE = new AttributeKey(
            IoFilter.class, "connectFuture");

    public void init() throws Exception {
    }
    
    public void destroy() throws Exception {
    }

    public String getName() {
    	return null;
    }

    public void sessionCreated(IoSession session) {
    }

    public void sessionOpened(IoSession session) {
    }

    public void sessionClosed(IoSession session) {
    }

    public void sessionIdle(IoSession session, IdleStatus status) {
    }

    public void exceptionCaught(IoSession session, Throwable cause) {
        if (logger.isWarnEnabled()) {
            logger.warn("EXCEPTION, please implement "
                    + getClass().getName()
                    + ".exceptionCaught() for proper handling:", cause);
        }
    }

    public void messageReceived(IoSession session, Object message) {
    }

    public void messageSent(IoSession session, WriteRequest message) {
    }
    
    public void filterClose(IoSession session) {
    }
    
    public void filterWrite(IoSession session, WriteRequest writeRequest) {
    }
    
    public void onPreAdd(IoFilter parent, String name, IoFilter nextFilter)
    		throws Exception {
    }

    public void onPostAdd(IoFilter parent, String name, IoFilter nextFilter)
    		throws Exception {
    }

    public void onPreRemove(IoFilter parent, String name, IoFilter nextFilter)
    		throws Exception {
    }

    public void onPostRemove(IoFilter parent, String name, IoFilter nextFilter)
    		throws Exception {
    }
    
    public IoFilter getNextFilterOut(IoSession session) {
    	return null;
    }
    
    public IoFilter getNextFilterIn(IoSession session) {
    	return null;
    }
}