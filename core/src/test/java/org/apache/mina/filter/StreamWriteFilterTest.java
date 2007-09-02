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
package org.apache.mina.filter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoFilter.NextFilter;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.common.support.DefaultWriteFuture;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.util.AvailablePortFinder;
import org.easymock.AbstractMatcher;
import org.easymock.MockControl;

import junit.framework.TestCase;

/**
 * Tests {@link StreamWriteFilter}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class StreamWriteFilterTest extends TestCase {
    MockControl mockSession;

    MockControl mockNextFilter;

    IoSession session;

    NextFilter nextFilter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        /*
         * Create the mocks.
         */
        mockSession = MockControl.createControl(IoSession.class);
        mockNextFilter = MockControl.createControl(NextFilter.class);
        session = (IoSession) mockSession.getMock();
        nextFilter = (NextFilter) mockNextFilter.getMock();

        session.getAttribute(StreamWriteFilter.CURRENT_STREAM);
        mockSession.setReturnValue(null);
    }

    public void testWriteEmptyStream() throws Exception {
        StreamWriteFilter filter = new StreamWriteFilter();

        InputStream stream = new ByteArrayInputStream(new byte[0]);
        WriteRequest writeRequest = new WriteRequest(stream,
                new DummyWriteFuture());

        /*
         * Record expectations
         */
        nextFilter.messageSent(session, stream);

        /*
         * Replay.
         */
        mockNextFilter.replay();
        mockSession.replay();

        filter.filterWrite(nextFilter, session, writeRequest);

        /*
         * Verify.
         */
        mockNextFilter.verify();
        mockSession.verify();

        assertTrue(writeRequest.getFuture().isWritten());
    }

    /**
     * Tests that the filter just passes objects which aren't InputStreams
     * through to the next filter.
     */
    public void testWriteNonStreamMessage() throws Exception {
        StreamWriteFilter filter = new StreamWriteFilter();

        Object message = new Object();
        WriteRequest writeRequest = new WriteRequest(message,
                new DummyWriteFuture());

        /*
         * Record expectations
         */
        nextFilter.filterWrite(session, writeRequest);
        session.getAttribute(StreamWriteFilter.CURRENT_STREAM);
        mockSession.setReturnValue(null);
        nextFilter.messageSent(session, message);

        /*
         * Replay.
         */
        mockNextFilter.replay();
        mockSession.replay();

        filter.filterWrite(nextFilter, session, writeRequest);
        filter.messageSent(nextFilter, session, message);

        /*
         * Verify.
         */
        mockNextFilter.verify();
        mockSession.verify();
    }

    /**
     * Tests when the contents of the stream fits into one write buffer.
     */
    public void testWriteSingleBufferStream() throws Exception {
        StreamWriteFilter filter = new StreamWriteFilter();

        byte[] data = new byte[] { 1, 2, 3, 4 };

        InputStream stream = new ByteArrayInputStream(data);
        WriteRequest writeRequest = new WriteRequest(stream,
                new DummyWriteFuture());

        /*
         * Record expectations
         */
        session.setAttribute(StreamWriteFilter.CURRENT_STREAM, stream);
        mockSession.setReturnValue(null);
        session.setAttribute(StreamWriteFilter.INITIAL_WRITE_FUTURE,
                writeRequest.getFuture());
        mockSession.setReturnValue(null);
        nextFilter
                .filterWrite(session, new WriteRequest(ByteBuffer.wrap(data)));
        mockNextFilter.setMatcher(new WriteRequestMatcher());

        session.getAttribute(StreamWriteFilter.CURRENT_STREAM);
        mockSession.setReturnValue(stream);
        session.removeAttribute(StreamWriteFilter.CURRENT_STREAM);
        mockSession.setReturnValue(stream);
        session.removeAttribute(StreamWriteFilter.INITIAL_WRITE_FUTURE);
        mockSession.setReturnValue(writeRequest.getFuture());
        session.removeAttribute(StreamWriteFilter.WRITE_REQUEST_QUEUE);
        mockSession.setReturnValue(null);
        nextFilter.messageSent(session, stream);

        /*
         * Replay.
         */
        mockNextFilter.replay();
        mockSession.replay();

        filter.filterWrite(nextFilter, session, writeRequest);
        filter.messageSent(nextFilter, session, data);

        /*
         * Verify.
         */
        mockNextFilter.verify();
        mockSession.verify();

        assertTrue(writeRequest.getFuture().isWritten());
    }

    /**
     * Tests when the contents of the stream doesn't fit into one write buffer.
     */
    public void testWriteSeveralBuffersStream() throws Exception {
        StreamWriteFilter filter = new StreamWriteFilter();
        filter.setWriteBufferSize(4);

        byte[] data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        byte[] chunk1 = new byte[] { 1, 2, 3, 4 };
        byte[] chunk2 = new byte[] { 5, 6, 7, 8 };
        byte[] chunk3 = new byte[] { 9, 10 };

        InputStream stream = new ByteArrayInputStream(data);
        WriteRequest writeRequest = new WriteRequest(stream,
                new DummyWriteFuture());

        /*
         * Record expectations
         */
        session.setAttribute(StreamWriteFilter.CURRENT_STREAM, stream);
        mockSession.setReturnValue(null);
        session.setAttribute(StreamWriteFilter.INITIAL_WRITE_FUTURE,
                writeRequest.getFuture());
        mockSession.setReturnValue(null);
        nextFilter.filterWrite(session, new WriteRequest(ByteBuffer
                .wrap(chunk1)));
        mockNextFilter.setMatcher(new WriteRequestMatcher());

        session.getAttribute(StreamWriteFilter.CURRENT_STREAM);
        mockSession.setReturnValue(stream);
        nextFilter.filterWrite(session, new WriteRequest(ByteBuffer
                .wrap(chunk2)));

        session.getAttribute(StreamWriteFilter.CURRENT_STREAM);
        mockSession.setReturnValue(stream);
        nextFilter.filterWrite(session, new WriteRequest(ByteBuffer
                .wrap(chunk3)));

        session.getAttribute(StreamWriteFilter.CURRENT_STREAM);
        mockSession.setReturnValue(stream);
        session.removeAttribute(StreamWriteFilter.CURRENT_STREAM);
        mockSession.setReturnValue(stream);
        session.removeAttribute(StreamWriteFilter.INITIAL_WRITE_FUTURE);
        mockSession.setReturnValue(writeRequest.getFuture());
        session.removeAttribute(StreamWriteFilter.WRITE_REQUEST_QUEUE);
        mockSession.setReturnValue(null);
        nextFilter.messageSent(session, stream);

        /*
         * Replay.
         */
        mockNextFilter.replay();
        mockSession.replay();

        filter.filterWrite(nextFilter, session, writeRequest);
        filter.messageSent(nextFilter, session, chunk1);
        filter.messageSent(nextFilter, session, chunk2);
        filter.messageSent(nextFilter, session, chunk3);

        /*
         * Verify.
         */
        mockNextFilter.verify();
        mockSession.verify();

        assertTrue(writeRequest.getFuture().isWritten());
    }

    public void testWriteWhileWriteInProgress() throws Exception {
        StreamWriteFilter filter = new StreamWriteFilter();

        Queue<? extends Object> queue = new LinkedList<Object>();
        InputStream stream = new ByteArrayInputStream(new byte[5]);

        /*
         * Record expectations
         */
        mockSession.reset();
        session.getAttribute(StreamWriteFilter.CURRENT_STREAM);
        mockSession.setReturnValue(stream);
        session.getAttribute(StreamWriteFilter.WRITE_REQUEST_QUEUE);
        mockSession.setReturnValue(queue);

        /*
         * Replay.
         */
        mockNextFilter.replay();
        mockSession.replay();

        WriteRequest wr = new WriteRequest(new Object(), new DummyWriteFuture());
        filter.filterWrite(nextFilter, session, wr);
        assertEquals(1, queue.size());
        assertSame(wr, queue.poll());

        /*
         * Verify.
         */
        mockNextFilter.verify();
        mockSession.verify();
    }

    public void testWritesWriteRequestQueueWhenFinished() throws Exception {
        StreamWriteFilter filter = new StreamWriteFilter();

        WriteRequest wrs[] = new WriteRequest[] {
                new WriteRequest(new Object(), new DummyWriteFuture()),
                new WriteRequest(new Object(), new DummyWriteFuture()),
                new WriteRequest(new Object(), new DummyWriteFuture()) };
        Queue<WriteRequest> queue = new LinkedList<WriteRequest>();
        queue.add(wrs[0]);
        queue.add(wrs[1]);
        queue.add(wrs[2]);
        InputStream stream = new ByteArrayInputStream(new byte[0]);

        /*
         * Record expectations
         */
        mockSession.reset();

        session.getAttribute(StreamWriteFilter.CURRENT_STREAM);
        mockSession.setReturnValue(stream);
        session.removeAttribute(StreamWriteFilter.CURRENT_STREAM);
        mockSession.setReturnValue(stream);
        session.removeAttribute(StreamWriteFilter.INITIAL_WRITE_FUTURE);
        mockSession.setReturnValue(new DefaultWriteFuture(session));
        session.removeAttribute(StreamWriteFilter.WRITE_REQUEST_QUEUE);
        mockSession.setReturnValue(queue);

        nextFilter.filterWrite(session, wrs[0]);
        session.getAttribute(StreamWriteFilter.CURRENT_STREAM);
        mockSession.setReturnValue(null);
        nextFilter.filterWrite(session, wrs[1]);
        session.getAttribute(StreamWriteFilter.CURRENT_STREAM);
        mockSession.setReturnValue(null);
        nextFilter.filterWrite(session, wrs[2]);
        session.getAttribute(StreamWriteFilter.CURRENT_STREAM);
        mockSession.setReturnValue(null);

        nextFilter.messageSent(session, stream);

        /*
         * Replay.
         */
        mockNextFilter.replay();
        mockSession.replay();

        filter.messageSent(nextFilter, session, new Object());
        assertEquals(0, queue.size());

        /*
         * Verify.
         */
        mockNextFilter.verify();
        mockSession.verify();
    }

    /**
     * Tests that {@link StreamWriteFilter#setWriteBufferSize(int)} checks the
     * specified size.
     */
    public void testSetWriteBufferSize() throws Exception {
        StreamWriteFilter filter = new StreamWriteFilter();

        try {
            filter.setWriteBufferSize(0);
            fail("0 writeBuferSize specified. IllegalArgumentException expected.");
        } catch (IllegalArgumentException iae) {
        }

        try {
            filter.setWriteBufferSize(-100);
            fail("Negative writeBuferSize specified. IllegalArgumentException expected.");
        } catch (IllegalArgumentException iae) {
        }

        filter.setWriteBufferSize(1);
        assertEquals(1, filter.getWriteBufferSize());
        filter.setWriteBufferSize(1024);
        assertEquals(1024, filter.getWriteBufferSize());
    }

    public void testWriteUsingSocketTransport() throws Exception {
        IoAcceptor acceptor = new SocketAcceptor();
        ((SocketAcceptorConfig) acceptor.getDefaultConfig())
                .setReuseAddress(true);
        SocketAddress address = new InetSocketAddress("localhost",
                AvailablePortFinder.getNextAvailable());

        IoConnector connector = new SocketConnector();

        FixedRandomInputStream stream = new FixedRandomInputStream(
                4 * 1024 * 1024);

        SenderHandler sender = new SenderHandler(stream);
        ReceiverHandler receiver = new ReceiverHandler(stream.size);

        acceptor.bind(address, sender);

        connector.connect(address, receiver);
        sender.latch.await();
        receiver.latch.await();

        acceptor.unbind(address);

        assertEquals(stream.bytesRead, receiver.bytesRead);
        assertEquals(stream.size, receiver.bytesRead);
        byte[] expectedMd5 = stream.digest.digest();
        byte[] actualMd5 = receiver.digest.digest();
        assertEquals(expectedMd5.length, actualMd5.length);
        for (int i = 0; i < expectedMd5.length; i++) {
            assertEquals(expectedMd5[i], actualMd5[i]);
        }
    }

    private static class FixedRandomInputStream extends InputStream {
        long size;

        long bytesRead = 0;

        Random random = new Random();

        MessageDigest digest;

        FixedRandomInputStream(long size) throws Exception {
            this.size = size;
            digest = MessageDigest.getInstance("MD5");
        }

        @Override
        public int read() throws IOException {
            if (isAllWritten())
                return -1;
            bytesRead++;
            byte b = (byte) random.nextInt(255);
            digest.update(b);
            return b;
        }

        public long getBytesRead() {
            return bytesRead;
        }

        public long getSize() {
            return size;
        }

        public boolean isAllWritten() {
            return bytesRead >= size;
        }
    }

    private static class SenderHandler extends IoHandlerAdapter {
        final CountDownLatch latch = new CountDownLatch( 1 );

        InputStream inputStream;

        StreamWriteFilter streamWriteFilter = new StreamWriteFilter();

        SenderHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void sessionCreated(IoSession session) throws Exception {
            super.sessionCreated(session);
            session.getFilterChain().addLast("codec", streamWriteFilter);
        }

        @Override
        public void sessionOpened(IoSession session) throws Exception {
            session.write(inputStream);
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause)
                throws Exception {
            latch.countDown();
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            latch.countDown();
        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status)
                throws Exception {
            latch.countDown();
        }

        @Override
        public void messageSent(IoSession session, Object message)
                throws Exception {
            if (message == inputStream) {
                latch.countDown();
            }
        }
    }

    private static class ReceiverHandler extends IoHandlerAdapter {
        final CountDownLatch latch = new CountDownLatch( 1 );

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

            session.setIdleTime(IdleStatus.READER_IDLE, 5);
        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status)
                throws Exception {
            session.close();
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause)
                throws Exception {
            latch.countDown();
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            latch.countDown();
        }

        @Override
        public void messageReceived(IoSession session, Object message)
                throws Exception {
            ByteBuffer buf = (ByteBuffer) message;
            while (buf.hasRemaining()) {
                digest.update(buf.get());
                bytesRead++;
            }
            if (bytesRead >= size) {
                session.close();
            }
        }
    }

    public static class WriteRequestMatcher extends AbstractMatcher {
        @Override
        protected boolean argumentMatches(Object expected, Object actual) {
            if (expected instanceof WriteRequest
                    && actual instanceof WriteRequest) {
                WriteRequest w1 = (WriteRequest) expected;
                WriteRequest w2 = (WriteRequest) actual;

                return w1.getMessage().equals(w2.getMessage())
                        && w1.getFuture().isWritten() == w2.getFuture()
                                .isWritten();
            }
            return super.argumentMatches(expected, actual);
        }
    }

    private static class DummyWriteFuture implements WriteFuture {
        private boolean written;

        public boolean isWritten() {
            return written;
        }

        public void setWritten(boolean written) {
            this.written = written;
        }

        public IoSession getSession() {
            return null;
        }

        public Object getLock() {
            return this;
        }

        public void join() {
        }

        public boolean join(long timeoutInMillis) {
            return true;
        }

        public boolean isReady() {
            return true;
        }

        public void addListener(IoFutureListener listener) {
        }

        public void removeListener(IoFutureListener listener) {
        }
    }
}
