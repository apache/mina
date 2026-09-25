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

package org.apache.mina.filter.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.FilterEvent;
import org.junit.Before;
import org.junit.Test;

/**
 * A test for DIRMINA-1019
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
abstract class AbstractNextFilter implements NextFilter {
    public abstract void messageReceived(IoSession session, Object message);
    
    public abstract void filterWrite(IoSession session, WriteRequest writeRequest);
    
    // Following are unimplemented as they aren't used in test
    public void sessionCreated(IoSession session) { }

    public void sessionOpened(IoSession session) { }

    public void sessionClosed(IoSession session) { }

    public void sessionIdle(IoSession session, IdleStatus status) { }

    public void exceptionCaught(IoSession session, Throwable cause) { }

    public void inputClosed(IoSession session) { }

    public void messageSent(IoSession session, WriteRequest writeRequest) { }

    public void filterClose(IoSession session) { }

    public void event(IoSession session, FilterEvent event) { }

    public String toString() {
        return null;
    }
};

/**
 * Tests for the {@code SslFilter}
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SslFilterTest {
    SslHandler test_class;

    @Before
    public void init() {
        test_class = new SslHandler(null, new DummySession());
        test_class.setCCCEnabled(true);
        test_class.setDisabled(false);
    }

    @Test
    public void testFlushRaceCondition() {
        final ExecutorService executor = Executors.newFixedThreadPool(1);
        final List<Object> message_received_messages = new ArrayList<>();
        final List<WriteRequest> filter_write_requests = new ArrayList<>();

        final AbstractNextFilter write_filter = new AbstractNextFilter()
        {
            @Override
            public void messageReceived(IoSession session, Object message) { }

            @Override
            public void filterWrite(IoSession session, WriteRequest writeRequest) {
                filter_write_requests.add(writeRequest);
            }
        };
        
        AbstractNextFilter receive_filter = new AbstractNextFilter()
        {
            @Override
            public void messageReceived(IoSession session, Object message) {
                message_received_messages.add(message);
                
                // This is where the race condition occurs. If a thread calls SslHandler.scheduleFilterWrite(),
                // followed by SslHandler.flushScheduledEvents(), the queued event will not be processed as
                // the current thread owns the SslHandler.sslLock and has already "dequeued" all the queued
                // filterWriteEventQueue.
                Future<?> write_scheduler = executor.submit(new Runnable() {
                    public void run() {
                        synchronized(test_class) {
                            test_class.scheduleFilterWrite(write_filter, new DefaultWriteRequest(new byte[] {}));
                            test_class.flushFilterWrite();
                        }
                    }
                });
                
                try {
                    write_scheduler.get();
                } catch (Exception e) { }
            }

            @Override
            public void filterWrite(IoSession session, WriteRequest writeRequest) { }
        };
        
        synchronized(test_class) {
            test_class.scheduleMessageReceived(receive_filter, new byte[] {});
        }
        
        test_class.flushMessageReceived();
        
        assertEquals(1, message_received_messages.size());
        assertEquals(1, filter_write_requests.size());
    }

    @Test
    public void testIsSslActiveScenarios() throws NoSuchAlgorithmException, SSLException {
        final SslFilter filter = new SslFilter(SSLContext.getDefault());
        final IoSession dummySession = new DummySession();

        // Scenario 1: No SSL handler attribute
        assertFalse(filter.isSslActive(dummySession));

        // Scenario 2: SSL handler present but disabled
        final SslHandler disabledHandler = new SslHandler(filter, dummySession);
        dummySession.setAttribute(SslFilter.SSL_HANDLER, disabledHandler);
        disabledHandler.setDisabled(true);
        assertFalse(filter.isSslActive(dummySession));

        // Scenario 3: SSL handler present, enabled, and initialized
        final SslHandler enabledHandler = new SslHandler(filter, dummySession);
        enabledHandler.init();
        dummySession.setAttribute(SslFilter.SSL_HANDLER, enabledHandler);
        assertTrue(filter.isSslActive(dummySession));

        // Scenario 4: SSL handler removed
        dummySession.removeAttribute(SslFilter.SSL_HANDLER);
        assertFalse(filter.isSslActive(dummySession));
    }

    // ------------------------------------------------------------------------
    // Tests for the SslFilter CCC / close-notify public API
    // ------------------------------------------------------------------------

    /**
     * {@link SslFilter#enableCCC(IoSession)} must mark the underlying handler
     * as CCC enabled.
     */
    @Test
    public void testEnableCCC() throws NoSuchAlgorithmException {
        final SslFilter filter = new SslFilter(SSLContext.getDefault());
        final IoSession session = new DummySession();
        final SslHandler handler = new SslHandler(filter, session);
        session.setAttribute(SslFilter.SSL_HANDLER, handler);

        assertFalse(handler.isCCCEnabled());

        filter.enableCCC(session);

        assertTrue(handler.isCCCEnabled());
    }

    /**
     * {@link SslFilter#stopSslWithoutCloseNotify(IoSession)} must disable the
     * underlying handler, which in turn makes SSL inactive.
     */
    @Test
    public void testStopSslWithoutCloseNotify() throws NoSuchAlgorithmException, SSLException {
        final SslFilter filter = new SslFilter(SSLContext.getDefault());
        final IoSession session = new DummySession();
        final SslHandler handler = new SslHandler(filter, session);
        handler.init();
        session.setAttribute(SslFilter.SSL_HANDLER, handler);

        assertTrue("SSL should be active before it is stopped", filter.isSslActive(session));

        filter.stopSslWithoutCloseNotify(session);

        assertTrue("The handler must be disabled", handler.isDisabled());
        assertFalse("SSL must no longer be active once disabled", filter.isSslActive(session));
    }

    /**
     * {@link SslFilter#getCccLock(IoSession)} must return the exact same lock
     * object owned by the session's handler.
     */
    @Test
    public void testGetCccLockReturnsHandlerLock() throws NoSuchAlgorithmException {
        final SslFilter filter = new SslFilter(SSLContext.getDefault());
        final IoSession session = new DummySession();
        final SslHandler handler = new SslHandler(filter, session);
        session.setAttribute(SslFilter.SSL_HANDLER, handler);

        assertSame(handler.getCccLock(), filter.getCccLock(session));
    }

    /**
     * The CCC / close-notify API methods rely on
     * {@code getSslSessionHandler(IoSession)}, which must throw an
     * {@link IllegalStateException} when there is no handler attached to the
     * session.
     */
    @Test(expected = IllegalStateException.class)
    public void testEnableCCCWithoutHandlerThrows() throws NoSuchAlgorithmException {
        final SslFilter filter = new SslFilter(SSLContext.getDefault());

        filter.enableCCC(new DummySession());
    }

    /**
     * When the session's handler belongs to a different filter, the CCC API
     * must reject the call with an {@link IllegalArgumentException}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetCccLockWithForeignHandlerThrows() throws NoSuchAlgorithmException {
        final SslFilter filter = new SslFilter(SSLContext.getDefault());
        final SslFilter otherFilter = new SslFilter(SSLContext.getDefault());
        final IoSession session = new DummySession();

        // The handler is managed by 'otherFilter', not 'filter'
        final SslHandler foreignHandler = new SslHandler(otherFilter, session);
        session.setAttribute(SslFilter.SSL_HANDLER, foreignHandler);

        filter.getCccLock(session);
    }

    /**
     * The {@code checkStatus} handling of a {@code close_notify} must not close
     * the session while CCC is enabled. We assert the behavioral contract via
     * the handler flags exposed by the commit.
     */
    @Test
    public void testCccEnabledKeepsHandlerActive() throws NoSuchAlgorithmException, SSLException {
        final SslFilter filter = new SslFilter(SSLContext.getDefault());
        final IoSession session = new DummySession();
        final SslHandler handler = new SslHandler(filter, session);
        handler.init();
        session.setAttribute(SslFilter.SSL_HANDLER, handler);

        filter.enableCCC(session);

        assertTrue("CCC enabled handler is still considered active", filter.isSslActive(session));
        assertTrue(handler.isCCCEnabled());
        assertFalse(handler.isDisabled());
    }

    // ------------------------------------------------------------------------
    // Regression tests: messageReceived / filterWrite behaviour must be
    // unchanged when CCC is NOT enabled (and the handler is not disabled).
    // ------------------------------------------------------------------------

    /**
     * Builds a {@link NextFilter} that records every {@code filterWrite} request
     * and every {@code messageReceived} message it is handed.
     */
    private static NextFilter recordingNextFilter(final List<Object> received, final List<WriteRequest> written) {
        return new AbstractNextFilter() {
            @Override
            public void messageReceived(IoSession session, Object message) {
                received.add(message);
            }

            @Override
            public void filterWrite(IoSession session, WriteRequest writeRequest) {
                written.add(writeRequest);
            }
        };
    }

    /**
     * When SSL has not been started yet (no {@code SSLEngine} created) and CCC
     * is not enabled, {@link SslFilter#filterWrite} must forward the plaintext
     * write request straight to the next filter, exactly as before the commit.
     */
    @Test
    public void testFilterWriteForwardsWhenSslNotStarted() throws NoSuchAlgorithmException, SSLException {
        final SslFilter filter = new SslFilter(SSLContext.getDefault());
        final IoSession session = new DummySession();
        // Not initialized => SSL not started, not disabled, CCC not enabled
        final SslHandler handler = new SslHandler(filter, session);
        session.setAttribute(SslFilter.SSL_HANDLER, handler);

        assertFalse(handler.isCCCEnabled());
        assertFalse(handler.isDisabled());
        assertFalse(filter.isSslStarted(session));

        final List<Object> received = new ArrayList<>();
        final List<WriteRequest> written = new ArrayList<>();
        final NextFilter next = recordingNextFilter(received, written);

        final WriteRequest request = new DefaultWriteRequest(IoBuffer.wrap(new byte[] { 1, 2, 3 }));

        filter.filterWrite(next, session, request);

        // Unchanged behaviour: the write is passed through untouched.
        assertEquals(1, written.size());
        assertSame(request, written.get(0));
    }

    /**
     * When {@link SslFilter#DISABLE_ENCRYPTION_ONCE} is set and CCC is not
     * enabled, {@link SslFilter#filterWrite} must forward the request without
     * encrypting it and clear the temporary marker attribute afterwards. This
     * is the StartTLS bypass path that must remain unchanged.
     */
    @Test
    public void testFilterWriteBypassesWithDisableEncryptionOnce() throws NoSuchAlgorithmException, SSLException {
        final SslFilter filter = new SslFilter(SSLContext.getDefault());
        final IoSession session = new DummySession();
        final SslHandler handler = new SslHandler(filter, session);
        handler.init(); // SSL started
        session.setAttribute(SslFilter.SSL_HANDLER, handler);
        session.setAttribute(SslFilter.DISABLE_ENCRYPTION_ONCE, Boolean.TRUE);

        assertFalse(handler.isCCCEnabled());
        assertFalse(handler.isDisabled());

        final List<Object> received = new ArrayList<>();
        final List<WriteRequest> written = new ArrayList<>();
        final NextFilter next = recordingNextFilter(received, written);

        final WriteRequest request = new DefaultWriteRequest(IoBuffer.wrap(new byte[] { 1, 2, 3 }));

        filter.filterWrite(next, session, request);

        // Unchanged behaviour: forwarded unencrypted and the marker is removed.
        assertEquals(1, written.size());
        assertSame(request, written.get(0));
        assertFalse(session.containsAttribute(SslFilter.DISABLE_ENCRYPTION_ONCE));
    }

    /**
     * While the handshake is still in progress and CCC is not enabled,
     * {@link SslFilter#filterWrite} must queue the write as a pre-handshake
     * request instead of forwarding it to the next filter. This deferral must
     * remain unchanged by the CCC changes.
     */
    @Test
    public void testFilterWriteDefersDuringHandshake() throws NoSuchAlgorithmException, SSLException {
        final SslFilter filter = new SslFilter(SSLContext.getDefault());
        final IoSession session = new DummySession();
        final SslHandler handler = new SslHandler(filter, session);
        handler.init(); // SSL started, handshake not complete

        session.setAttribute(SslFilter.SSL_HANDLER, handler);

        assertFalse(handler.isHandshakeComplete());
        assertFalse(handler.isCCCEnabled());
        assertFalse(handler.isDisabled());

        final List<Object> received = new ArrayList<>();
        final List<WriteRequest> written = new ArrayList<>();
        final NextFilter next = recordingNextFilter(received, written);

        final WriteRequest request = new DefaultWriteRequest(IoBuffer.wrap(new byte[] { 1, 2, 3 }));

        filter.filterWrite(next, session, request);

        // Unchanged behaviour: nothing is forwarded until the handshake completes.
        assertTrue(written.isEmpty());
    }

    /**
     * Once the engine is fully closed (inbound and outbound done) and CCC is not
     * enabled, {@link SslFilter#messageReceived} must push the raw message to
     * the next filter as-is, without attempting to decrypt it. This pass-through
     * path must remain unchanged by the CCC changes.
     */
    @Test
    public void testMessageReceivedForwardsWhenSessionClosed() throws NoSuchAlgorithmException, SSLException {
        final SslFilter filter = new SslFilter(SSLContext.getDefault());
        final IoSession session = new DummySession();
        // Not initialized => sslEngine null => both inbound and outbound "done"
        final SslHandler handler = new SslHandler(filter, session);
        session.setAttribute(SslFilter.SSL_HANDLER, handler);

        assertTrue(handler.isInboundDone());
        assertTrue(handler.isOutboundDone());
        assertFalse(handler.isCCCEnabled());
        assertFalse(handler.isDisabled());

        final List<Object> received = new ArrayList<>();
        final List<WriteRequest> written = new ArrayList<>();
        final NextFilter next = recordingNextFilter(received, written);

        final IoBuffer message = IoBuffer.wrap(new byte[] { 1, 2, 3 });

        filter.messageReceived(next, session, message);

        // Unchanged behaviour: the raw message is forwarded untouched.
        assertEquals(1, received.size());
        assertSame(message, received.get(0));
    }
}
