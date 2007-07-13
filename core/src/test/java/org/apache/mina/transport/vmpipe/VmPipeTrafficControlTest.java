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

import java.net.SocketAddress;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoHandler;
import org.apache.mina.transport.AbstractTrafficControlTest;

/**
 * Tests suspending and resuming reads and writes for the
 * {@link org.apache.mina.common.TransportType#VM_PIPE} transport type. 
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Id$
 */
public class VmPipeTrafficControlTest extends AbstractTrafficControlTest {

    public VmPipeTrafficControlTest() {
        super(new VmPipeAcceptor());
    }

    protected ConnectFuture connect(int port, IoHandler handler)
            throws Exception {
        IoConnector connector = new VmPipeConnector();
        SocketAddress addr = new VmPipeAddress(port);
        return connector.connect(addr, handler);
    }

    protected SocketAddress createServerSocketAddress(int port) {
        return new VmPipeAddress(port);
    }

}
