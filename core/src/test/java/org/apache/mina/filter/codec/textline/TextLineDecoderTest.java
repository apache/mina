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
import java.nio.charset.CharsetEncoder;
import java.util.LinkedList;
import java.util.Queue;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.common.BufferDataException;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.support.BaseIoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * Tests {@link TextLineDecoder}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class TextLineDecoderTest extends TestCase {
    public static void main(String[] args) {
        junit.textui.TestRunner.run(TextLineDecoderTest.class);
    }

    public void testNormalDecode() throws Exception {
        TextLineDecoder decoder = new TextLineDecoder(Charset.forName("UTF-8"),
                LineDelimiter.WINDOWS);

        CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
        IoSession session = new DummySession();
        TestDecoderOutput out = new TestDecoderOutput();
        ByteBuffer in = ByteBuffer.allocate(16);

        // Test one decode and one output
        in.putString("ABC\r\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(1, out.getMessageQueue().size());
        Assert.assertEquals("ABC", out.getMessageQueue().poll());

        // Test two decode and one output
        in.clear();
        in.putString("DEF", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, out.getMessageQueue().size());
        in.clear();
        in.putString("GHI\r\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(1, out.getMessageQueue().size());
        Assert.assertEquals("DEFGHI", out.getMessageQueue().poll());

        // Test one decode and two output
        in.clear();
        in.putString("JKL\r\nMNO\r\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(2, out.getMessageQueue().size());
        Assert.assertEquals("JKL", out.getMessageQueue().poll());
        Assert.assertEquals("MNO", out.getMessageQueue().poll());

        // Test splitted long delimiter
        decoder = new TextLineDecoder(Charset.forName("UTF-8"),
                new LineDelimiter("\n\n\n"));
        in.clear();
        in.putString("PQR\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, out.getMessageQueue().size());
        in.clear();
        in.putString("\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, out.getMessageQueue().size());
        in.clear();
        in.putString("\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(1, out.getMessageQueue().size());
        Assert.assertEquals("PQR", out.getMessageQueue().poll());

        // Test splitted long delimiter which produces two output
        decoder = new TextLineDecoder(Charset.forName("UTF-8"),
                new LineDelimiter("\n\n\n"));
        in.clear();
        in.putString("PQR\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, out.getMessageQueue().size());
        in.clear();
        in.putString("\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, out.getMessageQueue().size());
        in.clear();
        in.putString("\nSTU\n\n\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(2, out.getMessageQueue().size());
        Assert.assertEquals("PQR", out.getMessageQueue().poll());
        Assert.assertEquals("STU", out.getMessageQueue().poll());

        // Test splitted long delimiter mixed with partial non-delimiter.
        decoder = new TextLineDecoder(Charset.forName("UTF-8"),
                new LineDelimiter("\n\n\n"));
        in.clear();
        in.putString("PQR\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, out.getMessageQueue().size());
        in.clear();
        in.putString("X\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, out.getMessageQueue().size());
        in.clear();
        in.putString("\n\nSTU\n\n\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(2, out.getMessageQueue().size());
        Assert.assertEquals("PQR\nX", out.getMessageQueue().poll());
        Assert.assertEquals("STU", out.getMessageQueue().poll());
    }

    public void testAutoDecode() throws Exception {
        TextLineDecoder decoder = new TextLineDecoder(Charset.forName("UTF-8"),
                LineDelimiter.AUTO);

        CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
        IoSession session = new DummySession();
        TestDecoderOutput out = new TestDecoderOutput();
        ByteBuffer in = ByteBuffer.allocate(16);

        // Test one decode and one output
        in.putString("ABC\r\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(1, out.getMessageQueue().size());
        Assert.assertEquals("ABC", out.getMessageQueue().poll());

        // Test two decode and one output
        in.clear();
        in.putString("DEF", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, out.getMessageQueue().size());
        in.clear();
        in.putString("GHI\r\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(1, out.getMessageQueue().size());
        Assert.assertEquals("DEFGHI", out.getMessageQueue().poll());

        // Test one decode and two output
        in.clear();
        in.putString("JKL\r\nMNO\r\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(2, out.getMessageQueue().size());
        Assert.assertEquals("JKL", out.getMessageQueue().poll());
        Assert.assertEquals("MNO", out.getMessageQueue().poll());

        // Test multiple '\n's
        in.clear();
        in.putString("\n\n\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(3, out.getMessageQueue().size());
        Assert.assertEquals("", out.getMessageQueue().poll());
        Assert.assertEquals("", out.getMessageQueue().poll());
        Assert.assertEquals("", out.getMessageQueue().poll());

        // Test splitted long delimiter (\r\r\n)
        in.clear();
        in.putString("PQR\r", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, out.getMessageQueue().size());
        in.clear();
        in.putString("\r", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, out.getMessageQueue().size());
        in.clear();
        in.putString("\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(1, out.getMessageQueue().size());
        Assert.assertEquals("PQR", out.getMessageQueue().poll());

        // Test splitted long delimiter (\r\r\n) which produces two output
        in.clear();
        in.putString("PQR\r", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, out.getMessageQueue().size());
        in.clear();
        in.putString("\r", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, out.getMessageQueue().size());
        in.clear();
        in.putString("\nSTU\r\r\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(2, out.getMessageQueue().size());
        Assert.assertEquals("PQR", out.getMessageQueue().poll());
        Assert.assertEquals("STU", out.getMessageQueue().poll());

        // Test splitted long delimiter mixed with partial non-delimiter.
        in.clear();
        in.putString("PQR\r", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, out.getMessageQueue().size());
        in.clear();
        in.putString("X\r", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, out.getMessageQueue().size());
        in.clear();
        in.putString("\r\nSTU\r\r\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(2, out.getMessageQueue().size());
        Assert.assertEquals("PQR\rX", out.getMessageQueue().poll());
        Assert.assertEquals("STU", out.getMessageQueue().poll());
    }
    
    public void testOverflow() throws Exception {
        TextLineDecoder decoder = new TextLineDecoder(Charset.forName("UTF-8"),
                LineDelimiter.AUTO);
        decoder.setMaxLineLength(3);

        CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
        IoSession session = new DummySession();
        TestDecoderOutput out = new TestDecoderOutput();
        ByteBuffer in = ByteBuffer.allocate(16);

        // Make sure the overflow exception is not thrown until
        // the delimiter is encountered.
        in.putString("A", encoder).flip().mark();
        decoder.decode(session, in.reset().mark(), out);
        Assert.assertEquals(0, out.getMessageQueue().size());
        decoder.decode(session, in.reset().mark(), out);
        Assert.assertEquals(0, out.getMessageQueue().size());
        decoder.decode(session, in.reset().mark(), out);
        Assert.assertEquals(0, out.getMessageQueue().size());
        decoder.decode(session, in.reset().mark(), out);
        Assert.assertEquals(0, out.getMessageQueue().size());
        
        in.clear().putString("A\r\nB\r\n", encoder).flip();
        try {
            decoder.decode(session, in, out);
            Assert.fail();
        } catch (BufferDataException e) {
            // Success!
        }
        
        decoder.decode(session, in, out);
        Assert.assertEquals(1, out.getMessageQueue().size());
        Assert.assertEquals("B", out.getMessageQueue().poll());

        // Make sure OOM is not thrown.
        long oldFreeMemory = Runtime.getRuntime().freeMemory();
        in = ByteBuffer.allocate(1048576 * 16).mark();
        for (int i = 0; i < 10; i ++) {
            decoder.decode(session, in.reset().mark(), out);
            Assert.assertEquals(0, out.getMessageQueue().size());

            // Memory consumption should be minimal.
            Assert.assertTrue(Runtime.getRuntime().freeMemory() - oldFreeMemory < 1048576); 
        }

        in.clear().putString("C\r\nD\r\n", encoder).flip();
        try {
            decoder.decode(session, in, out);
            Assert.fail();
        } catch (BufferDataException e) {
            // Success!
        }
        
        decoder.decode(session, in, out);
        Assert.assertEquals(1, out.getMessageQueue().size());
        Assert.assertEquals("D", out.getMessageQueue().poll());

        // Memory consumption should be minimal.
        Assert.assertTrue(Runtime.getRuntime().freeMemory() - oldFreeMemory < 1048576); 
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

    private static class TestDecoderOutput implements ProtocolDecoderOutput {
        private Queue<Object> messageQueue = new LinkedList<Object>();

        public void write(Object message) {
            messageQueue.add(message);
        }

        public Queue<Object> getMessageQueue() {
            return messageQueue;
        }

        public void flush() {
        }
    }
}
