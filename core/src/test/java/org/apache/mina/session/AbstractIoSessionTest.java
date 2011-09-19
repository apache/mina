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

import static org.mockito.Mockito.mock;

import java.net.SocketAddress;

import junit.framework.Assert;

import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSessionConfig;
import org.junit.Test;

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
        public boolean isClosing() {
            return false;
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
    }

    @Test
    public void testGetId() {
        Assert.assertNotSame((new DummySession(mock(IoService.class))).getId(),
                (new DummySession(mock(IoService.class))).getId());

    }

    @Test
    public void testCreationTime() {
        long before = System.currentTimeMillis();
        long creation = (new DummySession(mock(IoService.class))).getCreationTime();
        long after = System.currentTimeMillis();
        Assert.assertTrue(creation <= after);
        Assert.assertTrue(creation >= before);
    }

    @Test
    public void testAttachment() {
        AbstractIoSession aio = new DummySession(mock(IoService.class));
        String value = "value";
        Assert.assertNull(aio.getAttribute("test"));
        Assert.assertEquals(null, aio.setAttribute("test", value));
        Assert.assertTrue(aio.containsAttribute("test"));
        Assert.assertEquals(aio.getAttributeNames().size(), 1);
        Assert.assertEquals(value, aio.setAttribute("test", value));
        Assert.assertEquals(aio.getAttributeNames().size(), 1);
        Assert.assertTrue(aio.containsAttribute("test"));
        Assert.assertEquals(value, aio.getAttribute("test"));
        Assert.assertEquals(value, aio.removeAttribute("test"));
        Assert.assertEquals(aio.getAttributeNames().size(), 0);
        Assert.assertFalse(aio.containsAttribute("test"));

        Assert.assertEquals(null, aio.getAttribute("test"));
        Assert.assertNotNull(aio.getService());
    }

}
