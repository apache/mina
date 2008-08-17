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

import junit.framework.Assert;
import junit.framework.TestCase;

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

/**
 * Tests {@link DefaultIoFilterChain}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class IoFilterChainTest extends TestCase {
    private DummySession session;
    private IoFilterChain chain;
    private String result;

    private final IoHandler handler = new IoHandlerAdapter() {
        @Override
        public void sessionCreated(IoSession session) {
            result += "HS0";
        }

        @Override
        public void sessionOpened(IoSession session) {
            result += "HSO";
        }

        @Override
        public void sessionClosed(IoSession session) {
            result += "HSC";
        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status) {
            result += "HSI";
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) {
            result += "HEC";
            if (cause.getClass() != Exception.class) {
                cause.printStackTrace(System.out);
            }
        }

        @Override
        public void messageReceived(IoSession session, Object message) {
            result += "HMR";
        }

        @Override
        public void messageSent(IoSession session, Object message) {
            result += "HMS";
        }
    };

    @Override
    public void setUp() {
        session = new DummySession();
        session.setHandler(handler);
        chain = session.getFilterChain();
        result = "";
    }

    @Override
    public void tearDown() {
    }

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

        Assert.assertEquals("FCAEBGDH", actual);
    }

    public void testGet() throws Exception {
        IoFilter filterA = new IoFilterAdapter();
        IoFilter filterB = new IoFilterAdapter();
        IoFilter filterC = new IoFilterAdapter();
        IoFilter filterD = new IoFilterAdapter();

        chain.addFirst("A", filterA);
        chain.addLast("B", filterB);
        chain.addBefore("B", "C", filterC);
        chain.addAfter("A", "D", filterD);

        Assert.assertSame(filterA, chain.get("A"));
        Assert.assertSame(filterB, chain.get("B"));
        Assert.assertSame(filterC, chain.get("C"));
        Assert.assertSame(filterD, chain.get("D"));
    }

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

        Assert.assertEquals(0, chain.getAll().size());
    }

    public void testClear() throws Exception {
        chain.addLast("A", new EventOrderTestFilter('A'));
        chain.addLast("B", new EventOrderTestFilter('A'));
        chain.addLast("C", new EventOrderTestFilter('A'));
        chain.addLast("D", new EventOrderTestFilter('A'));
        chain.addLast("E", new EventOrderTestFilter('A'));

        chain.clear();

        Assert.assertEquals(0, chain.getAll().size());
    }

    public void testToString() throws Exception {
        // When the chain is empty
        Assert.assertEquals("{ empty }", chain.toString());

        // When there's one filter
        chain.addLast("A", new IoFilterAdapter() {
            @Override
            public String toString() {
                return "B";
            }
        });
        Assert.assertEquals("{ (A:B) }", chain.toString());

        // When there are two
        chain.addLast("C", new IoFilterAdapter() {
            @Override
            public String toString() {
                return "D";
            }
        });
        Assert.assertEquals("{ (A:B), (C:D) }", chain.toString());
    }

    public void testDefault() {
        run("HS0 HSO HMR HMS HSI HEC HSC");
    }

    public void testChained() throws Exception {
        chain.addLast("A", new EventOrderTestFilter('A'));
        chain.addLast("B", new EventOrderTestFilter('B'));
        run("AS0 BS0 HS0" + "ASO BSO HSO" + "AMR BMR HMR"
                + "BFW AFW AMS BMS HMS" + "ASI BSI HSI" + "AEC BEC HEC"
                + "ASC BSC HSC");
    }

    public void testAddRemove() throws Exception {
        IoFilter filter = new AddRemoveTestFilter();

        chain.addFirst("A", filter);
        assertEquals("ADDED", result);

        chain.remove("A");
        assertEquals("ADDEDREMOVED", result);
    }

    private void run(String expectedResult) {
        chain.fireSessionCreated();
        chain.fireSessionOpened();
        chain.fireMessageReceived(new Object());
        chain.fireFilterWrite(new DefaultWriteRequest(new Object()));
        chain.fireSessionIdle(IdleStatus.READER_IDLE);
        chain.fireExceptionCaught(new Exception());
        chain.fireSessionClosed();

        result = formatResult(result);
        expectedResult = formatResult(expectedResult);

        System.out.println("Expected: " + expectedResult);
        System.out.println("Actual:   " + result);
        Assert.assertEquals(expectedResult, result);
    }

    private String formatResult(String result) {
        result = result.replaceAll("\\s", "");
        StringBuilder buf = new StringBuilder(result.length() * 4 / 3);
        for (int i = 0; i < result.length(); i++) {
            buf.append(result.charAt(i));
            if (i % 3 == 2) {
                buf.append(' ');
            }
        }

        return buf.toString();
    }

    private class EventOrderTestFilter extends IoFilterAdapter {
        private final char id;

        private EventOrderTestFilter(char id) {
            this.id = id;
        }

        @Override
        public void sessionCreated(NextFilter nextFilter, IoSession session) {
            result += id + "S0";
            nextFilter.sessionCreated(session);
        }

        @Override
        public void sessionOpened(NextFilter nextFilter, IoSession session) {
            result += id + "SO";
            nextFilter.sessionOpened(session);
        }

        @Override
        public void sessionClosed(NextFilter nextFilter, IoSession session) {
            result += id + "SC";
            nextFilter.sessionClosed(session);
        }

        @Override
        public void sessionIdle(NextFilter nextFilter, IoSession session,
                IdleStatus status) {
            result += id + "SI";
            nextFilter.sessionIdle(session, status);
        }

        @Override
        public void exceptionCaught(NextFilter nextFilter, IoSession session,
                Throwable cause) {
            result += id + "EC";
            nextFilter.exceptionCaught(session, cause);
        }

        @Override
        public void filterWrite(NextFilter nextFilter, IoSession session,
                WriteRequest writeRequest) {
            result += id + "FW";
            nextFilter.filterWrite(session, writeRequest);
        }

        @Override
        public void messageReceived(NextFilter nextFilter, IoSession session,
                Object message) {
            result += id + "MR";
            nextFilter.messageReceived(session, message);
        }

        @Override
        public void messageSent(NextFilter nextFilter, IoSession session,
                WriteRequest writeRequest) {
            result += id + "MS";
            nextFilter.messageSent(session, writeRequest);
        }

        @Override
        public void filterClose(NextFilter nextFilter, IoSession session)
                throws Exception {
            nextFilter.filterClose(session);
        }
    }

    private class AddRemoveTestFilter extends IoFilterAdapter {
        @Override
        public void onPostAdd(IoFilterChain parent, String name,
                NextFilter nextFilter) {
            result += "ADDED";
        }

        @Override
        public void onPostRemove(IoFilterChain parent, String name,
                NextFilter nextFilter) {
            result += "REMOVED";
        }
    }
}
