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
package org.apache.mina.filter.codec.textline;

import java.net.SocketAddress;
import java.nio.charset.Charset;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.common.support.BaseIoSession;
import org.apache.mina.filter.codec.support.SimpleProtocolEncoderOutput;

/**
 * Tests {@link TextLineEncoder}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class TextLineEncoderTest extends TestCase {
    public static void main(String[] args) {
        junit.textui.TestRunner.run(TextLineEncoderTest.class);
    }

    public void testEncode() throws Exception {
        TextLineEncoder encoder = new TextLineEncoder(Charset.forName("UTF-8"),
                LineDelimiter.WINDOWS);
        IoSession session = new DummySession();
        SimpleProtocolEncoderOutput out = new SimpleProtocolEncoderOutput() {
            @Override
            protected WriteFuture doFlush(ByteBuffer buf) {
                return null;
            }
        };

        encoder.encode(session, "ABC", out);
        Assert.assertEquals(1, out.getBufferQueue().size());
        ByteBuffer buf = out.getBufferQueue().poll();
        Assert.assertEquals(5, buf.remaining());
        Assert.assertEquals('A', buf.get());
        Assert.assertEquals('B', buf.get());
        Assert.assertEquals('C', buf.get());
        Assert.assertEquals('\r', buf.get());
        Assert.assertEquals('\n', buf.get());
    }

    private static class DummySession extends BaseIoSession {
        @Override
        protected void updateTrafficMask() {
        }

        public IoService getService() {
            return null;
        }

        public IoServiceConfig getServiceConfig() {
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
    }
}
