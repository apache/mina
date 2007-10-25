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
package org.apache.mina.filter.keepalive;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.ChainedIoHandler;
import org.apache.mina.handler.chain.IoHandlerCommand;
import org.apache.mina.transport.vmpipe.VmPipeAcceptor;
import org.apache.mina.transport.vmpipe.VmPipeAddress;
import org.apache.mina.transport.vmpipe.VmPipeConnector;

/**
 * Tests if (Active)KeepAliveFilter is working. The test makes a simple
 * server-client setup where the client will reply to three pings.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class KeepAliveFilterTest extends TestCase {
    protected ScheduledExecutorService scheduler;

    protected KeepAliveFilterFactory factory;

    int serverSentCounter;

    int clientReceiveCounter;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(KeepAliveFilterTest.class);
    }

    @Override
    protected void setUp() {
        this.scheduler = new ScheduledThreadPoolExecutor(1);
        this.factory = new KeepAliveFilterFactory(scheduler);
        factory.setPingingInterval(100);
        factory.setWaitingTime(300);
        this.serverSentCounter = 0;
        this.clientReceiveCounter = 0;
    }

    @Override
    protected void tearDown() {
        this.factory = null;
        this.serverSentCounter = 0;
        this.clientReceiveCounter = 0;
    }

    public void testKeepAliveFilter() throws IOException {
        IoAcceptor acceptor = new VmPipeAcceptor();
        VmPipeAddress address = new VmPipeAddress(1234);

        // make the server
        FakeServer server = new FakeServer();
        acceptor.setLocalAddress(address);
        acceptor.setHandler(server);
        acceptor.bind();

        // now connect to server
        VmPipeConnector connector = new VmPipeConnector();
        connector.setHandler(new FakeClient());
        ConnectFuture future = connector.connect(address);

        future.awaitUninterruptibly();
        server.getSemaphore().acquireUninterruptibly();
        // the semaphore is released when the connection is closed

        if (clientReceiveCounter != 4) {
            fail("Connection terminated prematurely");
        }

        acceptor.unbind();
        scheduler.shutdownNow();
    }

    class FakeServer extends ChainedIoHandler {
        Semaphore semaphore = new Semaphore(0);

        @Override
        public void sessionCreated(IoSession session) throws Exception {
            super.getChain().addFirst("KeepAlive",
                    factory.createActiveKeepAliveFilter(session));
        }

        @Override
        public void messageReceived(IoSession session, Object message)
                throws Exception {
            super.messageReceived(session, message);
        }

        @Override
        public void messageSent(IoSession session, Object message)
                throws Exception {
            super.messageSent(session, message);
            if (serverSentCounter > 1 && clientReceiveCounter < 1) {
                fail("Something wrong with test. Client not receiving");
            }
            if (serverSentCounter - clientReceiveCounter > 4) {
                fail("ActiveFilter should have broken connection");
            }
            serverSentCounter++;
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            semaphore.release();
        }

        Semaphore getSemaphore() {
            return semaphore;
        }
    }

    class FakeClient extends ChainedIoHandler {
        FakeClient() {
            super.getChain().addFirst("KeepAlive",
                    factory.createPassiveKeepAliveFilter());
            super.getChain().addLast("print", new IoHandlerCommand() {
                public void execute(NextCommand next, IoSession session,
                        Object message) throws Exception {
                    System.out.println(message);
                }
            });
        }

        @Override
        public void messageReceived(IoSession session, Object message)
                throws Exception {
            if (clientReceiveCounter >= 4) {
                return;
            }
            super.messageReceived(session, message);
            clientReceiveCounter++;
        }

    }
}
