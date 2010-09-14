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
package org.apache.mina.filter.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.apache.mina.util.AvailablePortFinder;
import org.easymock.IArgumentMatcher;
import org.easymock.classextension.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Add documentation
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractStreamWriteFilterTest<M, U extends AbstractStreamWriteFilter<M>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStreamWriteFilterTest.class);

    protected final IoSession session = new DummySession();

    abstract protected U createFilter();
    
    abstract protected M createMessage(byte[] data) throws Exception;
    
    @Test
    public void testWriteEmptyFile() throws Exception {
        AbstractStreamWriteFilter<M> filter = createFilter();
        M message = createMessage(new byte[0]);

        WriteRequest writeRequest = new DefaultWriteRequest(message,
                new DummyWriteFuture());

        NextFilter nextFilter = EasyMock.createMock(NextFilter.class);
       /*
         * Record expectations
         */
        nextFilter.messageSent(session, writeRequest);

        /*
         * Replay.
         */
        EasyMock.replay(nextFilter);

        filter.filterWrite(nextFilter, session, writeRequest);

        /*
         * Verify.
         */
        EasyMock.verify(nextFilter);

        assertTrue(writeRequest.getFuture().isWritten());
    }

    /**
     * Tests that the filter just passes objects which aren't FileRegion's
     * through to the next filter.
     *
     * @throws Exception when something goes wrong
     */
    @Test
    public void testWriteNonFileRegionMessage() throws Exception {
        AbstractStreamWriteFilter<M> filter = createFilter();

        Object message = new Object();
        WriteRequest writeRequest = new DefaultWriteRequest(message,
                new DummyWriteFuture());

        NextFilter nextFilter = EasyMock.createMock(NextFilter.class);
        /*
         * Record expectations
         */
        nextFilter.filterWrite(session, writeRequest);
        nextFilter.messageSent(session, writeRequest);

        /*
         * Replay.
         */
        EasyMock.replay(nextFilter);

        filter.filterWrite(nextFilter, session, writeRequest);
        filter.messageSent(nextFilter, session, writeRequest);

        /*
         * Verify.
         */
        EasyMock.verify(nextFilter);
    }

    /**
     * Tests when the contents of the file fits into one write buffer.
     *
     * @throws Exception when something goes wrong
     */
    @Test
    public void testWriteSingleBufferFile() throws Exception {
        byte[] data = new byte[] { 1, 2, 3, 4 };

        AbstractStreamWriteFilter<M> filter = createFilter();
        M message = createMessage(data);

        WriteRequest writeRequest = new DefaultWriteRequest(message,
                new DummyWriteFuture());

        NextFilter nextFilter = EasyMock.createMock(NextFilter.class);
        /*
         * Record expectations
         */
        nextFilter.filterWrite(EasyMock.eq(session), eqWriteRequest(new DefaultWriteRequest(IoBuffer
                .wrap(data))));
        nextFilter.messageSent(session, writeRequest);

        /*
         * Replay.
         */
        EasyMock.replay(nextFilter);

        filter.filterWrite(nextFilter, session, writeRequest);
        filter.messageSent(nextFilter, session, writeRequest);

        /*
         * Verify.
         */
        EasyMock.verify(nextFilter);

        assertTrue(writeRequest.getFuture().isWritten());
    }

    /**
     * Tests when the contents of the file doesn't fit into one write buffer.
     *
     * @throws Exception when something goes wrong
     */
    @Test
    public void testWriteSeveralBuffersStream() throws Exception {
        AbstractStreamWriteFilter<M> filter = createFilter();
        filter.setWriteBufferSize(4);

        byte[] data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        byte[] chunk1 = new byte[] { 1, 2, 3, 4 };
        byte[] chunk2 = new byte[] { 5, 6, 7, 8 };
        byte[] chunk3 = new byte[] { 9, 10 };

        M message = createMessage(data);
        WriteRequest writeRequest = new DefaultWriteRequest(message,
                new DummyWriteFuture());

        WriteRequest chunk1Request = new DefaultWriteRequest(IoBuffer
                .wrap(chunk1));
        WriteRequest chunk2Request = new DefaultWriteRequest(IoBuffer
                .wrap(chunk2));
        WriteRequest chunk3Request = new DefaultWriteRequest(IoBuffer
                .wrap(chunk3));

        NextFilter nextFilter = EasyMock.createMock(NextFilter.class);
        /*
         * Record expectations
         */
        nextFilter.filterWrite(EasyMock.eq(session), eqWriteRequest(chunk1Request));
        nextFilter.filterWrite(EasyMock.eq(session), eqWriteRequest(chunk2Request));
        nextFilter.filterWrite(EasyMock.eq(session), eqWriteRequest(chunk3Request));
        nextFilter.messageSent(EasyMock.eq(session), eqWriteRequest(writeRequest));

        /*
         * Replay.
         */
        EasyMock.replay(nextFilter);

        filter.filterWrite(nextFilter, session, writeRequest);
        filter.messageSent(nextFilter, session, chunk1Request);
        filter.messageSent(nextFilter, session, chunk2Request);
        filter.messageSent(nextFilter, session, chunk3Request);

        /*
         * Verify.
         */
        EasyMock.verify(nextFilter);

        assertTrue(writeRequest.getFuture().isWritten());
    }

    @Test
    public void testWriteWhileWriteInProgress() throws Exception {
        AbstractStreamWriteFilter<M> filter = createFilter();
        M message = createMessage(new byte[5]);

        Queue<WriteRequest> queue = new LinkedList<WriteRequest>();

        /*
         * Make up the situation.
         */
        session.setAttribute(filter.CURRENT_STREAM, message);
        session.setAttribute(filter.WRITE_REQUEST_QUEUE, queue);

        NextFilter nextFilter = EasyMock.createMock(NextFilter.class);
        /*
         * Replay.  (We recorded *nothing* because nothing should occur.)
         */
        EasyMock.replay(nextFilter);

        WriteRequest wr = new DefaultWriteRequest(new Object(),
                new DummyWriteFuture());
        filter.filterWrite(nextFilter, session, wr);
        assertEquals(1, queue.size());
        assertSame(wr, queue.poll());

        /*
         * Verify.
         */
        EasyMock.verify(nextFilter);

        session.removeAttribute(filter.CURRENT_STREAM);
        session.removeAttribute(filter.WRITE_REQUEST_QUEUE);
    }

    @Test
    public void testWritesWriteRequestQueueWhenFinished() throws Exception {
        AbstractStreamWriteFilter<M> filter = createFilter();
        M message = createMessage(new byte[0]);
        
        WriteRequest wrs[] = new WriteRequest[] {
                new DefaultWriteRequest(new Object(), new DummyWriteFuture()),
                new DefaultWriteRequest(new Object(), new DummyWriteFuture()),
                new DefaultWriteRequest(new Object(), new DummyWriteFuture()) };
        Queue<WriteRequest> queue = new LinkedList<WriteRequest>();
        queue.add(wrs[0]);
        queue.add(wrs[1]);
        queue.add(wrs[2]);

        /*
         * Make up the situation.
         */
        session.setAttribute(filter.CURRENT_STREAM, message);
        session.setAttribute(filter.CURRENT_WRITE_REQUEST,
                new DefaultWriteRequest(message));
        session.setAttribute(filter.WRITE_REQUEST_QUEUE, queue);

        /*
         * Record expectations
         */
        NextFilter nextFilter = EasyMock.createMock(NextFilter.class);
        nextFilter.filterWrite(session, wrs[0]);
        nextFilter.filterWrite(session, wrs[1]);
        nextFilter.filterWrite(session, wrs[2]);
        nextFilter.messageSent(EasyMock.eq(session), eqWriteRequest(new DefaultWriteRequest(message)));

        /*
         * Replay.
         */
        EasyMock.replay(nextFilter);

        filter.messageSent(nextFilter, session, new DefaultWriteRequest(
                new Object()));
        assertEquals(0, queue.size());

        /*
         * Verify.
         */
        EasyMock.verify(nextFilter);
    }

    /**
     * Tests that {@link StreamWriteFilter#setWriteBufferSize(int)} checks the
     * specified size.
     */
    @Test
    public void testSetWriteBufferSize() {
        AbstractStreamWriteFilter<M> filter = createFilter();

        try {
            filter.setWriteBufferSize(0);
            fail("0 writeBuferSize specified. IllegalArgumentException expected.");
        } catch (IllegalArgumentException iae) {
            // Pass, exception was thrown
            // Signifies a successful test execution
            assertTrue(true);
        }

        try {
            filter.setWriteBufferSize(-100);
            fail("Negative writeBuferSize specified. IllegalArgumentException expected.");
        } catch (IllegalArgumentException iae) {
            // Pass, exception was thrown
            // Signifies a successful test execution
            assertTrue(true);
        }

        filter.setWriteBufferSize(1);
        assertEquals(1, filter.getWriteBufferSize());
        filter.setWriteBufferSize(1024);
        assertEquals(1024, filter.getWriteBufferSize());
    }

    @Test
    public void testWriteUsingSocketTransport() throws Exception {
        NioSocketAcceptor acceptor = new NioSocketAcceptor();
        acceptor.setReuseAddress(true);
        SocketAddress address = new InetSocketAddress("localhost",
                AvailablePortFinder.getNextAvailable());

        NioSocketConnector connector = new NioSocketConnector();

        // Generate 4MB of random data
        byte[] data = new byte[4 * 1024 * 1024];
        new Random().nextBytes(data);

        byte[] expectedMd5 = MessageDigest.getInstance("MD5").digest(data);

        M message = createMessage(data);
        
        SenderHandler sender = new SenderHandler(message);
        ReceiverHandler receiver = new ReceiverHandler(data.length);

        acceptor.setHandler(sender);
        connector.setHandler(receiver);
        
        acceptor.bind(address);
        connector.connect(address);
        sender.latch.await();
        receiver.latch.await();

        acceptor.dispose();

        assertEquals(data.length, receiver.bytesRead);
        byte[] actualMd5 = receiver.digest.digest();
        assertEquals(expectedMd5.length, actualMd5.length);
        for (int i = 0; i < expectedMd5.length; i++) {
            assertEquals(expectedMd5[i], actualMd5[i]);
        }
    }

    private class SenderHandler extends IoHandlerAdapter {
        final CountDownLatch latch = new CountDownLatch(1);

        private M message;

        StreamWriteFilter streamWriteFilter = new StreamWriteFilter();

        SenderHandler(M message) {
            this.message = message;
        }

        @Override
        public void sessionCreated(IoSession session) throws Exception {
            super.sessionCreated(session);
            session.getFilterChain().addLast("codec", streamWriteFilter);
        }

        @Override
        public void sessionOpened(IoSession session) throws Exception {
            session.write(message);
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause)
                throws Exception {
            LOGGER.error("SenderHandler: exceptionCaught", cause);
            latch.countDown();
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            LOGGER.info("SenderHandler: sessionClosed");
            latch.countDown();
        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status)
                throws Exception {
            LOGGER.info("SenderHandler: sessionIdle");
            latch.countDown();
        }

        @Override
        public void messageSent(IoSession session, Object message)
                throws Exception {
            LOGGER.info("SenderHandler: messageSent");
            if (message == this.message) {
                LOGGER.info("message == this.message");
                latch.countDown();
            }
        }
    }

    private static class ReceiverHandler extends IoHandlerAdapter {
        final CountDownLatch latch = new CountDownLatch(1);

        long bytesRead = 0;

        long size = 0;

        MessageDigest digest;

        ReceiverHandler(long size) throws Exception {
            this.size = size;
            digest = MessageDigest.getInstance("MD5");
        }

        @Override
        public void sessionCreated(IoSession session) throws Exception {
            super.sessionCreated(session);

            session.getConfig().setIdleTime(IdleStatus.READER_IDLE, 5);
        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status)
                throws Exception {
            LOGGER.info("ReceiverHandler: sessionIdle");
            session.close(true);
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause)
                throws Exception {
            LOGGER.error("ReceiverHandler: exceptionCaught", cause);
            latch.countDown();
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            LOGGER.info("ReceiverHandler: sessionClosed");
            latch.countDown();
        }

        @Override
        public void messageReceived(IoSession session, Object message)
                throws Exception {
            LOGGER.info("messageReceived");
            IoBuffer buf = (IoBuffer) message;
            while (buf.hasRemaining()) {
                digest.update(buf.get());
                bytesRead++;
            }
            LOGGER.info("messageReceived: bytesRead = {}", bytesRead);
            if (bytesRead >= size) {
                session.close(true);
            }
        }
    }

    public static WriteRequest eqWriteRequest(WriteRequest expected) {
        EasyMock.reportMatcher(new WriteRequestMatcher(expected));
        return null;
    }
    
    private static class WriteRequestMatcher implements IArgumentMatcher {
        private final WriteRequest expected;
        
        public WriteRequestMatcher(WriteRequest expected) {
                this.expected = expected;
        }
        
        public boolean matches(Object actual) {
            if (actual instanceof WriteRequest) {
                WriteRequest w2 = (WriteRequest) actual;

                return expected.getMessage().equals(w2.getMessage())
                        && expected.getFuture().isWritten() == w2.getFuture()
                        .isWritten();
            }
            return false;
        }
        public void appendTo(StringBuffer buffer) {
                buffer.append("Expected a WriteRequest with the message '").append(expected.getMessage()).append("'");
        }
    }

    private static class DummyWriteFuture implements WriteFuture {
        private boolean written;

        /**
         * Default constructor
         */
        public DummyWriteFuture() {
            super();
        }
        
        public boolean isWritten() {
            return written;
        }

        public void setWritten() {
            this.written = true;
        }

        public IoSession getSession() {
            return null;
        }

        public Object getLock() {
            return this;
        }

        public void join() {
            // Do nothing
        }

        public boolean join(long timeoutInMillis) {
            return true;
        }

        public boolean isDone() {
            return true;
        }

        public WriteFuture addListener(IoFutureListener<?> listener) {
            return this;
        }

        public WriteFuture removeListener(IoFutureListener<?> listener) {
            return this;
        }

        public WriteFuture await() throws InterruptedException {
            return this;
        }

        public boolean await(long timeout, TimeUnit unit)
                throws InterruptedException {
            return true;
        }

        public boolean await(long timeoutMillis) throws InterruptedException {
            return true;
        }

        public WriteFuture awaitUninterruptibly() {
            return this;
        }

        public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
            return true;
        }

        public boolean awaitUninterruptibly(long timeoutMillis) {
            return true;
        }

        public Throwable getException() {
            return null;
        }

        public void setException(Throwable cause) {
            throw new IllegalStateException();
        }
    }

    
}
