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
package org.apache.mina.protocol.http.client;

import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.http.HttpResponse;

public class HttpIoHandler extends IoHandlerAdapter {
    private AsyncHttpClientCallback callback;

    public HttpIoHandler(AsyncHttpClientCallback callback) {
        this.callback = callback;
    }

    @Override
    public void sessionOpened(IoSession ioSession) throws Exception {
        super.sessionOpened(ioSession);
    }

    @Override
    public void messageReceived(IoSession ioSession, Object object)
            throws Exception {
        HttpResponse message = (HttpResponse) object;
        callback.onResponse(message);
    }

    @Override
    public void exceptionCaught(IoSession ioSession, Throwable throwable)
            throws Exception {
        callback.onException(throwable);
    }

    @Override
    public void sessionClosed(IoSession ioSession) throws Exception {
        callback.onClosed();
    }
}
