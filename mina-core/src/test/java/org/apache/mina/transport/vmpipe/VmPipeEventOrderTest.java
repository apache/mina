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

import static org.junit.Assert.assertEquals;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.junit.Test;

/**
 * Makes sure the order of events are correct.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class VmPipeEventOrderTest {
    @Test
    public void testServerToClient() throws Exception {
        IoAcceptor acceptor = new VmPipeAcceptor();
        IoConnector connector = new VmPipeConnector();

        acceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void sessionOpened(IoSession session) throws Exception {
                session.write("B");
            }

            @Override
            public void messageSent(IoSession session, Object message)
                    throws Exception {
                session.close(true);
            }
        });

        acceptor.bind(new VmPipeAddress(1));

        final StringBuffer actual = new StringBuffer();

        connector.setHandler(new IoHandlerAdapter() {

            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                actual.append(message);
            }

            @Override
            public void sessionClosed(IoSession session) throws Exception {
                actual.append("C");
            }

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                actual.append("A");
            }

        });

        ConnectFuture future = connector.connect(new VmPipeAddress(1));

        future.awaitUninterruptibly();
        future.getSession().getCloseFuture().awaitUninterruptibly();
        acceptor.dispose();

        // sessionClosed() might not be invoked yet
        // even if the connection is closed.
        while (actual.indexOf("C") < 0) {
            Thread.yield();
        }

        assertEquals("ABC", actual.toString());
    }

    @Test
    public void testClientToServer() throws Exception {
        IoAcceptor acceptor = new VmPipeAcceptor();
        IoConnector connector = new VmPipeConnector();

        final StringBuffer actual = new StringBuffer();

        acceptor.setHandler(new IoHandlerAdapter() {

            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                actual.append(message);
            }

            @Override
            public void sessionClosed(IoSession session) throws Exception {
                actual.append("C");
            }

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                actual.append("A");
            }

        });

        acceptor.bind(new VmPipeAddress(1));

        connector.setHandler(new IoHandlerAdapter() {
            @Override
            public void sessionOpened(IoSession session) throws Exception {
                session.write("B");
            }

            @Override
            public void messageSent(IoSession session, Object message)
                    throws Exception {
                session.close(true);
            }
        });

        ConnectFuture future = connector.connect(new VmPipeAddress(1));

        future.awaitUninterruptibly();
        future.getSession().getCloseFuture().awaitUninterruptibly();
        acceptor.dispose();
        connector.dispose();

        // sessionClosed() might not be invoked yet
        // even if the connection is closed.
        while (actual.indexOf("C") < 0) {
            Thread.yield();
        }

        assertEquals("ABC", actual.toString());
    }

    @Test
    public void testSessionCreated() throws Exception {
        final Semaphore semaphore = new Semaphore(0);
        final StringBuffer stringBuffer = new StringBuffer();
        VmPipeAcceptor vmPipeAcceptor = new VmPipeAcceptor();
        final VmPipeAddress vmPipeAddress = new VmPipeAddress(12345);
        vmPipeAcceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void sessionCreated(IoSession session) throws Exception {
                // pretend we are doing some time-consuming work. For
                // performance reasons, you would never want to do time
                // consuming work in sessionCreated.
                // However, this increases the likelihood of the timing bug.
                Thread.sleep(1000);
                stringBuffer.append("A");
            }

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                stringBuffer.append("B");
            }

            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                stringBuffer.append("C");
            }
            
            @Override
            public void sessionClosed(IoSession session) throws Exception {
                stringBuffer.append("D");
                semaphore.release();
            }
        });
        vmPipeAcceptor.bind(vmPipeAddress);

        final VmPipeConnector vmPipeConnector = new VmPipeConnector();
        vmPipeConnector.getFilterChain().addLast("executor", new ExecutorFilter());
        vmPipeConnector.setHandler(new IoHandlerAdapter());
        ConnectFuture connectFuture = vmPipeConnector.connect(vmPipeAddress);
        connectFuture.awaitUninterruptibly();
        connectFuture.getSession().write(IoBuffer.wrap(new byte[1])).awaitUninterruptibly();
        connectFuture.getSession().close(false).awaitUninterruptibly();

        semaphore.tryAcquire(1, TimeUnit.SECONDS);
        vmPipeAcceptor.unbind(vmPipeAddress);
        assertEquals(1, connectFuture.getSession().getWrittenBytes());
        assertEquals("ABCD", stringBuffer.toString());
    }
}
