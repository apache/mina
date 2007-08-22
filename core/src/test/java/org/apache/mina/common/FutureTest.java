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

import java.io.IOException;

import junit.framework.TestCase;

/**
 * Tests {@link IoFuture} implementations.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$ 
 */
public class FutureTest extends TestCase {

    public void testCloseFuture() throws Exception {
        DefaultCloseFuture future = new DefaultCloseFuture(null);
        assertFalse(future.isReady());
        assertFalse(future.isClosed());

        TestThread thread = new TestThread(future);
        thread.start();

        future.setClosed();
        thread.join();

        assertTrue(thread.success);
        assertTrue(future.isReady());
        assertTrue(future.isClosed());
    }

    public void testConnectFuture() throws Exception {
        DefaultConnectFuture future = new DefaultConnectFuture();
        assertFalse(future.isReady());
        assertFalse(future.isConnected());
        assertNull(future.getSession());
        assertNull(future.getException());

        TestThread thread = new TestThread(future);
        thread.start();

        IoSession session = new DummySession();

        future.setSession(session);
        thread.join();

        assertTrue(thread.success);
        assertTrue(future.isReady());
        assertTrue(future.isConnected());
        assertEquals(session, future.getSession());
        assertNull(future.getException());

        future = new DefaultConnectFuture();
        thread = new TestThread(future);
        thread.start();
        future.setException(new IOException());
        thread.join();

        assertTrue(thread.success);
        assertTrue(future.isReady());
        assertFalse(future.isConnected());
        assertTrue(future.getException() instanceof IOException);

        try {
            future.getSession();
            fail("IOException should be thrown.");
        } catch (Exception e) {
        }
    }

    public void testWriteFuture() throws Exception {
        DefaultWriteFuture future = new DefaultWriteFuture(null);
        assertFalse(future.isReady());
        assertFalse(future.isWritten());

        TestThread thread = new TestThread(future);
        thread.start();

        future.setWritten(true);
        thread.join();

        assertTrue(thread.success);
        assertTrue(future.isReady());
        assertTrue(future.isWritten());

        future = new DefaultWriteFuture(null);
        thread = new TestThread(future);
        thread.start();

        future.setWritten(false);
        thread.join();

        assertTrue(thread.success);
        assertTrue(future.isReady());
        assertFalse(future.isWritten());
    }

    public void testAddListener() throws Exception {
        DefaultCloseFuture future = new DefaultCloseFuture(null);
        assertFalse(future.isReady());
        assertFalse(future.isClosed());

        TestListener listener1 = new TestListener();
        TestListener listener2 = new TestListener();
        future.addListener(listener1);
        future.addListener(listener2);

        TestThread thread = new TestThread(future);
        thread.start();

        future.setClosed();
        thread.join();

        assertTrue(thread.success);
        assertTrue(future.isReady());
        assertTrue(future.isClosed());

        assertSame(future, listener1.notifiedFuture);
        assertSame(future, listener2.notifiedFuture);
    }

    public void testLateAddListener() throws Exception {
        DefaultCloseFuture future = new DefaultCloseFuture(null);
        assertFalse(future.isReady());
        assertFalse(future.isClosed());

        TestThread thread = new TestThread(future);
        thread.start();

        future.setClosed();
        thread.join();

        assertTrue(thread.success);
        assertTrue(future.isReady());
        assertTrue(future.isClosed());

        TestListener listener = new TestListener();
        future.addListener(listener);
        assertSame(future, listener.notifiedFuture);
    }

    public void testRemoveListener1() throws Exception {
        DefaultCloseFuture future = new DefaultCloseFuture(null);
        assertFalse(future.isReady());
        assertFalse(future.isClosed());

        TestListener listener1 = new TestListener();
        TestListener listener2 = new TestListener();
        future.addListener(listener1);
        future.addListener(listener2);
        future.removeListener(listener1);

        TestThread thread = new TestThread(future);
        thread.start();

        future.setClosed();
        thread.join();

        assertTrue(thread.success);
        assertTrue(future.isReady());
        assertTrue(future.isClosed());

        assertSame(null, listener1.notifiedFuture);
        assertSame(future, listener2.notifiedFuture);
    }

    public void testRemoveListener2() throws Exception {
        DefaultCloseFuture future = new DefaultCloseFuture(null);
        assertFalse(future.isReady());
        assertFalse(future.isClosed());

        TestListener listener1 = new TestListener();
        TestListener listener2 = new TestListener();
        future.addListener(listener1);
        future.addListener(listener2);
        future.removeListener(listener2);

        TestThread thread = new TestThread(future);
        thread.start();

        future.setClosed();
        thread.join();

        assertTrue(thread.success);
        assertTrue(future.isReady());
        assertTrue(future.isClosed());

        assertSame(future, listener1.notifiedFuture);
        assertSame(null, listener2.notifiedFuture);
    }

    private static class TestThread extends Thread {
        private final IoFuture future;

        private boolean success;

        public TestThread(IoFuture future) {
            this.future = future;
        }

        @Override
        public void run() {
            success = future.awaitUninterruptibly(10000);
        }
    }

    private static class TestListener implements IoFutureListener {
        private IoFuture notifiedFuture;

        public void operationComplete(IoFuture future) {
            this.notifiedFuture = future;
        }
    }
}
