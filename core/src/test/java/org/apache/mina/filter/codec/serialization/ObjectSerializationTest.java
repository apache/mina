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
package org.apache.mina.filter.codec.serialization;

import java.io.ByteArrayOutputStream;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.common.support.BaseIoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.support.SimpleProtocolEncoderOutput;

/**
 * Tests object serialization codec and streams.
 * 
 * @author The Apache MINA Project Team (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class ObjectSerializationTest extends TestCase {
    public void testEncoder() throws Exception {
        final String expected = "1234";

        IoSession session = new MockIoSession();
        SimpleProtocolEncoderOutput out = new SimpleProtocolEncoderOutput() {
            protected WriteFuture doFlush(ByteBuffer buf) {
                return null;
            }
        };

        ProtocolEncoder encoder = new ObjectSerializationEncoder();
        encoder.encode(session, expected, out);

        Assert.assertEquals(1, out.getBufferQueue().size());
        ByteBuffer buf = out.getBufferQueue().poll();

        testDecoderAndInputStream(expected, buf);
    }

    public void testOutputStream() throws Exception {
        final String expected = "1234";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectSerializationOutputStream osos = new ObjectSerializationOutputStream(
                baos);

        osos.writeObject(expected);
        osos.flush();

        testDecoderAndInputStream(expected, ByteBuffer.wrap(baos.toByteArray()));
    }

    private void testDecoderAndInputStream(String expected, ByteBuffer in)
            throws Exception {
        // Test InputStream
        ObjectSerializationInputStream osis = new ObjectSerializationInputStream(
                in.duplicate().asInputStream());

        Object actual = osis.readObject();
        assertEquals(expected, actual);

        // Test ProtocolDecoder
        ProtocolDecoder decoder = new ObjectSerializationDecoder();
        MockProtocolDecoderOutput decoderOut = new MockProtocolDecoderOutput();
        IoSession session = new MockIoSession();
        decoder.decode(session, in.duplicate(), decoderOut);

        Assert.assertEquals(expected, decoderOut.result.get(0));
        Assert.assertEquals(1, decoderOut.result.size());
    }

    private static class MockIoSession extends BaseIoSession {

        protected void updateTrafficMask() {
        }

        public IoSessionConfig getConfig() {
            return null;
        }

        public IoFilterChain getFilterChain() {
            return null;
        }

        public IoHandler getHandler() {
            return null;
        }

        public SocketAddress getLocalAddress() {
            return null;
        }

        public SocketAddress getRemoteAddress() {
            return null;
        }

        public int getScheduledWriteBytes() {
            return 0;
        }

        public int getScheduledWriteRequests() {
            return 0;
        }

        public IoService getService() {
            return null;
        }

        public SocketAddress getServiceAddress() {
            return null;
        }

        public IoServiceConfig getServiceConfig() {
            return null;
        }

        public TransportType getTransportType() {
            return null;
        }
    }

    private static class MockProtocolDecoderOutput implements
            ProtocolDecoderOutput {
        private List<Object> result = new ArrayList<Object>();

        public void flush() {
        }

        public void write(Object message) {
            result.add(message);
        }
    }
}
