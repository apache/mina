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

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.Test;

public class SocketAcceptorTest {

    @Test
    public void testBindTwice() throws Exception {
        NioSocketAcceptor acceptor = new NioSocketAcceptor() {

            private int nRequests;

            private CountDownLatch secondRequestAdded = new CountDownLatch(1);

            @Override
            protected void bindRequestAdded() {
                super.bindRequestAdded();
                nRequests++;
                if (nRequests == 2) {
                    secondRequestAdded.countDown();
                }
            }

            @Override
            protected void handleUnbound(Collection<AcceptorOperationFuture> unboundFutures) throws Exception {
                super.handleUnbound(unboundFutures);
                if (!unboundFutures.isEmpty() && nRequests == 1) {
                    secondRequestAdded.await();
                }
            }
        };
        acceptor.setCloseOnDeactivation(false);
        acceptor.setReuseAddress(true);
        acceptor.setHandler(new IoHandlerAdapter());
        try {
            int port = AvailablePortFinder.getNextAvailable(1025);
            InetSocketAddress address = new InetSocketAddress("127.0.0.1", port);
            acceptor.bind(address);
            acceptor.unbind(address);
            acceptor.bind(address);
            acceptor.unbind(address);
        } finally {
            acceptor.dispose();
        }
    }
}