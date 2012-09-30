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
package org.apache.mina.session;

import static junit.framework.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

import junit.framework.Assert;

import org.apache.mina.api.AbstractIoFilter;
import org.apache.mina.api.IoFilter;
import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSessionConfig;
import org.apache.mina.filterchain.ReadFilterChainController;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.junit.Before;
import org.junit.Test;

/**
 * A test class for IoSession
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class AbstractIoSessionTest {

    private class DummySession extends AbstractIoSession {
        private DummySession(IoService service) {
            super(service, null);
        }

        @Override
        public IoFuture<Void> close(boolean immediately) {
            return null;
        }

        @Override
        public IoSessionConfig getConfig() {
            return null;
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

        /**
         * {@inheritDoc}
         */
        @Override
        protected void channelClose() {

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void flushWriteQueue() {
        }
    }

    private IoService service = null;

    private final IoFilter filter1 = spy(new PassthruFilter());

    private final IoFilter filter2 = spy(new PassthruFilter());

    private final IoFilter filter3 = spy(new PassthruFilter());

    @Before
    public void setup() {
        service = mock(IoService.class);

        when(service.getFilters()).thenReturn(new IoFilter[] { filter1, filter2, filter3 });
    }

    @Test
    public void testGetId() {

        Assert.assertNotSame((new DummySession(service)).getId(), (new DummySession(service)).getId());

    }

    @Test
    public void testCreationTime() {
        long before = System.currentTimeMillis();
        long creation = (new DummySession(service)).getCreationTime();
        long after = System.currentTimeMillis();
        Assert.assertTrue(creation <= after);
        Assert.assertTrue(creation >= before);
    }

    @Test
    public void testAttachment() {
        AbstractIoSession aio = new DummySession(service);
        String value = "value";
        AttributeKey<String> key = new AttributeKey<String>(String.class, "test");
        assertNull(aio.getAttribute(key, null));
        assertEquals(null, aio.setAttribute(key, value));

        assertEquals(aio.getAttributeKeys().size(), 1);
        assertEquals(value, aio.setAttribute(key, value));
        assertEquals(aio.getAttributeKeys().size(), 1);
        assertEquals(value, aio.getAttribute(key, null));
        assertEquals(value, aio.removeAttribute(key));
        assertEquals(aio.getAttributeKeys().size(), 0);

        assertEquals(null, aio.getAttribute(key, null));
        assertNotNull(aio.getService());
    }

    @Test
    public void chain_reads() {
        DummySession session = new DummySession(service);
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        long before = System.currentTimeMillis();
        session.processMessageReceived(buffer);
        verify(filter1).messageReceived(eq(session), eq(buffer), any(ReadFilterChainController.class));
        verify(filter2).messageReceived(eq(session), eq(buffer), any(ReadFilterChainController.class));
        verify(filter3).messageReceived(eq(session), eq(buffer), any(ReadFilterChainController.class));
        assertEquals(1024L, session.getReadBytes());
        long lastRead = session.getLastReadTime();
        assertTrue(lastRead - before < 100);
    }

    @Test
    public void chain_writes() {
        DummySession session = new DummySession(service);
        ByteBuffer buffer = mock(ByteBuffer.class);
        session.processMessageWriting(buffer, null);
        verify(filter1).messageWriting(eq(session), eq(buffer), any(WriteFilterChainController.class));
        verify(filter2).messageWriting(eq(session), eq(buffer), any(WriteFilterChainController.class));
        verify(filter3).messageWriting(eq(session), eq(buffer), any(WriteFilterChainController.class));
    }

    @Test
    public void chain_created() {
        DummySession session = new DummySession(service);
        session.processSessionCreated();
        verify(filter1).sessionCreated(eq(session));
        verify(filter2).sessionCreated(eq(session));
        verify(filter3).sessionCreated(eq(session));
    }

    @Test
    public void chain_open() {
        DummySession session = new DummySession(service);
        session.processSessionOpened();
        verify(filter1).sessionOpened(eq(session));
        verify(filter2).sessionOpened(eq(session));
        verify(filter3).sessionOpened(eq(session));
    }

    @Test
    public void chain_close() {
        DummySession session = new DummySession(service);
        session.processSessionClosed();
        verify(filter1).sessionClosed(eq(session));
        verify(filter2).sessionClosed(eq(session));
        verify(filter3).sessionClosed(eq(session));
    }

    @Test
    public void increment_written_bytes() {
        DummySession session = new DummySession(service);
        assertEquals(0, session.getWrittenBytes());
        session.incrementWrittenBytes(1024);
        assertEquals(1024, session.getWrittenBytes());
    }

    private class PassthruFilter extends AbstractIoFilter {

    }
}
