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
package org.apache.mina.filter.executor;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * TODO Add documentation
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ExecutorFilterRegressionTest {
    private ExecutorFilter filter;

    public ExecutorFilterRegressionTest() {
        // Do nothing
    }

    @Before
    public void setUp() throws Exception {
        filter = new ExecutorFilter(8);
    }

    @After
    public void tearDown() throws Exception {
        ((ExecutorService) filter.getExecutor()).shutdown();
        filter = null;
    }

    @Test
    public void testEventOrder() throws Throwable {
        final EventOrderChecker nextFilter = new EventOrderChecker();
        final EventOrderCounter[] sessions = new EventOrderCounter[] {
                new EventOrderCounter(), new EventOrderCounter(),
                new EventOrderCounter(), new EventOrderCounter(),
                new EventOrderCounter(), new EventOrderCounter(),
                new EventOrderCounter(), new EventOrderCounter(),
                new EventOrderCounter(), new EventOrderCounter(), };
        final int loop = 1000000;
        final int end = sessions.length - 1;
        final ExecutorFilter filter = this.filter;
        ExecutorService executor = (ExecutorService) filter.getExecutor();
        //executor.setKeepAliveTime(3, TimeUnit.SECONDS);

        for (int i = 0; i < loop; i++) {
            Integer objI = new Integer(i);

            for (int j = end; j >= 0; j--) {
                filter.messageReceived(nextFilter, sessions[j], objI);
            }

            if (nextFilter.throwable != null) {
                throw nextFilter.throwable;
            }
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

        for (int i = end; i >= 0; i--) {
            assertEquals(loop - 1, sessions[i].lastCount.intValue());
        }
    }

    private static class EventOrderCounter extends DummySession {
        Integer lastCount = null;

        /**
         * Default constructor
         */
        public EventOrderCounter() {
            super();
        }
        
        public synchronized void setLastCount(Integer newCount) {
            if (lastCount != null) {
                assertEquals(lastCount.intValue() + 1, newCount
                        .intValue());
            }

            lastCount = newCount;
        }
    }

    private static class EventOrderChecker implements NextFilter {
        Throwable throwable;

        /**
         * Default constructor
         */
        public EventOrderChecker() {
            super();
        }
        
        public void sessionOpened(IoSession session) {
            // Do nothing
        }

        public void sessionClosed(IoSession session) {
            // Do nothing
        }

        public void sessionIdle(IoSession session, IdleStatus status) {
            // Do nothing
        }

        public void exceptionCaught(IoSession session, Throwable cause) {
            // Do nothing
        }

        public void messageReceived(IoSession session, Object message) {
            try {
                ((EventOrderCounter) session).setLastCount((Integer) message);
            } catch (Throwable t) {
                if (this.throwable == null) {
                    this.throwable = t;
                }
            }
        }

        public void messageSent(IoSession session, WriteRequest writeRequest) {
            // Do nothing
        }

        public void filterWrite(IoSession session, WriteRequest writeRequest) {
            // Do nothing
        }

        public void filterClose(IoSession session) {
            // Do nothing
        }

        public void sessionCreated(IoSession session) {
            // Do nothing
        }
    }
}
