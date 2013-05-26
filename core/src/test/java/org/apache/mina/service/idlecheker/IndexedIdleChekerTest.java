/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/**
 * 
 */
package org.apache.mina.service.idlecheker;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.net.SocketAddress;

import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSessionConfig;
import org.apache.mina.service.idlechecker.IdleChecker;
import org.apache.mina.service.idlechecker.IndexedIdleChecker;
import org.apache.mina.session.AbstractIoSession;
import org.apache.mina.session.AbstractIoSessionConfig;
import org.apache.mina.session.WriteRequest;
import org.junit.Test;

/**
 * Unit test for {@link IndexedIdleChecker}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class IndexedIdleChekerTest {

    private final IndexedIdleChecker idleChecker = new IndexedIdleChecker();

    private final long now = System.currentTimeMillis();

    @Test
    public void process_on_empty_index() {
        assertEquals(0, idleChecker.processIdleSession(now));
    }

    @Test
    public void dont_send_premature_events() {
        IoService service = mock(IoService.class);
        DummySession session = new DummySession(service, idleChecker);

        session.getConfig().setIdleTimeInMillis(IdleStatus.READ_IDLE, 2000L);

        idleChecker.sessionRead(session, now);

        // should be idle in 2 second
        assertEquals(0, idleChecker.processIdleSession(now));
        assertEquals(0, session.readIdleCount);
        assertEquals(0, session.writeIdleCount);
        assertEquals(1, idleChecker.processIdleSession(now + 3001));

    }

    @Test
    public void read_event() {
        IoService service = mock(IoService.class);
        DummySession session = new DummySession(service, idleChecker);

        session.getConfig().setIdleTimeInMillis(IdleStatus.READ_IDLE, 1000L);

        idleChecker.sessionRead(session, now);

        // should be idle in 1 second
        assertEquals(0, idleChecker.processIdleSession(now));
        assertEquals(0, session.readIdleCount);
        assertEquals(0, session.writeIdleCount);
        assertEquals(1, idleChecker.processIdleSession(now + 2000));
        assertEquals(1, session.readIdleCount);

    }

    @Test
    public void write_event() {
        IoService service = mock(IoService.class);
        DummySession session = new DummySession(service, idleChecker);

        session.getConfig().setIdleTimeInMillis(IdleStatus.WRITE_IDLE, 1000L);

        idleChecker.sessionWritten(session, now);

        // should be idle in 1 second
        assertEquals(1, idleChecker.processIdleSession(now + 12000));
        assertEquals(0, session.readIdleCount);
        assertEquals(1, session.writeIdleCount);
    }

    private class DummySession extends AbstractIoSession {

        int readIdleCount = 0;

        int writeIdleCount = 0;

        private DummySession(IoService service, IdleChecker checker) {
            super(service, checker);
        }

        @Override
        public IoFuture<Void> close(boolean immediately) {
            return null;
        }

        IoSessionConfig config = new AbstractIoSessionConfig() {
        };

        @Override
        public IoSessionConfig getConfig() {
            return config;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void processSessionIdle(IdleStatus status) {
            if (status == IdleStatus.READ_IDLE) {
                readIdleCount++;
            }
            if (status == IdleStatus.WRITE_IDLE) {
                writeIdleCount++;
            }
        }

        @Override
        public SocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public boolean isConnected() {
            return false;
        }

        @Override
        public boolean isReadSuspended() {
            return false;
        }

        @Override
        public boolean isWriteSuspended() {
            return false;
        }

        @Override
        public void resumeRead() {
        }

        @Override
        public void resumeWrite() {
        }

        @Override
        public void suspendRead() {
        }

        @Override
        public void suspendWrite() {
        }

        @Override
        public boolean isSecuring() {
            return false;
        }

        @Override
        public boolean isSecured() {
            return false;
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public WriteRequest enqueueWriteRequest(WriteRequest writeRequest) {
            return null;
        }
    }
}
