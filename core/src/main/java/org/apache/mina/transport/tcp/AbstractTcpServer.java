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
package org.apache.mina.transport.tcp;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;
import java.util.Set;

import org.apache.mina.api.IoFilter;
import org.apache.mina.service.server.AbstractIoServer;

/**
 * Base class for TCP based Servers
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class AbstractTcpServer extends AbstractIoServer {
    /**
     * Create an new AbsractTcpServer instance
     */
    protected AbstractTcpServer() {
        super();
    }

    @Override
    public Set<SocketAddress> getLocalAddresses() {
        return null;
    }

    @Override
    public void bind(SocketAddress... localAddress) throws IOException {
    }

    @Override
    public void unbindAll() throws IOException {
    }

    @Override
    public void unbind(SocketAddress... localAddresses) throws IOException {
    }

    private List<IoFilter> filters;

}
