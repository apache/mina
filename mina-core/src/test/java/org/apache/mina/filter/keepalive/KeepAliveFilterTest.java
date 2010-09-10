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

import static org.apache.mina.filter.keepalive.KeepAliveRequestTimeoutHandler.EXCEPTION;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link KeepAliveFilter} used by the connector with different
 * interested {@link IdleStatus}es.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class KeepAliveFilterTest {
    // Constants -----------------------------------------------------
    static final IoBuffer PING = IoBuffer.wrap(new byte[] { 1 });
    static final IoBuffer PONG = IoBuffer.wrap(new byte[] { 2 });
    private static final int INTERVAL = 2;
    private static final int TIMEOUT = 1;

    private int port;
    private NioSocketAcceptor acceptor;

    @Before
    public void setUp() throws Exception {
        acceptor = new NioSocketAcceptor();
        KeepAliveMessageFactory factory = new ServerFactory();
        KeepAliveFilter filter = new KeepAliveFilter(factory,
                IdleStatus.BOTH_IDLE);
        acceptor.getFilterChain().addLast("keep-alive", filter);
        acceptor.setHandler(new IoHandlerAdapter());
        acceptor.setDefaultLocalAddress(new InetSocketAddress(0));
        acceptor.bind();
        port = acceptor.getLocalAddress().getPort();
    }

    @After
    public void tearDown() throws Exception {
        acceptor.unbind();
        acceptor.dispose();
    }

    @Test
    public void testKeepAliveFilterForReaderIdle() throws Exception {
        keepAliveFilterForIdleStatus(IdleStatus.READER_IDLE);
    }

    @Test
    public void testKeepAliveFilterForBothIdle() throws Exception {
        keepAliveFilterForIdleStatus(IdleStatus.BOTH_IDLE);
    }

    @Test
    public void testKeepAliveFilterForWriterIdle() throws Exception {
        keepAliveFilterForIdleStatus(IdleStatus.WRITER_IDLE);
    }

    // Package protected ---------------------------------------------

    // Protected -----------------------------------------------------

    // Private -------------------------------------------------------

    private void keepAliveFilterForIdleStatus(IdleStatus status)
            throws Exception {
        NioSocketConnector connector = new NioSocketConnector();
        KeepAliveFilter filter = new KeepAliveFilter(new ClientFactory(),
                status, EXCEPTION, INTERVAL, TIMEOUT);
        filter.setForwardEvent(true);
        connector.getFilterChain().addLast("keep-alive", filter);

        final AtomicBoolean gotException = new AtomicBoolean(false);
        connector.setHandler(new IoHandlerAdapter() {
            @Override
            public void exceptionCaught(IoSession session, Throwable cause)
                    throws Exception {
                //cause.printStackTrace();
                gotException.set(true);
            }

            @Override
            public void sessionIdle(IoSession session, IdleStatus status)
                    throws Exception {
                // Do nothing
            }
        });

        ConnectFuture future = connector.connect(
                new InetSocketAddress("127.0.0.1", port)).awaitUninterruptibly();
        IoSession session = future.getSession();
        assertNotNull(session);

        Thread.sleep((INTERVAL + TIMEOUT + 1) * 1000);

        assertFalse("got an exception on the client", gotException.get());

        session.close(true);
        connector.dispose();
    }

    static boolean checkRequest(IoBuffer message) {
        IoBuffer buff = message;
        boolean check = buff.get() == 1;
        buff.rewind();
        return check;
    }

    static boolean checkResponse(IoBuffer message) {
        IoBuffer buff = message;
        boolean check = buff.get() == 2;
        buff.rewind();
        return check;
    }

    // Inner classes -------------------------------------------------
    private final class ServerFactory implements KeepAliveMessageFactory {
        /**
         * Default constructor
         */
        public ServerFactory() {
            super();
        }
        
        public Object getRequest(IoSession session) {
            return null;
        }

        public Object getResponse(IoSession session, Object request) {
            return PONG.duplicate();
        }

        public boolean isRequest(IoSession session, Object message) {
            if (message instanceof IoBuffer) {
                return checkRequest((IoBuffer) message);
            }
            return false;
        }

        public boolean isResponse(IoSession session, Object message) {
            if (message instanceof IoBuffer) {
                return checkResponse((IoBuffer) message);
            }
            return false;
        }
    }

    private final class ClientFactory implements KeepAliveMessageFactory {
        /**
         * Default constructor
         */
        public ClientFactory() {
            super();
        }
        
        public Object getRequest(IoSession session) {
            return PING.duplicate();
        }

        public Object getResponse(IoSession session, Object request) {
            return null;
        }

        public boolean isRequest(IoSession session, Object message) {
            if (message instanceof IoBuffer) {
                return checkRequest((IoBuffer) message);
            }
            return false;
        }

        public boolean isResponse(IoSession session, Object message) {
            if (message instanceof IoBuffer) {
                return checkResponse((IoBuffer) message);
            }
            return false;
        }
    }
}
