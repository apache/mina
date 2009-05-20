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
package org.apache.mina.filter.logging;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.statistic.ProfilerTimerFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests {@link MdcInjectionFilter} in variuos scenarios.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class MdcInjectionFilterTest {

    static Logger LOGGER = LoggerFactory.getLogger(MdcInjectionFilterTest.class);
    private static final int TIMEOUT = 5000;

    final MyAppender appender = new MyAppender();
    private int port;
    private NioSocketAcceptor acceptor;

    @Before
    public void setUp() throws Exception {
        // comment out next line if you want to see normal logging
        org.apache.log4j.Logger.getRootLogger().removeAllAppenders();
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.DEBUG);
        org.apache.log4j.Logger.getRootLogger().addAppender(appender);
        acceptor = new NioSocketAcceptor();
    }


    @After
    public void tearDown() throws Exception {
        acceptor.dispose();
    }

    @Test
    public void testSimpleChain() throws IOException, InterruptedException {
        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        chain.addFirst("mdc-injector", new MdcInjectionFilter());
        chain.addLast("dummy", new DummyIoFilter());
        chain.addLast("protocol", new ProtocolCodecFilter(new DummyProtocolCodecFactory()));
        test(chain);
    }

    @Test
    public void testExecutorFilterAtTheEnd() throws IOException, InterruptedException {
        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        MdcInjectionFilter mdcInjectionFilter = new MdcInjectionFilter();
        chain.addFirst("mdc-injector1", mdcInjectionFilter);
        chain.addLast("dummy", new DummyIoFilter());
        chain.addLast("protocol", new ProtocolCodecFilter(new DummyProtocolCodecFactory()));
        chain.addLast("executor" , new ExecutorFilter());
        chain.addLast("mdc-injector2", mdcInjectionFilter);
        test(chain);
    }

    @Test
    public void testExecutorFilterAtBeginning() throws IOException, InterruptedException {
        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        MdcInjectionFilter mdcInjectionFilter = new MdcInjectionFilter();
        chain.addLast("executor" , new ExecutorFilter());
        chain.addLast("mdc-injector", mdcInjectionFilter);
        chain.addLast("dummy", new DummyIoFilter());
        chain.addLast("protocol", new ProtocolCodecFilter(new DummyProtocolCodecFactory()));
        test(chain);
    }

    @Test
    public void testExecutorFilterBeforeProtocol() throws IOException, InterruptedException {
        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        MdcInjectionFilter mdcInjectionFilter = new MdcInjectionFilter();
        chain.addLast("executor" , new ExecutorFilter());
        chain.addLast("mdc-injector", mdcInjectionFilter);
        chain.addLast("dummy", new DummyIoFilter());
        chain.addLast("protocol", new ProtocolCodecFilter(new DummyProtocolCodecFactory()));
        test(chain);
    }

    @Test
    public void testMultipleFilters() throws IOException, InterruptedException {
        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        MdcInjectionFilter mdcInjectionFilter = new MdcInjectionFilter();
        chain.addLast("executor" , new ExecutorFilter());
        chain.addLast("mdc-injector", mdcInjectionFilter);
        chain.addLast("profiler", new ProfilerTimerFilter());
        chain.addLast("dummy", new DummyIoFilter());
        chain.addLast("logger", new LoggingFilter());
        chain.addLast("protocol", new ProtocolCodecFilter(new DummyProtocolCodecFactory()));
        test(chain);
    }

    @Test
    public void testTwoExecutorFilters() throws IOException, InterruptedException {
        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        MdcInjectionFilter mdcInjectionFilter = new MdcInjectionFilter();
        chain.addLast("executor1" , new ExecutorFilter());
        chain.addLast("mdc-injector1", mdcInjectionFilter);
        chain.addLast("protocol", new ProtocolCodecFilter(new DummyProtocolCodecFactory()));
        chain.addLast("dummy", new DummyIoFilter());
        chain.addLast("executor2" , new ExecutorFilter());
        // add the MdcInjectionFilter instance after every ExecutorFilter
        // it's important to use the same MdcInjectionFilter instance
        chain.addLast("mdc-injector2",  mdcInjectionFilter);
        test(chain);
    }

    @Test
    public void testOnlyRemoteAddress() throws IOException, InterruptedException {
        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        chain.addFirst("mdc-injector", new MdcInjectionFilter(
            MdcInjectionFilter.MdcKey.remoteAddress));
        chain.addLast("dummy", new DummyIoFilter());
        chain.addLast("protocol", new ProtocolCodecFilter(new DummyProtocolCodecFactory()));
        SimpleIoHandler simpleIoHandler = new SimpleIoHandler();
        acceptor.setHandler(simpleIoHandler);
        acceptor.bind(new InetSocketAddress(0));
        port = acceptor.getLocalAddress().getPort();
        acceptor.setFilterChainBuilder(chain);
        // create some clients
        NioSocketConnector connector = new NioSocketConnector();
        connector.setHandler(new IoHandlerAdapter());
        connectAndWrite(connector,0);
        connectAndWrite(connector,1);
        // wait until Iohandler has received all events
        simpleIoHandler.messageSentLatch.await();
        simpleIoHandler.sessionIdleLatch.await();
        simpleIoHandler.sessionClosedLatch.await();
        connector.dispose();

        // make a copy to prevent ConcurrentModificationException
        List<LoggingEvent> events = new ArrayList<LoggingEvent>(appender.events);
        // verify that all logging events have correct MDC
        for (LoggingEvent event : events) {
            for (MdcInjectionFilter.MdcKey mdcKey : MdcInjectionFilter.MdcKey.values()) {
              String key = mdcKey.name();
              Object value = event.getMDC(key);
              if (mdcKey == MdcInjectionFilter.MdcKey.remoteAddress) {
                  assertNotNull(
                      "MDC[remoteAddress] not set for [" + event.getMessage() + "]", value);
              } else {
                  assertNull("MDC[" + key + "] set for [" + event.getMessage() + "]", value);
              }
            }
        }
    }

    private void test(DefaultIoFilterChainBuilder chain) throws IOException, InterruptedException {
        // configure the server
        SimpleIoHandler simpleIoHandler = new SimpleIoHandler();
        acceptor.setHandler(simpleIoHandler);
        acceptor.bind(new InetSocketAddress(0));
        port = acceptor.getLocalAddress().getPort();
        acceptor.setFilterChainBuilder(chain);
        // create some clients
        NioSocketConnector connector = new NioSocketConnector();
        connector.setHandler(new IoHandlerAdapter());
        SocketAddress remoteAddressClients[] = new SocketAddress[2];
        remoteAddressClients[0] = connectAndWrite(connector,0);
        remoteAddressClients[1] = connectAndWrite(connector,1);
        // wait until Iohandler has received all events
        simpleIoHandler.messageSentLatch.await();
        simpleIoHandler.sessionIdleLatch.await();
        simpleIoHandler.sessionClosedLatch.await();
        connector.dispose();

        // make a copy to prevent ConcurrentModificationException
        List<LoggingEvent> events = new ArrayList<LoggingEvent>(appender.events);

        // verify that all logging events have correct MDC
        for (LoggingEvent event : events) {
            if (!ExecutorFilter.class.getName().equals(event.getLoggerName())) {
                Object remoteAddress = event.getMDC("remoteAddress");
                assertNotNull(
                    "MDC[remoteAddress] not set for [" + event.getMessage() + "]",
                    remoteAddress);
                assertNotNull(
                    "MDC[remotePort] not set for [" + event.getMessage() + "]",
                    event.getMDC("remotePort"));
                assertEquals(
                    "every event should have MDC[handlerClass]",
                    SimpleIoHandler.class.getName(),
                    event.getMDC("handlerClass") );
            }
        }
        // assert we have received all expected logging events for each client
        for (int i = 0; i < remoteAddressClients.length; i++) {
            SocketAddress remoteAddressClient = remoteAddressClients[i];
            assertEventExists(events, "sessionCreated", remoteAddressClient, null);
            assertEventExists(events, "sessionOpened", remoteAddressClient, null);
            assertEventExists(events, "decode", remoteAddressClient, null);
            assertEventExists(events, "messageReceived-1", remoteAddressClient, null);
            assertEventExists(events, "messageReceived-2", remoteAddressClient, "user-" + i);
            assertEventExists(events, "encode", remoteAddressClient, null);
            assertEventExists(events, "exceptionCaught", remoteAddressClient, "user-" + i);
            assertEventExists(events, "messageSent-1", remoteAddressClient, "user-" + i);
            assertEventExists(events, "messageSent-2", remoteAddressClient, null);
            assertEventExists(events, "sessionIdle", remoteAddressClient, "user-" + i);
            assertEventExists(events, "sessionClosed", remoteAddressClient, "user-" + i);
            assertEventExists(events, "sessionClosed", remoteAddressClient, "user-" + i);
            assertEventExists(events, "DummyIoFilter.sessionOpened", remoteAddressClient, "user-" + i);
        }
    }

    private SocketAddress connectAndWrite(NioSocketConnector connector, int clientNr) {
        ConnectFuture connectFuture = connector.connect(new InetSocketAddress("localhost", port));
        connectFuture.awaitUninterruptibly(TIMEOUT);
        IoBuffer message = IoBuffer.allocate(4).putInt(clientNr).flip();
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

        /**
         * Default constructor
         */
        public SimpleIoHandler() {
            super();
        }
        
        @Override
        public void sessionCreated(IoSession session) throws Exception {
            LOGGER.info("sessionCreated");
            session.getConfig().setIdleTime(IdleStatus.BOTH_IDLE, 1);
        }

        @Override
        public void sessionOpened(IoSession session) throws Exception {
            LOGGER.info("sessionOpened");
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            LOGGER.info("sessionClosed");
            sessionClosedLatch.countDown();
        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
            LOGGER.info("sessionIdle");
            sessionIdleLatch.countDown();
            session.close(true);
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
            LOGGER.info("exceptionCaught", cause);
        }

        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            LOGGER.info("messageReceived-1");
            // adding a custom property to the context
            String user = "user-" + message;
            MdcInjectionFilter.setProperty(session, "user", user);
            LOGGER.info("messageReceived-2");
            session.getService().broadcast(message);
            throw new RuntimeException("just a test, forcing exceptionCaught");
        }

        @Override
        public void messageSent(IoSession session, Object message) throws Exception {
            LOGGER.info("messageSent-1");
            MdcInjectionFilter.removeProperty(session, "user");
            LOGGER.info("messageSent-2");
            messageSentLatch.countDown();
        }
    }

    private static class DummyProtocolCodecFactory implements ProtocolCodecFactory {
        /**
         * Default constructor
         */
        public DummyProtocolCodecFactory() {
            super();
        }
        
        public ProtocolEncoder getEncoder(IoSession session) throws Exception {
            return new ProtocolEncoderAdapter() {
                public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
                    LOGGER.info("encode");
                    IoBuffer buffer = IoBuffer.allocate(4).putInt(123).flip();
                    out.write(buffer);
                }
            };
        }

        public ProtocolDecoder getDecoder(IoSession session) throws Exception {
            return new ProtocolDecoderAdapter() {
                public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
                    if (in.remaining() >= 4) {
                        int value = in.getInt();
                        LOGGER.info("decode");
                        out.write(value);
                    }
                }
            };
        }
    }

    private static class MyAppender extends AppenderSkeleton {
        List<LoggingEvent> events = Collections.synchronizedList(new ArrayList<LoggingEvent>());

        /**
         * Default constructor
         */
        public MyAppender() {
            super();
        }
        
        @Override
        protected void append(final LoggingEvent loggingEvent) {
            loggingEvent.getMDCCopy();
            events.add(loggingEvent);
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }

        @Override
        public void close() {
            // Do nothing
        }
    }

    static class DummyIoFilter extends IoFilterAdapter {
        @Override
        public void sessionOpened(NextFilter nextFilter, IoSession session) throws Exception {
            LOGGER.info("DummyIoFilter.sessionOpened");
            nextFilter.sessionOpened(session);
        }
    }

}
