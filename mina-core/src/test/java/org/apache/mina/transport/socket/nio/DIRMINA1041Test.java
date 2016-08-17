package org.apache.mina.transport.socket.nio;

import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class DIRMINA1041Test {

    private static final Logger LOG = LoggerFactory.getLogger(DIRMINA1041Test.class);
    private static final String HOST = "localhost";
    private static final int PORT = AvailablePortFinder.getNextAvailable(); 
    private static final long TIMEOUT = 3000L;
    private SocketAcceptor acceptor;
    private SocketConnector connector;
    
    @Before
    public void setUp() throws Exception {
        acceptor = new NioSocketAcceptor();
        acceptor.setHandler(new SomeAcceptHandler());
        acceptor.bind(new InetSocketAddress(HOST, PORT));

        connector = new NioSocketConnector();
        connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory()));
        connector.setHandler(new SomeConnectHandler());
    }

    @Test
    public void testWrite() throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            IoSession session = getSession();

            WriteFuture future = session.write("Test");
            LOG.info("Waiting for WriteFuture to complete. Session: " + session);
            if (!future.await(TIMEOUT)) {
                Assert.fail("WriteFuture did not complete. Session: " + session);
            }

            closeSession(session);
        }
    }

    @After
    public void tearDown() throws Exception {
        try { connector.dispose(true); } catch (Throwable e) { e.printStackTrace(); }
        try { acceptor.unbind(); acceptor.dispose(true); } catch (Throwable e) { e.printStackTrace(); }
    }

    private IoSession getSession() {
        ConnectFuture future = connector.connect(new InetSocketAddress(HOST, PORT));
        if (!future.awaitUninterruptibly(TIMEOUT)) {
            Assert.fail("ConnectFuture did not complete.");
        }
        return future.getSession();
    }

    private void closeSession(IoSession session) {
        CloseFuture closeFuture = session.closeNow();
        if (!closeFuture.awaitUninterruptibly(TIMEOUT)) {
            Assert.fail("CloseFuture did not complete.");
        }
    }

    private class SomeConnectHandler extends IoHandlerAdapter {
        @Override
        public void sessionClosed(IoSession session) throws Exception {
            LOG.info("Connector - Session closed : " + session);
        }
    }

    private class SomeAcceptHandler extends IoHandlerAdapter {
        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            session.closeNow();
        }
    }
}
