package org.apache.mina.filter.logging;

import junit.framework.TestCase;
import org.apache.mina.filter.codec.*;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.statistic.ProfilerTimerFilter;
import org.apache.mina.common.*;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 */
public class MDCInjectionFilterTest extends TestCase {

    private static Logger logger = LoggerFactory.getLogger("MDCInjectionFilterTest");
    private static final int PORT = 7475;
    private static final int TIMEOUT = 5000;

    private MyAppender appender = new MyAppender();
    private SocketAcceptor acceptor;

    protected void setUp() throws Exception {
        super.setUp();
        // uncomment next line if you want to see normal logging
        org.apache.log4j.Logger.getRootLogger().removeAllAppenders();
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.DEBUG);
        org.apache.log4j.Logger.getRootLogger().addAppender(appender);
        acceptor = new SocketAcceptor();
    }


    protected void tearDown() throws Exception {
        acceptor.unbind();
        super.tearDown();
    }

    public void testSimpleChain() throws IOException, InterruptedException {
        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        chain.addFirst("mdc-injector", new MDCInjectionFilter());
        chain.addLast("dummy", new DummyIoFilter());
        chain.addLast("protocol", new ProtocolCodecFilter(new DummyProtocolCodecFactory()));
        test(chain);
    }

    public void testExecutorFilterAtTheEnd() throws IOException, InterruptedException {
        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        MDCInjectionFilter mdcInjectionFilter = new MDCInjectionFilter();
        chain.addFirst("mdc-injector1", mdcInjectionFilter);
        chain.addLast("dummy", new DummyIoFilter());
        chain.addLast("protocol", new ProtocolCodecFilter(new DummyProtocolCodecFactory()));
        chain.addLast("executor" , new ExecutorFilter());
        chain.addLast("mdc-injector2", mdcInjectionFilter);
        test(chain);
    }

    public void testExecutorFilterAtBeginning() throws IOException, InterruptedException {
        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        MDCInjectionFilter mdcInjectionFilter = new MDCInjectionFilter();
        chain.addLast("executor" , new ExecutorFilter());
        chain.addLast("mdc-injector", mdcInjectionFilter);
        chain.addLast("dummy", new DummyIoFilter());
        chain.addLast("protocol", new ProtocolCodecFilter(new DummyProtocolCodecFactory()));
        test(chain);
    }

    public void testExecutorFilterBeforeProtocol() throws IOException, InterruptedException {
        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        MDCInjectionFilter mdcInjectionFilter = new MDCInjectionFilter();
        chain.addLast("executor" , new ExecutorFilter());
        chain.addLast("mdc-injector", mdcInjectionFilter);
        chain.addLast("dummy", new DummyIoFilter());
        chain.addLast("protocol", new ProtocolCodecFilter(new DummyProtocolCodecFactory()));
        test(chain);
    }

    public void testMultipleFilters() throws IOException, InterruptedException {
        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        MDCInjectionFilter mdcInjectionFilter = new MDCInjectionFilter();
        chain.addLast("executor" , new ExecutorFilter());
        chain.addLast("mdc-injector", mdcInjectionFilter);
        chain.addLast("profiler", new ProfilerTimerFilter());
        chain.addLast("dummy", new DummyIoFilter());
        chain.addLast("logger", new LoggingFilter());
        chain.addLast("protocol", new ProtocolCodecFilter(new DummyProtocolCodecFactory()));
        test(chain);
    }


    public void testTwoExecutorFilters() throws IOException, InterruptedException {
        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        MDCInjectionFilter mdcInjectionFilter = new MDCInjectionFilter();
        chain.addLast("executor1" , new ExecutorFilter());
        chain.addLast("mdc-injector1", mdcInjectionFilter);
        chain.addLast("protocol", new ProtocolCodecFilter(new DummyProtocolCodecFactory()));
        chain.addLast("dummy", new DummyIoFilter());
        chain.addLast("executor2" , new ExecutorFilter());
        chain.addLast("mdc-injector2", mdcInjectionFilter);
        test(chain);
    }

    private void test(DefaultIoFilterChainBuilder chain) throws IOException, InterruptedException {
        // configure the server
        SimpleIoHandler simpleIoHandler = new SimpleIoHandler();
        acceptor.setHandler(simpleIoHandler);
        acceptor.setLocalAddress(new InetSocketAddress(PORT));
        acceptor.bind();
        acceptor.setFilterChainBuilder(chain);
        // create some clients
        SocketConnector connector = new SocketConnector();
        connector.setHandler(new IoHandlerAdapter());
        SocketAddress remoteAddressClients[] = new SocketAddress[2];
        remoteAddressClients[0] = connectAndWrite(connector,0);
        remoteAddressClients[1] = connectAndWrite(connector,1);
        // wait until Iohandler has received all events
        simpleIoHandler.messageSentLatch.await();
        simpleIoHandler.sessionIdleLatch.await();
        simpleIoHandler.sessionClosedLatch.await();

        // verify that all logging events have correct MDC
        for (LoggingEvent event : new ArrayList<LoggingEvent>(appender.events)) {
            if (!ExecutorFilter.class.getName().equals(event.getLoggerName())) {
                Object remoteAddress = event.getMDC("remoteAddress");
                assertNotNull(
                    "MDC[remoteAddress] not set for [" + event.getMessage() + "]",
                    remoteAddress);
                assertNotNull(
                    "MDC[remotePort] not set for [" + event.getMessage() + "]",
                    event.getMDC("remotePort"));
                assertEquals(
                    "every event should have MDC[IoHandlerClass]",
                    SimpleIoHandler.class.getName(),
                    event.getMDC("IoHandlerClass") );
            }
        }
        // asert we have received all expected logging events for each client
        for (int i = 0; i < remoteAddressClients.length; i++) {
            SocketAddress remoteAddressClient = remoteAddressClients[i];
            assertEventExists(appender.events, "sessionCreated", remoteAddressClient, null);
            assertEventExists(appender.events, "sessionOpened", remoteAddressClient, null);
            assertEventExists(appender.events, "decode", remoteAddressClient, null);
            assertEventExists(appender.events, "messageReceived", remoteAddressClient, null);
            assertEventExists(appender.events, "encode", remoteAddressClient, null);
            assertEventExists(appender.events, "exceptionCaught", remoteAddressClient, "user-" + i);
            assertEventExists(appender.events, "messageSent", remoteAddressClient, "user-" + i);
            assertEventExists(appender.events, "sessionIdle", remoteAddressClient, "user-" + i);
            assertEventExists(appender.events, "sessionClosed", remoteAddressClient, "user-" + i);
            assertEventExists(appender.events, "sessionClosed", remoteAddressClient, "user-" + i);
            assertEventExists(appender.events, "DummyIoFilter.sessionOpened", remoteAddressClient, "user-" + i);
        }
    }

    private SocketAddress connectAndWrite(SocketConnector connector, int clientNr) {
        ConnectFuture connectFuture = connector.connect(new InetSocketAddress("localhost",PORT));
        connectFuture.awaitUninterruptibly(TIMEOUT);
        ByteBuffer message = ByteBuffer.allocate(4).putInt(clientNr).flip();
        IoSession session = connectFuture.getSession();
        session.write(message).awaitUninterruptibly(TIMEOUT);
        return session.getLocalAddress();
    }

    private void assertEventExists(List<LoggingEvent> events,
                                   String message,
                                   SocketAddress address,
                                   String user) {
        InetSocketAddress remoteAddress = (InetSocketAddress) address;
        for (LoggingEvent event : events) {
            if (event.getMessage().equals(message) &&
                event.getMDC("remoteAddress").equals(remoteAddress.toString()) &&
                event.getMDC("remoteIp").equals(remoteAddress.getAddress().getHostAddress()) &&
                event.getMDC("remotePort").equals(remoteAddress.getPort()+"") ) {
                if (user == null && event.getMDC("user") == null) {
                    return;
                }
                if (user != null && user.equals(event.getMDC("user"))) {
                    return;
                }                
                return;
            }
        }
        fail("No LoggingEvent found from [" + remoteAddress +"] with message [" + message + "]");
    }

    private static class SimpleIoHandler extends IoHandlerAdapter {

        CountDownLatch sessionIdleLatch = new CountDownLatch(2);
        CountDownLatch sessionClosedLatch = new CountDownLatch(2);
        CountDownLatch messageSentLatch = new CountDownLatch(2);

        public void sessionCreated(IoSession session) throws Exception {
            logger.info("sessionCreated");
            session.getConfig().setIdleTime(IdleStatus.BOTH_IDLE, 1);
        }

        public void sessionOpened(IoSession session) throws Exception {
            logger.info("sessionOpened");
        }

        public void sessionClosed(IoSession session) throws Exception {
            logger.info("sessionClosed");
            sessionClosedLatch.countDown();
        }

        public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
            logger.info("sessionIdle");
            sessionIdleLatch.countDown();
            session.close();            
        }

        public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
            logger.info("exceptionCaught", cause);
        }

        public void messageReceived(IoSession session, Object message) throws Exception {
            logger.info("messageReceived");
            // adding a custom property to the context
            String user = "user-" + message;
            MDCInjectionFilter.setProperty(session, "user", user);
            logger.info("logged-in: " + user);
            session.write(message);
            throw new RuntimeException("just a test, forcing exceptionCaught");
        }

        public void messageSent(IoSession session, Object message) throws Exception {
            logger.info("messageSent");
            messageSentLatch.countDown();
        }
    }

    private static class DummyProtocolCodecFactory implements ProtocolCodecFactory {

        public ProtocolEncoder getEncoder() throws Exception {
            return new ProtocolEncoderAdapter() {
                public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
                    logger.info("encode");
                    ByteBuffer buffer = ByteBuffer.allocate(4).putInt(123).flip();
                    out.write(buffer);
                }
            };
        }

        public ProtocolDecoder getDecoder() throws Exception {
            return new ProtocolDecoderAdapter() {
                public void decode(IoSession session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception {
                    if (in.remaining() >= 4) {
                        int value = in.getInt();
                        logger.info("decode");
                        out.write(value);                        
                    }
                }
            };
        }
    }

    private static class MyAppender extends AppenderSkeleton {

        List<LoggingEvent> events = Collections.synchronizedList(new ArrayList<LoggingEvent>());

        protected void append(final LoggingEvent loggingEvent) {
            loggingEvent.getMDCCopy();
            events.add(loggingEvent);
        }

        public boolean requiresLayout() {
            return false;
        }

        public void close() {
        }
    }

    private static class DummyIoFilter extends IoFilterAdapter {
        public void sessionOpened(NextFilter nextFilter, IoSession session) throws Exception {
            logger.info("DummyIoFilter.sessionOpened");
            nextFilter.sessionOpened(session);
        }
    }

}
