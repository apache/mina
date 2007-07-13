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
package org.apache.mina.common;

import java.net.SocketAddress;
import java.util.Iterator;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.IoFilterChain.Entry;
import org.apache.mina.common.support.AbstractIoFilterChain;
import org.apache.mina.common.support.BaseIoSession;

/**
 * Tests {@link AbstractIoFilterChain}.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$ 
 */
public class IoFilterChainTest extends TestCase {
    private IoFilterChainImpl chain;

    private IoSession session;

    private String result;

    public void setUp() {
        chain = new IoFilterChainImpl();
        session = new TestSession();
        result = "";
    }

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
        for (Iterator i = chain.getAll().iterator(); i.hasNext();) {
            Entry e = (Entry) i.next();
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
            public String toString() {
                return "B";
            }
        });
        Assert.assertEquals("{ (A:B) }", chain.toString());

        // When there are two
        chain.addLast("C", new IoFilterAdapter() {
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
        chain.fireSessionCreated(session);
        chain.fireSessionOpened(session);
        chain.fireMessageReceived(session, new Object());
        chain.fireFilterWrite(session, new WriteRequest(new Object()));
        chain.fireSessionIdle(session, IdleStatus.READER_IDLE);
        chain.fireExceptionCaught(session, new Exception());
        chain.fireSessionClosed(session);

        result = formatResult(result);
        expectedResult = formatResult(expectedResult);

        System.out.println("Expected: " + expectedResult);
        System.out.println("Actual:   " + result);
        Assert.assertEquals(expectedResult, result);
    }

    private String formatResult(String result) {
        result = result.replaceAll("\\s", "");
        StringBuffer buf = new StringBuffer(result.length() * 4 / 3);
        for (int i = 0; i < result.length(); i++) {
            buf.append(result.charAt(i));
            if (i % 3 == 2) {
                buf.append(' ');
            }
        }

        return buf.toString();
    }

    private class TestSession extends BaseIoSession implements IoSession {
        private IoHandler handler = new IoHandlerAdapter() {
            public void sessionCreated(IoSession session) {
                result += "HS0";
            }

            public void sessionOpened(IoSession session) {
                result += "HSO";
            }

            public void sessionClosed(IoSession session) {
                result += "HSC";
            }

            public void sessionIdle(IoSession session, IdleStatus status) {
                result += "HSI";
            }

            public void exceptionCaught(IoSession session, Throwable cause) {
                result += "HEC";
                if (cause.getClass() != Exception.class) {
                    cause.printStackTrace(System.out);
                }
            }

            public void messageReceived(IoSession session, Object message) {
                result += "HMR";
            }

            public void messageSent(IoSession session, Object message) {
                result += "HMS";
            }
        };

        public IoHandler getHandler() {
            return handler;
        }

        public CloseFuture close() {
            return null;
        }

        public TransportType getTransportType() {
            return TransportType.VM_PIPE;
        }

        public SocketAddress getRemoteAddress() {
            return null;
        }

        public SocketAddress getLocalAddress() {
            return null;
        }

        public IoFilterChain getFilterChain() {
            return new AbstractIoFilterChain(this) {
                protected void doWrite(IoSession session,
                        WriteRequest writeRequest) {
                }

                protected void doClose(IoSession session) {
                }
            };
        }

        public int getScheduledWriteRequests() {
            return 0;
        }

        protected void updateTrafficMask() {
        }

        public boolean isClosing() {
            return false;
        }

        public IoService getService() {
            return null;
        }

        public IoSessionConfig getConfig() {
            return null;
        }

        public SocketAddress getServiceAddress() {
            return null;
        }

        public int getScheduledWriteBytes() {
            return 0;
        }

        public IoServiceConfig getServiceConfig() {
            return null;
        }
    }

    private class EventOrderTestFilter extends IoFilterAdapter {
        private final char id;

        private EventOrderTestFilter(char id) {
            this.id = id;
        }

        public void sessionCreated(NextFilter nextFilter, IoSession session) {
            result += id + "S0";
            nextFilter.sessionCreated(session);
        }

        public void sessionOpened(NextFilter nextFilter, IoSession session) {
            result += id + "SO";
            nextFilter.sessionOpened(session);
        }

        public void sessionClosed(NextFilter nextFilter, IoSession session) {
            result += id + "SC";
            nextFilter.sessionClosed(session);
        }

        public void sessionIdle(NextFilter nextFilter, IoSession session,
                IdleStatus status) {
            result += id + "SI";
            nextFilter.sessionIdle(session, status);
        }

        public void exceptionCaught(NextFilter nextFilter, IoSession session,
                Throwable cause) {
            result += id + "EC";
            nextFilter.exceptionCaught(session, cause);
        }

        public void filterWrite(NextFilter nextFilter, IoSession session,
                WriteRequest writeRequest) {
            result += id + "FW";
            nextFilter.filterWrite(session, writeRequest);
        }

        public void messageReceived(NextFilter nextFilter, IoSession session,
                Object message) {
            result += id + "MR";
            nextFilter.messageReceived(session, message);
        }

        public void messageSent(NextFilter nextFilter, IoSession session,
                Object message) {
            result += id + "MS";
            nextFilter.messageSent(session, message);
        }

        public void filterClose(NextFilter nextFilter, IoSession session)
                throws Exception {
            nextFilter.filterClose(session);
        }
    }

    private class AddRemoveTestFilter extends IoFilterAdapter {
        public void onPostAdd(IoFilterChain parent, String name,
                NextFilter nextFilter) {
            result += "ADDED";
        }

        public void onPostRemove(IoFilterChain parent, String name,
                NextFilter nextFilter) {
            result += "REMOVED";
        }
    }

    private static class IoFilterChainImpl extends AbstractIoFilterChain {
        protected IoFilterChainImpl() {
            super(new BaseIoSession() {
                protected void updateTrafficMask() {
                }

                public IoService getService() {
                    return null;
                }

                public IoHandler getHandler() {
                    return null;
                }

                public IoFilterChain getFilterChain() {
                    return null;
                }

                public TransportType getTransportType() {
                    return null;
                }

                public SocketAddress getRemoteAddress() {
                    return null;
                }

                public SocketAddress getLocalAddress() {
                    return null;
                }

                public int getScheduledWriteRequests() {
                    return 0;
                }

                public IoSessionConfig getConfig() {
                    return null;
                }

                public SocketAddress getServiceAddress() {
                    return null;
                }

                public int getScheduledWriteBytes() {
                    return 0;
                }

                public IoServiceConfig getServiceConfig() {
                    return null;
                }
            });
        }

        protected void doWrite(IoSession session, WriteRequest writeRequest) {
            fireMessageSent(session, writeRequest);
        }

        protected void doClose(IoSession session) {
        }
    }

}
