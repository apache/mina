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
package org.apache.mina.transport.socket.nio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.Test;

/**
 * Test for DIRMINA-732
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DatagramSessionIdleTest {

    private boolean readerIdleReceived;

    private boolean writerIdleReceived;

    private boolean bothIdleReceived;

    private Object mutex = new Object();

    private class TestHandler extends IoHandlerAdapter {

        @Override
        public void sessionIdle(IoSession session, IdleStatus status)
                throws Exception {
            if (status == IdleStatus.BOTH_IDLE) {
                bothIdleReceived = true;
            } else if (status == IdleStatus.READER_IDLE) {
                readerIdleReceived = true;
            } else if (status == IdleStatus.WRITER_IDLE) {
                writerIdleReceived = true;
            }
            
            synchronized (mutex) {
                mutex.notifyAll();
            }
            
            super.sessionIdle(session, status);
        }
    }

    @Test
    public void testSessionIdle() throws Exception {
        final int READER_IDLE_TIME = 3;//seconds
        final int WRITER_IDLE_TIME = READER_IDLE_TIME + 2;//seconds
        final int BOTH_IDLE_TIME = WRITER_IDLE_TIME + 2;//seconds
        
        NioDatagramAcceptor acceptor = new NioDatagramAcceptor();
        acceptor.getSessionConfig().setBothIdleTime(BOTH_IDLE_TIME);
        acceptor.getSessionConfig().setReaderIdleTime(READER_IDLE_TIME);
        acceptor.getSessionConfig().setWriterIdleTime(WRITER_IDLE_TIME);
        InetSocketAddress bindAddress = new InetSocketAddress(  AvailablePortFinder.getNextAvailable());
        acceptor.setHandler(new TestHandler());
        acceptor.bind(bindAddress);
        IoSession session = acceptor.newSession(new InetSocketAddress(
                "127.0.0.1", AvailablePortFinder.getNextAvailable()), bindAddress);
        
        //check properties to be copied from acceptor to session
        assertEquals(BOTH_IDLE_TIME, session.getConfig().getBothIdleTime());
        assertEquals(READER_IDLE_TIME, session.getConfig().getReaderIdleTime());
        assertEquals(WRITER_IDLE_TIME, session.getConfig().getWriterIdleTime());
        
        //verify that IDLE events really received by handler
        long startTime = System.currentTimeMillis();
        
        synchronized (mutex) {
            while (!readerIdleReceived
                    && (System.currentTimeMillis() - startTime) < (READER_IDLE_TIME + 1) * 1000)
                try {
                    mutex.wait(READER_IDLE_TIME * 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
        
        assertTrue(readerIdleReceived);
        
        synchronized (mutex) {
            while (!writerIdleReceived
                    && (System.currentTimeMillis() - startTime) < (WRITER_IDLE_TIME + 1) * 1000)
                try {
                    mutex.wait((WRITER_IDLE_TIME - READER_IDLE_TIME) * 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
        
        assertTrue(writerIdleReceived);
        
        synchronized (mutex) {
            while (!bothIdleReceived
                    && (System.currentTimeMillis() - startTime) < (BOTH_IDLE_TIME + 1) * 1000)
                try {
                    mutex.wait((BOTH_IDLE_TIME - WRITER_IDLE_TIME) * 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
        
        assertTrue(bothIdleReceived);
    }
}