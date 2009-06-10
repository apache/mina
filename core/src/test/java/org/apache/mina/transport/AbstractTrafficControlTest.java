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
package org.apache.mina.transport;

import java.net.SocketAddress;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Abstract base class for testing suspending and resuming reads and
 * writes.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractTrafficControlTest {

    protected int port;
    protected IoAcceptor acceptor;
    protected TransportMetadata transportType;

    public AbstractTrafficControlTest(IoAcceptor acceptor) {
        this.acceptor = acceptor;
    }

    @Before
    public void setUp() throws Exception {
        acceptor.setHandler(new ServerIoHandler());
        acceptor.bind(createServerSocketAddress(0));
        port = getPort(acceptor.getLocalAddress());
    }

    @After
    public void tearDown() throws Exception {
        acceptor.unbind();
        acceptor.dispose();
    }

    protected abstract ConnectFuture connect(int port, IoHandler handler)
            throws Exception;

    protected abstract SocketAddress createServerSocketAddress(int port);
    protected abstract int getPort(SocketAddress address);

    @Test
    public void testSuspendResumeReadWrite() throws Exception {
        ConnectFuture future = connect(port, new ClientIoHandler());
        future.awaitUninterruptibly();
        IoSession session = future.getSession();

        // We wait for the sessionCreated() event is fired because we
        // cannot guarantee that it is invoked already.
        while (session.getAttribute("lock") == null) {
            Thread.yield();
        }
        
        Object lock = session.getAttribute("lock");
        synchronized (lock) {

            write(session, "1");
            assertEquals('1', read(session));
            assertEquals("1", getReceived(session));
            assertEquals("1", getSent(session));

            session.suspendRead();

            Thread.sleep(100);

            write(session, "2");
            assertFalse(canRead(session));
            assertEquals("1", getReceived(session));
            assertEquals("12", getSent(session));

            session.suspendWrite();

            Thread.sleep(100);

            write(session, "3");
            assertFalse(canRead(session));
            assertEquals("1", getReceived(session));
            assertEquals("12", getSent(session));

            session.resumeRead();

            Thread.sleep(100);

            write(session, "4");
            assertEquals('2', read(session));
            assertEquals("12", getReceived(session));
            assertEquals("12", getSent(session));

            session.resumeWrite();

            Thread.sleep(100);

            assertEquals('3', read(session));
            assertEquals('4', read(session));

            write(session, "5");
            assertEquals('5', read(session));
            assertEquals("12345", getReceived(session));
            assertEquals("12345", getSent(session));

            session.suspendWrite();

            Thread.sleep(100);

            write(session, "6");
            assertFalse(canRead(session));
            assertEquals("12345", getReceived(session));
            assertEquals("12345", getSent(session));

            session.suspendRead();
            session.resumeWrite();

            Thread.sleep(100);

            write(session, "7");
            assertFalse(canRead(session));
            assertEquals("12345", getReceived(session));
            assertEquals("1234567", getSent(session));

            session.resumeRead();

            Thread.sleep(100);

            assertEquals('6', read(session));
            assertEquals('7', read(session));

            assertEquals("1234567", getReceived(session));
            assertEquals("1234567", getSent(session));

        }

        session.close(true).awaitUninterruptibly();
    }

    private void write(IoSession session, String s) throws Exception {
        session.write(IoBuffer.wrap(s.getBytes("ASCII")));
    }

    private int read(IoSession session) throws Exception {
        int pos = ((Integer) session.getAttribute("pos")).intValue();
        for (int i = 0; i < 10 && pos == getReceived(session).length(); i++) {
            Object lock = session.getAttribute("lock");
            lock.wait(200);
        }
        session.setAttribute("pos", new Integer(pos + 1));
        String received = getReceived(session);
        assertTrue(received.length() > pos);
        return getReceived(session).charAt(pos);
    }

    private boolean canRead(IoSession session) throws Exception {
        int pos = ((Integer) session.getAttribute("pos")).intValue();
        Object lock = session.getAttribute("lock");
        lock.wait(250);
        String received = getReceived(session);
        return pos < received.length();
    }

    private String getReceived(IoSession session) throws Exception {
        return session.getAttribute("received").toString();
    }

    private String getSent(IoSession session) throws Exception {
        return session.getAttribute("sent").toString();
    }

    private static class ClientIoHandler extends IoHandlerAdapter {
        /**
         * Default constructor
         */
        public ClientIoHandler() {
            super();
        }
        
        @Override
        public void sessionCreated(IoSession session) throws Exception {
            super.sessionCreated(session);
            session.setAttribute("pos", new Integer(0));
            session.setAttribute("received", new StringBuffer());
            session.setAttribute("sent", new StringBuffer());
            session.setAttribute("lock", new Object());
        }

        @Override
        public void messageReceived(IoSession session, Object message)
                throws Exception {
            IoBuffer buffer = (IoBuffer) message;
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            Object lock = session.getAttribute("lock");
            synchronized (lock) {
                StringBuffer sb = (StringBuffer) session
                        .getAttribute("received");
                sb.append(new String(data, "ASCII"));
                lock.notifyAll();
            }
        }

        @Override
        public void messageSent(IoSession session, Object message)
                throws Exception {
            IoBuffer buffer = (IoBuffer) message;
            buffer.rewind();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            StringBuffer sb = (StringBuffer) session.getAttribute("sent");
            sb.append(new String(data, "ASCII"));
        }

    }

    private static class ServerIoHandler extends IoHandlerAdapter {
        /**
         * Default constructor
         */
        public ServerIoHandler() {
            super();
        }
        
        @Override
        public void messageReceived(IoSession session, Object message)
                throws Exception {
            // Just echo the received bytes.
            IoBuffer rb = (IoBuffer) message;
            IoBuffer wb = IoBuffer.allocate(rb.remaining());
            wb.put(rb);
            wb.flip();
            session.write(wb);
        }
    }
}
