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
package org.apache.mina.filter.reqres;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;
import org.easymock.AbstractMatcher;
import org.easymock.MockControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link RequestResponseFilter}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class RequestResponseFilterTest {

    private ScheduledExecutorService scheduler;

    private RequestResponseFilter filter;

    private IoSession session;

    private IoFilterChain chain;

    private NextFilter nextFilter;

    private MockControl nextFilterControl;

    private final WriteRequestMatcher matcher = new WriteRequestMatcher();

    @Before
    public void setUp() throws Exception {
        scheduler = Executors.newScheduledThreadPool(1);
        filter = new RequestResponseFilter(new MessageInspector(), scheduler);

        // Set up mock objects.
        session = new DummySession();
        chain = session.getFilterChain();
        nextFilterControl = MockControl.createControl(NextFilter.class);
        nextFilter = (NextFilter) nextFilterControl.getMock();

        // Initialize the filter.
        filter.onPreAdd(chain, "reqres", nextFilter);
        filter.onPostAdd(chain, "reqres", nextFilter);
        assertFalse(session.getAttributeKeys().isEmpty());
    }

    @After
    public void tearDown() throws Exception {
        // Destroy the filter.
        filter.onPreRemove(chain, "reqres", nextFilter);
        filter.onPostRemove(chain, "reqres", nextFilter);
        filter.destroy();
        filter = null;
        scheduler.shutdown();
    }

    @Test
    public void testWholeResponse() throws Exception {
        Request req = new Request(1, new Object(), Long.MAX_VALUE);
        Response res = new Response(req, new Message(1, ResponseType.WHOLE),
                ResponseType.WHOLE);
        WriteRequest rwr = new DefaultWriteRequest(req);

        // Record
        nextFilter.filterWrite(session, new DefaultWriteRequest(req
                .getMessage()));
        nextFilterControl.setMatcher(matcher);
        nextFilter.messageSent(session, rwr);
        nextFilter.messageReceived(session, res);

        // Replay
        nextFilterControl.replay();
        filter.filterWrite(nextFilter, session, rwr);
        filter.messageSent(nextFilter, session, matcher.getLastWriteRequest());
        filter.messageReceived(nextFilter, session, res.getMessage());
        filter.messageReceived(nextFilter, session, res.getMessage()); // Ignored

        // Verify
        nextFilterControl.verify();
        assertEquals(res, req.awaitResponse());
        assertNoSuchElementException(req);
    }

    private void assertNoSuchElementException(Request req)
            throws InterruptedException {
        // Make sure if an exception is thrown if a user waits one more time.
        try {
            req.awaitResponse();
            fail();
        } catch (NoSuchElementException e) {
            // Signifies a successful test execution
            assertTrue(true);
        }
    }

    @Test
    public void testPartialResponse() throws Exception {
        Request req = new Request(1, new Object(), Long.MAX_VALUE);
        Response res1 = new Response(req, new Message(1, ResponseType.PARTIAL),
                ResponseType.PARTIAL);
        Response res2 = new Response(req, new Message(1,
                ResponseType.PARTIAL_LAST), ResponseType.PARTIAL_LAST);
        WriteRequest rwr = new DefaultWriteRequest(req);

        // Record
        nextFilter.filterWrite(session, new DefaultWriteRequest(req
                .getMessage()));
        nextFilterControl.setMatcher(matcher);
        nextFilter.messageSent(session, rwr);
        nextFilter.messageReceived(session, res1);
        nextFilter.messageReceived(session, res2);

        // Replay
        nextFilterControl.replay();
        filter.filterWrite(nextFilter, session, rwr);
        filter.messageSent(nextFilter, session, matcher.getLastWriteRequest());
        filter.messageReceived(nextFilter, session, res1.getMessage());
        filter.messageReceived(nextFilter, session, res2.getMessage());
        filter.messageReceived(nextFilter, session, res1.getMessage()); // Ignored
        filter.messageReceived(nextFilter, session, res2.getMessage()); // Ignored

        // Verify
        nextFilterControl.verify();
        assertEquals(res1, req.awaitResponse());
        assertEquals(res2, req.awaitResponse());
        assertNoSuchElementException(req);
    }

    @Test
    public void testWholeResponseTimeout() throws Exception {
        Request req = new Request(1, new Object(), 10); // 10ms timeout
        Response res = new Response(req, new Message(1, ResponseType.WHOLE),
                ResponseType.WHOLE);
        WriteRequest rwr = new DefaultWriteRequest(req);

        // Record
        nextFilter.filterWrite(session, new DefaultWriteRequest(req
                .getMessage()));
        nextFilterControl.setMatcher(matcher);
        nextFilter.messageSent(session, rwr);
        nextFilter.exceptionCaught(session, new RequestTimeoutException(req));
        nextFilterControl.setMatcher(new ExceptionMatcher());

        // Replay
        nextFilterControl.replay();
        filter.filterWrite(nextFilter, session, rwr);
        filter.messageSent(nextFilter, session, matcher.getLastWriteRequest());
        Thread.sleep(300); // Wait until the request times out.
        filter.messageReceived(nextFilter, session, res.getMessage()); // Ignored

        // Verify
        nextFilterControl.verify();
        assertRequestTimeoutException(req);
        assertNoSuchElementException(req);
    }

    private void assertRequestTimeoutException(Request req)
            throws InterruptedException {
        try {
            req.awaitResponse();
            fail();
        } catch (RequestTimeoutException e) {
            // Signifies a successful test execution
            assertTrue(true);
        }
    }

    @Test
    public void testPartialResponseTimeout() throws Exception {
        Request req = new Request(1, new Object(), 10); // 10ms timeout
        Response res1 = new Response(req, new Message(1, ResponseType.PARTIAL),
                ResponseType.PARTIAL);
        Response res2 = new Response(req, new Message(1,
                ResponseType.PARTIAL_LAST), ResponseType.PARTIAL_LAST);
        WriteRequest rwr = new DefaultWriteRequest(req);

        // Record
        nextFilter.filterWrite(session, new DefaultWriteRequest(req
                .getMessage()));
        nextFilterControl.setMatcher(matcher);
        nextFilter.messageSent(session, rwr);
        nextFilter.messageReceived(session, res1);
        nextFilter.exceptionCaught(session, new RequestTimeoutException(req));
        nextFilterControl.setMatcher(new ExceptionMatcher());

        // Replay
        nextFilterControl.replay();
        filter.filterWrite(nextFilter, session, rwr);
        filter.messageSent(nextFilter, session, matcher.getLastWriteRequest());
        filter.messageReceived(nextFilter, session, res1.getMessage());
        Thread.sleep(300); // Wait until the request times out.
        filter.messageReceived(nextFilter, session, res2.getMessage()); // Ignored
        filter.messageReceived(nextFilter, session, res1.getMessage()); // Ignored

        // Verify
        nextFilterControl.verify();
        assertEquals(res1, req.awaitResponse());
        assertRequestTimeoutException(req);
        assertNoSuchElementException(req);
    }

    @Test
    public void testTimeoutByDisconnection() throws Exception {
        // We run a test case that doesn't raise a timeout to make sure
        // the timeout is not raised again by disconnection.
        testWholeResponse();
        nextFilterControl.reset();

        Request req1 = new Request(1, new Object(), Long.MAX_VALUE);
        Request req2 = new Request(2, new Object(), Long.MAX_VALUE);
        WriteRequest rwr1 = new DefaultWriteRequest(req1);
        WriteRequest rwr2 = new DefaultWriteRequest(req2);

        // Record
        nextFilter.filterWrite(session, new DefaultWriteRequest(req1
                .getMessage()));
        nextFilterControl.setMatcher(matcher);
        nextFilter.messageSent(session, rwr1);
        nextFilter.filterWrite(session, new DefaultWriteRequest(req2
                .getMessage()));
        nextFilter.messageSent(session, rwr2);
        nextFilter.exceptionCaught(session, new RequestTimeoutException(req1));
        nextFilterControl.setMatcher(new ExceptionMatcher());
        nextFilter.exceptionCaught(session, new RequestTimeoutException(req2));
        nextFilter.sessionClosed(session);

        // Replay
        nextFilterControl.replay();
        filter.filterWrite(nextFilter, session, rwr1);
        filter.messageSent(nextFilter, session, matcher.getLastWriteRequest());
        filter.filterWrite(nextFilter, session, rwr2);
        filter.messageSent(nextFilter, session, matcher.getLastWriteRequest());
        filter.sessionClosed(nextFilter, session);

        // Verify
        nextFilterControl.verify();
        assertRequestTimeoutException(req1);
        assertRequestTimeoutException(req2);
    }

    static class Message {
        private final int id;

        private final ResponseType type;

        Message(int id, ResponseType type) {
            this.id = id;
            this.type = type;
        }

        public int getId() {
            return id;
        }

        public ResponseType getType() {
            return type;
        }
    }

    private static class MessageInspector implements ResponseInspector {
        /**
         * Default constructor
         */
        public MessageInspector() {
            super();
        }
        
        public Object getRequestId(Object message) {
            if (!(message instanceof Message)) {
                return null;
            }

            return ((Message) message).getId();
        }

        public ResponseType getResponseType(Object message) {
            if (!(message instanceof Message)) {
                return null;
            }

            return ((Message) message).getType();
        }
    }

    private static class WriteRequestMatcher extends AbstractMatcher {
        private WriteRequest lastWriteRequest;

        /**
         * Default constructor
         */
        public WriteRequestMatcher() {
            super();
        }
        
        public WriteRequest getLastWriteRequest() {
            return lastWriteRequest;
        }

        @Override
        protected boolean argumentMatches(Object expected, Object actual) {
            if (actual instanceof WriteRequest
                    && expected instanceof WriteRequest) {
                boolean answer = ((WriteRequest) expected).getMessage().equals(
                        ((WriteRequest) actual).getMessage());
                lastWriteRequest = (WriteRequest) actual;
                return answer;
            }
            return super.argumentMatches(expected, actual);
        }
    }

    static class ExceptionMatcher extends AbstractMatcher {
        @Override
        protected boolean argumentMatches(Object expected, Object actual) {
            if (actual instanceof RequestTimeoutException
                    && expected instanceof RequestTimeoutException) {
                return ((RequestTimeoutException) expected)
                        .getRequest()
                        .equals(((RequestTimeoutException) actual).getRequest());
            }
            return super.argumentMatches(expected, actual);
        }
    }
}
