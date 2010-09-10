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
package org.apache.mina.transport.socket.nio;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.transport.AbstractConnectorTest;

/**
 * Tests {@link NioDatagramConnector}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DatagramConnectorTest extends AbstractConnectorTest {

    @Override
    protected IoAcceptor createAcceptor() {
        return new NioDatagramAcceptor();
    }

    @Override
    protected IoConnector createConnector() {
        return new NioDatagramConnector();
    }

    @Override
    public void testConnectFutureFailureTiming() throws Exception {
        // Skip the test; Datagram connection can be made even if there's no
        // server at the endpoint.
    }

}
