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
package org.apache.mina;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoFilter;
import org.apache.mina.api.IoSession;
import org.apache.mina.transport.tcp.NioTcpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpProtocol implements IoFilter {

    private final static Logger logger = LoggerFactory.getLogger(HttpProtocol.class);

    // a queue of half-baked (pending/unfinished) HTTP post request
    private final Map<IoSession, PartialHttpRequest> partials = new HashMap<IoSession, PartialHttpRequest>();

    @Override
    public String toString() {
        return "HttpProtocol";
    }

    @Override
    public void sessionCreated(IoSession session) {
        // TODO Auto-generated method stub

    }

    @Override
    public void sessionOpened(IoSession session) {
        logger.debug("Session {} open", session);
    }

    @Override
    public void sessionClosed(IoSession session) {
        logger.debug("Session {} closed", session);
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {
        // TODO Auto-generated method stub

    }

    @Override
    public Object messageReceived(IoSession iosession, Object message) {
        logger.debug("handle read...");

        NioTcpSession session = (NioTcpSession) iosession;
        ByteBuffer buffer = (ByteBuffer) message;
        // do we have any unfinished http post requests for this channel?
        HttpRequest request = null;

        if (partials.containsKey(session)) {
            request = HttpRequest.continueParsing(buffer, partials.get(session));
            if (!(request instanceof PartialHttpRequest)) { // received the entire payload/body
                partials.remove(session);
            }
        } else { // we received a partial request, will keep it until it's totally built
            request = HttpRequest.of(buffer);
            if (request instanceof PartialHttpRequest) {
                partials.put(session, (PartialHttpRequest) request);
            }
        }
        // set extra request info
        request.setRemoteHost(session.getRemoteAddress().getAddress());
        request.setRemotePort(session.getRemoteAddress().getPort());
        request.setServerHost(session.getLocalAddress().getAddress());
        request.setServerPort(session.getLocalAddress().getPort());

        if (request.isKeepAlive()) {
            // TODO : handle keep-alive
        }

        // Only close if not async. In that case its up to RH to close it (+ don't close if it's a partial request).
        if (!(request instanceof PartialHttpRequest)) {
            return request;
        } else {
            return null;
        }

    }

    @Override
    public Object messageWriting(IoSession session, Object message) {
        // TODO Auto-generated method stub
        return null;
    }

}
