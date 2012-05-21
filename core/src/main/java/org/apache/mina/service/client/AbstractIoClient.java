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
package org.apache.mina.service.client;

import java.net.SocketAddress;

import org.apache.mina.api.IoClient;
import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoSession;
import org.apache.mina.service.AbstractIoService;

/**
 * TODO
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractIoClient extends AbstractIoService implements IoClient {
    /**
     * Create an new AbstractIoClient instance
     */
    protected AbstractIoClient() {
        super();
    }

    @Override
    public long getConnectTimeoutMillis() {
        return 0;
    }

    @Override
    public void setConnectTimeoutMillis(long connectTimeoutInMillis) {
    }

    @Override
    public IoFuture<IoSession> connect(SocketAddress remoteAddress) {
        return null;
    }

    @Override
    public IoFuture<IoSession> connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        return null;
    }

}
