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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.SocketAddress;

import junit.framework.Assert;

import org.apache.mina.api.IoFilter;
import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSessionConfig;
import org.junit.Before;
import org.junit.Test;

/**
 * A test class for IoSession
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class AbstractIoSessionTest {

    private static final class DummySession extends AbstractIoSession {
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
            // TODO Auto-generated method stub
            return false;
        }
    }

    private IoService service = null;

    @Before
    public void setup() {
        service = mock(IoService.class);
        when(service.getFilters()).thenReturn(new IoFilter[] {});
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
}
