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
package org.apache.mina.core;

import org.apache.mina.core.filterchain.DefaultIoFilterChain;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChain.Entry;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.util.NoopFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Tests {@link DefaultIoFilterChain}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class IoFilterChainTest {
    private DummySession dummySession;
    private IoFilterChain chain;
    String testResult;

    private final IoHandler handler = new IoHandlerAdapter() {
        @Override
        public void sessionCreated(IoSession session) {
            testResult += "HS0";
        }

        @Override
        public void sessionOpened(IoSession session) {
            testResult += "HSO";
        }

        @Override
        public void sessionClosed(IoSession session) {
            testResult += "HSC";
        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status) {
            testResult += "HSI";
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) {
            testResult += "HEC";
            if (cause.getClass() != Exception.class) {
                //cause.printStackTrace(System.out);
            }
        }

        @Override
        public void messageReceived(IoSession session, Object message) {
            testResult += "HMR";
        }

        @Override
        public void messageSent(IoSession session, Object message) {
            testResult += "HMS";
        }
    };

    @Before
    public void setUp() {
        dummySession = new DummySession();
        dummySession.setHandler(handler);
        chain = dummySession.getFilterChain();
        testResult = "";
    }

    @After
    public void tearDown() {
        // Do nothing
    }

    @Test
    public void testAdd() throws Exception {
        chain.addFirst("A", new EventOrderTestFilter('A'));
        chain.addLast("B", new EventOrderTestFilter('A'));
        chain.addFirst("C", new EventOrderTestFilter('A'));
        chain.addLast("D", new EventOrderTestFilter('A'));
        chain.addBefore("B", "E", new EventOrderTestFilter('A'));
        chain.addBefore("C", "F", new EventOrderTestFilter('A'));
        chain.addAfter("B", "G", new EventOrderTestFilter('A'));
        chain.addAfter("D", "H", new EventOrderTestFilter('A'));

        String actual = "";
        for (Entry e : chain.getAll()) {
            actual += e.getName();
        }

        assertEquals("FCAEBGDH", actual);
    }

    @Test
    public void testGet() throws Exception {
        IoFilter filterA = new NoopFilter();
        IoFilter filterB = new NoopFilter();
        IoFilter filterC = new NoopFilter();
        IoFilter filterD = new NoopFilter();

        chain.addFirst("A", filterA);
        chain.addLast("B", filterB);
        chain.addBefore("B", "C", filterC);
        chain.addAfter("A", "D", filterD);

        assertSame(filterA, chain.get("A"));
        assertSame(filterB, chain.get("B"));
        assertSame(filterC, chain.get("C"));
        assertSame(filterD, chain.get("D"));
    }

    @Test
    public void testRemove() throws Exception {
        chain.addLast("A", new EventOrderTestFilter('A'));
        chain.addLast("B", new EventOrderTestFilter('A'));
        chain.addLast("C", new EventOrderTestFilter('A'));
        chain.addLast("D", new EventOrderTestFilter('A'));
        chain.addLast("E", new EventOrderTestFilter('A'));

        chain.remove("A");
        chain.remove("E");
        chain.remove("C");
        chain.remove("B");
        chain.remove("D");

        assertEquals(0, chain.getAll().size());
    }

    @Test
    public void testClear() throws Exception {
        chain.addLast("A", new EventOrderTestFilter('A'));
        chain.addLast("B", new EventOrderTestFilter('A'));
        chain.addLast("C", new EventOrderTestFilter('A'));
        chain.addLast("D", new EventOrderTestFilter('A'));
        chain.addLast("E", new EventOrderTestFilter('A'));

        chain.clear();

        assertEquals(0, chain.getAll().size());
    }

    @Test
    public void testToString() throws Exception {
        // When the chain is empty
        assertEquals("{ empty }", chain.toString());

        // When there's one filter
        chain.addLast("A", new IoFilterAdapter() {
            @Override
            public String toString() {
                return "B";
            }
        });
        assertEquals("{ (A:B) }", chain.toString());

        // When there are two
        chain.addLast("C", new IoFilterAdapter() {
            @Override
            public String toString() {
                return "D";
            }
        });
        assertEquals("{ (A:B), (C:D) }", chain.toString());
    }

    @Test
    public void testDefault() {
        run("HS0 HSO HMR HMS HSI HEC HSC");
    }

    @Test
    public void testChained() throws Exception {
        chain.addLast("A", new EventOrderTestFilter('A'));
        chain.addLast("B", new EventOrderTestFilter('B'));
        run("AS0 BS0 HS0" + "ASO BSO HSO" + "AMR BMR HMR"
                + "BFW AFW AMS BMS HMS" + "ASI BSI HSI" + "AEC BEC HEC"
                + "ASC BSC HSC");
    }

    @Test
    public void testAddRemove() throws Exception {
        IoFilter filter = new AddRemoveTestFilter();

        chain.addFirst("A", filter);
        assertEquals("ADDED", testResult);

        chain.remove("A");
        assertEquals("ADDEDREMOVED", testResult);
    }

    private void run(String expectedResult) {
        chain.fireSessionCreated();
        chain.fireSessionOpened();
        chain.fireMessageReceived(new Object());
        chain.fireFilterWrite(new DefaultWriteRequest(new Object()));
        chain.fireSessionIdle(IdleStatus.READER_IDLE);
        chain.fireExceptionCaught(new Exception());
        chain.fireSessionClosed();

        testResult = formatResult(testResult);
        String formatedExpectedResult = formatResult(expectedResult); 

        assertEquals(formatedExpectedResult, testResult);
    }

    private String formatResult(String result) {
        String newResult = result.replaceAll("\\s", "");
        StringBuilder buf = new StringBuilder(newResult.length() * 4 / 3);
        
        for (int i = 0; i < newResult.length(); i++) {
            buf.append(newResult.charAt(i));
        
            if (i % 3 == 2) {
                buf.append(' ');
            }
        }

        return buf.toString();
    }

    private class EventOrderTestFilter extends IoFilterAdapter {
        private final char id;

        EventOrderTestFilter(char id) {
            this.id = id;
        }

        @Override
        public void sessionCreated(NextFilter nextFilter, IoSession session) {
            testResult += id + "S0";
            nextFilter.sessionCreated(session);
        }

        @Override
        public void sessionOpened(NextFilter nextFilter, IoSession session) {
            testResult += id + "SO";
            nextFilter.sessionOpened(session);
        }

        @Override
        public void sessionClosed(NextFilter nextFilter, IoSession session) {
            testResult += id + "SC";
            nextFilter.sessionClosed(session);
        }

        @Override
        public void sessionIdle(NextFilter nextFilter, IoSession session,
                IdleStatus status) {
            testResult += id + "SI";
            nextFilter.sessionIdle(session, status);
        }

        @Override
        public void exceptionCaught(NextFilter nextFilter, IoSession session,
                Throwable cause) {
            testResult += id + "EC";
            nextFilter.exceptionCaught(session, cause);
        }

        @Override
        public void filterWrite(NextFilter nextFilter, IoSession session,
                WriteRequest writeRequest) {
            testResult += id + "FW";
            nextFilter.filterWrite(session, writeRequest);
        }

        @Override
        public void messageReceived(NextFilter nextFilter, IoSession session,
                Object message) {
            testResult += id + "MR";
            nextFilter.messageReceived(session, message);
        }

        @Override
        public void messageSent(NextFilter nextFilter, IoSession session,
                WriteRequest writeRequest) {
            testResult += id + "MS";
            nextFilter.messageSent(session, writeRequest);
        }

        @Override
        public void filterClose(NextFilter nextFilter, IoSession session)
                throws Exception {
            nextFilter.filterClose(session);
        }
    }

    private class AddRemoveTestFilter extends IoFilterAdapter {
        /**
         * Default constructor
         */
        public AddRemoveTestFilter() {
            super();
        }
        
        @Override
        public void onPostAdd(IoFilterChain parent, String name,
                NextFilter nextFilter) {
            testResult += "ADDED";
        }

        @Override
        public void onPostRemove(IoFilterChain parent, String name,
                NextFilter nextFilter) {
            testResult += "REMOVED";
        }
    }
}
