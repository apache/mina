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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.apache.mina.api.AbstractIoFilter;
import org.apache.mina.api.IoFilter;
import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSession;
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
        private DummySession(final IoService service) {
            super(service, null);
        }

        @Override
        public IoFuture<Void> close(final boolean immediately) {
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

        @Override
        public WriteRequest enqueueWriteRequest(WriteRequest writeRequest) {
            return null;
        }
    }

    private IoService service = null;

    private final IoFilter filter1 = spy(new PassthruFilter());

    private final IoFilter filter2 = spy(new PassthruFilter());

    private final IoFilter filter3 = spy(new PassthruFilter());

    private final IoFilter filterWriteBack = spy(new PassthruFilter() {
        @Override
        public void messageReceived(IoSession session, Object message, ReadFilterChainController controller) {
            controller.callWriteMessageForRead(message);
        }
    });

    @Before
    public void setup() {
        service = mock(IoService.class);

        when(service.getFilters()).thenReturn(new IoFilter[] { filter1, filter2, filter3 });
    }

    @Test
    public void testGetId() {

        assertNotSame((new DummySession(service)).getId(), (new DummySession(service)).getId());

    }

    @Test
    public void testCreationTime() {
        final long before = System.currentTimeMillis();
        final long creation = (new DummySession(service)).getCreationTime();
        final long after = System.currentTimeMillis();
        assertTrue(creation <= after);
        assertTrue(creation >= before);
    }

    @Test
    public void testAttachment() {
        final AbstractIoSession aio = new DummySession(service);
        final String value = "value";
        final AttributeKey<String> key = new AttributeKey<String>(String.class, "test");
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
        final DummySession session = new DummySession(service);
        final ByteBuffer buffer = ByteBuffer.allocate(1024);

        final long before = System.currentTimeMillis();
        session.processMessageReceived(buffer);
        verify(filter1).messageReceived(eq(session), eq(buffer), any(ReadFilterChainController.class));
        verify(filter2).messageReceived(eq(session), eq(buffer), any(ReadFilterChainController.class));
        verify(filter3).messageReceived(eq(session), eq(buffer), any(ReadFilterChainController.class));
        assertEquals(1024L, session.getReadBytes());
        final long lastRead = session.getLastReadTime();
        assertTrue(lastRead - before < 100);
    }

    @Test
    public void chain_reads_with_writeback() {
        service = mock(IoService.class);
        when(service.getFilters()).thenReturn(new IoFilter[] { filter1, filterWriteBack, filter3 });
        final DummySession session = new DummySession(service);
        final ByteBuffer buffer = ByteBuffer.allocate(1024);

        final long before = System.currentTimeMillis();
        session.processMessageReceived(buffer);
        verify(filter1).messageReceived(eq(session), eq(buffer), any(ReadFilterChainController.class));
        verify(filterWriteBack).messageReceived(eq(session), eq(buffer), any(ReadFilterChainController.class));
        verify(filter1).messageWriting(eq(session), any(WriteRequest.class), any(WriteFilterChainController.class));

        assertEquals(1024L, session.getReadBytes());
        final long lastRead = session.getLastReadTime();
        assertTrue(lastRead - before < 100);
        verifyNoMoreInteractions(filter1, filter2, filter3, filterWriteBack);
    }

    @Test
    public void chain_reads_with_writeback_final() {
        service = mock(IoService.class);
        when(service.getFilters()).thenReturn(new IoFilter[] { filterWriteBack, filter2, filter3 });
        final DummySession session = new DummySession(service);
        final ByteBuffer buffer = ByteBuffer.allocate(1024);

        final long before = System.currentTimeMillis();
        session.processMessageReceived(buffer);
        verify(filterWriteBack).messageReceived(eq(session), eq(buffer), any(ReadFilterChainController.class));

        assertEquals(1024L, session.getReadBytes());
        final long lastRead = session.getLastReadTime();
        assertTrue(lastRead - before < 100);
        verifyNoMoreInteractions(filter1, filter2, filter3, filterWriteBack);
    }

    @Test
    public void chain_writes() {
        final DummySession session = new DummySession(service);
        final WriteRequest buffer = mock(DefaultWriteRequest.class);
        session.processMessageWriting(buffer, null);
        verify(filter1).messageWriting(eq(session), eq(buffer), any(WriteFilterChainController.class));
        verify(filter2).messageWriting(eq(session), eq(buffer), any(WriteFilterChainController.class));
        verify(filter3).messageWriting(eq(session), eq(buffer), any(WriteFilterChainController.class));
    }

    @Test
    public void chain_open() {
        final DummySession session = new DummySession(service);
        session.processSessionOpen();
        verify(filter1).sessionOpened(eq(session));
        verify(filter2).sessionOpened(eq(session));
        verify(filter3).sessionOpened(eq(session));
    }

    @Test
    public void chain_close() {
        final DummySession session = new DummySession(service);
        session.processSessionClosed();
        verify(filter1).sessionClosed(eq(session));
        verify(filter2).sessionClosed(eq(session));
        verify(filter3).sessionClosed(eq(session));
    }

    @Test
    public void increment_written_bytes() {
        final DummySession session = new DummySession(service);
        assertEquals(0, session.getWrittenBytes());
        session.incrementWrittenBytes(1024);
        assertEquals(1024, session.getWrittenBytes());
    }

    private class PassthruFilter extends AbstractIoFilter {

    }
}
