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
package org.apache.mina.example.proxy;

import java.nio.charset.Charset;

import org.apache.mina.common.buffer.IoBuffer;
import org.apache.mina.common.service.IoHandlerAdapter;
import org.apache.mina.common.session.IoSession;
import org.apache.mina.common.session.TrafficMask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of {@link org.apache.mina.common.service.IoHandler} classes which handle
 * proxied connections.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 *
 */
public abstract class AbstractProxyIoHandler extends IoHandlerAdapter {
    private static final Charset CHARSET = Charset.forName("iso8859-1");

    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Override
    public void sessionCreated(IoSession session) throws Exception {
        session.setTrafficMask(TrafficMask.NONE);
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        if (session.getAttribute( "" ) != null) {
            ((IoSession) session.getAttribute("")).setAttribute("", null);
            ((IoSession) session.getAttribute("")).closeOnFlush();
            session.setAttribute("", null);
        }
    }

    @Override
    public void messageReceived(IoSession session, Object message)
            throws Exception {
        IoBuffer rb = (IoBuffer) message;
        IoBuffer wb = IoBuffer.allocate(rb.remaining());
        rb.mark();
        wb.put(rb);
        wb.flip();
        ((IoSession) session.getAttribute("")).write(wb);
        rb.reset();
        logger.info(rb.getString(CHARSET.newDecoder()));
    }
}
