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
package org.apache.mina.transport.vmpipe;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.mina.core.service.IoConnector;
import org.apache.mina.transport.AbstractBindTest;
import org.apache.mina.util.AvailablePortFinder;

/**
 * Tests {@link VmPipeAcceptor} bind and unbind.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class VmPipeBindTest extends AbstractBindTest {

    public VmPipeBindTest() {
        super(new VmPipeAcceptor());
    }

    @Override
    protected SocketAddress createSocketAddress(int port) {
        return new VmPipeAddress(port);
    }

    @Override
    protected int getPort(SocketAddress address) {
        return ((VmPipeAddress) address).getPort();
    }

    @Override
    protected IoConnector newConnector() {
        return new VmPipeConnector();
    }

    @Override
    protected void bind( boolean reuseAddress ) throws IOException
    {
        acceptor.setHandler(new EchoProtocolHandler());

        // Find an available test port and bind to it.
        int nbTry = 10;
        
        while (true) {
            try {
                port = AvailablePortFinder.getNextAvailable();
                acceptor.setDefaultLocalAddress(createSocketAddress(port));
                acceptor.bind();
                break;
            } catch (RuntimeException re ) {
                nbTry--;
                
                if (nbTry == 0) {
                    throw new IOException("Cannot bind any test port.");
                }
            }
        }
    }

    @Override
    protected void setReuseAddress( boolean reuseAddress ) throws IOException
    {
        // Nothing to do for VmPipe
    }
}
