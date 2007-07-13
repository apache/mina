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

import junit.framework.TestCase;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.TransportType;
import org.apache.mina.util.AvailablePortFinder;

/**
 * Abstract base class for testing suspending and resuming reads and
 * writes.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractTrafficControlTest extends TestCase {
    protected int port = 0;

    protected IoAcceptor acceptor;

    protected TransportType transportType;

    public AbstractTrafficControlTest(IoAcceptor acceptor) {
        this.acceptor = acceptor;
    }

    protected void setUp() throws Exception {
        super.setUp();

        port = AvailablePortFinder.getNextAvailable();

        acceptor.bind(createServerSocketAddress(port), new ServerIoHandler());

    }

    protected void tearDown() throws Exception {
        super.tearDown();

        acceptor.unbind(createServerSocketAddress(port));
    }

    protected abstract ConnectFuture connect(int port, IoHandler handler)
            throws Exception;

    protected abstract SocketAddress createServerSocketAddress(int port);

    public void testSuspendResumeReadWrite() throws Exception {
        ConnectFuture future = connect(port, new ClientIoHandler());
        future.join();
        IoSession session = future.getSession();

        // We wait for the sessionCreated() event is fired becayse we cannot guarentee that
        // it is invoked already.
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

            write(session, "2");
            assertFalse(canRead(session));
            assertEquals("1", getReceived(session));
            assertEquals("12", getSent(session));

            session.suspendWrite();

            write(session, "3");
            assertFalse(canRead(session));
            assertEquals("1", getReceived(session));
            assertEquals("12", getSent(session));

            session.resumeRead();

            write(session, "4");
            assertEquals('2', read(session));
            assertEquals("12", getReceived(session));
            assertEquals("12", getSent(session));

            session.resumeWrite();
            assertEquals('3', read(session));
            assertEquals('4', read(session));

            write(session, "5");
            assertEquals('5', read(session));
            assertEquals("12345", getReceived(session));
            assertEquals("12345", getSent(session));

            session.suspendWrite();

            write(session, "6");
            assertFalse(canRead(session));
            assertEquals("12345", getReceived(session));
            assertEquals("12345", getSent(session));

            session.suspendRead();
            session.resumeWrite();

            write(session, "7");
            assertFalse(canRead(session));
            assertEquals("12345", getReceived(session));
            assertEquals("1234567", getSent(session));

            session.resumeRead();
            assertEquals('6', read(session));
            assertEquals('7', read(session));

            assertEquals("1234567", getReceived(session));
            assertEquals("1234567", getSent(session));

        }

        session.close().join();
    }

    private void write(IoSession session, String s) throws Exception {
        session.write(ByteBuffer.wrap(s.getBytes("ASCII")));
    }

    private int read(IoSession session) throws Exception {
        int pos = ((Integer) session.getAttribute("pos")).intValue();
        for (int i = 0; i < 10 && pos == getReceived(session).length(); i++) {
            Object lock = session.getAttribute("lock");
            lock.wait(200);
        }
        session.setAttribute("pos", new Integer(pos + 1));
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

    public static class ClientIoHandler extends IoHandlerAdapter {
        public void sessionCreated(IoSession session) throws Exception {
            super.sessionCreated(session);
            session.setAttribute("pos", new Integer(0));
            session.setAttribute("received", new StringBuffer());
            session.setAttribute("sent", new StringBuffer());
            session.setAttribute("lock", new Object());
        }

        public void messageReceived(IoSession session, Object message)
                throws Exception {
            ByteBuffer buffer = (ByteBuffer) message;
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

        public void messageSent(IoSession session, Object message)
                throws Exception {
            ByteBuffer buffer = (ByteBuffer) message;
            buffer.rewind();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            StringBuffer sb = (StringBuffer) session.getAttribute("sent");
            sb.append(new String(data, "ASCII"));
        }

    }

    private static class ServerIoHandler extends IoHandlerAdapter {
        public void messageReceived(IoSession session, Object message)
                throws Exception {
            // Just echo the received bytes.
            ByteBuffer rb = (ByteBuffer) message;
            ByteBuffer wb = ByteBuffer.allocate(rb.remaining());
            wb.put(rb);
            wb.flip();
            session.write(wb);
        }
    }
}
