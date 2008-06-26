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

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolCodecSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.RecoverableProtocolDecoderException;

/**
 * Tests {@link TextLineDecoder}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
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
        ProtocolCodecSession session = new ProtocolCodecSession();
        ProtocolDecoderOutput out = session.getDecoderOutput();
        IoBuffer in = IoBuffer.allocate(16);

        // Test one decode and one output
        in.putString("ABC\r\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(1, session.getDecoderOutputQueue().size());
        Assert.assertEquals("ABC", session.getDecoderOutputQueue().poll());

        // Test two decode and one output
        in.clear();
        in.putString("DEF", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, session.getDecoderOutputQueue().size());
        in.clear();
        in.putString("GHI\r\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(1, session.getDecoderOutputQueue().size());
        Assert.assertEquals("DEFGHI", session.getDecoderOutputQueue().poll());

        // Test one decode and two output
        in.clear();
        in.putString("JKL\r\nMNO\r\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(2, session.getDecoderOutputQueue().size());
        Assert.assertEquals("JKL", session.getDecoderOutputQueue().poll());
        Assert.assertEquals("MNO", session.getDecoderOutputQueue().poll());

        // Test aborted delimiter (DIRMINA-506)
        in.clear();
        in.putString("ABC\r\r\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(1, session.getDecoderOutputQueue().size());
        Assert.assertEquals("ABC\r", session.getDecoderOutputQueue().poll());

        // Test splitted long delimiter
        decoder = new TextLineDecoder(Charset.forName("UTF-8"),
                new LineDelimiter("\n\n\n"));
        in.clear();
        in.putString("PQR\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, session.getDecoderOutputQueue().size());
        in.clear();
        in.putString("\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, session.getDecoderOutputQueue().size());
        in.clear();
        in.putString("\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(1, session.getDecoderOutputQueue().size());
        Assert.assertEquals("PQR", session.getDecoderOutputQueue().poll());

        // Test splitted long delimiter which produces two output
        decoder = new TextLineDecoder(Charset.forName("UTF-8"),
                new LineDelimiter("\n\n\n"));
        in.clear();
        in.putString("PQR\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, session.getDecoderOutputQueue().size());
        in.clear();
        in.putString("\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, session.getDecoderOutputQueue().size());
        in.clear();
        in.putString("\nSTU\n\n\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(2,session.getDecoderOutputQueue().size());
        Assert.assertEquals("PQR", session.getDecoderOutputQueue().poll());
        Assert.assertEquals("STU", session.getDecoderOutputQueue().poll());

        // Test splitted long delimiter mixed with partial non-delimiter.
        decoder = new TextLineDecoder(Charset.forName("UTF-8"),
                new LineDelimiter("\n\n\n"));
        in.clear();
        in.putString("PQR\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, session.getDecoderOutputQueue().size());
        in.clear();
        in.putString("X\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, session.getDecoderOutputQueue().size());
        in.clear();
        in.putString("\n\nSTU\n\n\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(2, session.getDecoderOutputQueue().size());
        Assert.assertEquals("PQR\nX", session.getDecoderOutputQueue().poll());
        Assert.assertEquals("STU", session.getDecoderOutputQueue().poll());
    }

    public void testAutoDecode() throws Exception {
        TextLineDecoder decoder = new TextLineDecoder(Charset.forName("UTF-8"),
                LineDelimiter.AUTO);

        CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
        ProtocolCodecSession session = new ProtocolCodecSession();
        ProtocolDecoderOutput out = session.getDecoderOutput();
        IoBuffer in = IoBuffer.allocate(16);

        // Test one decode and one output
        in.putString("ABC\r\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(1, session.getDecoderOutputQueue().size());
        Assert.assertEquals("ABC", session.getDecoderOutputQueue().poll());

        // Test two decode and one output
        in.clear();
        in.putString("DEF", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, session.getDecoderOutputQueue().size());
        in.clear();
        in.putString("GHI\r\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(1, session.getDecoderOutputQueue().size());
        Assert.assertEquals("DEFGHI", session.getDecoderOutputQueue().poll());

        // Test one decode and two output
        in.clear();
        in.putString("JKL\r\nMNO\r\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(2, session.getDecoderOutputQueue().size());
        Assert.assertEquals("JKL", session.getDecoderOutputQueue().poll());
        Assert.assertEquals("MNO", session.getDecoderOutputQueue().poll());

        // Test multiple '\n's
        in.clear();
        in.putString("\n\n\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(3, session.getDecoderOutputQueue().size());
        Assert.assertEquals("", session.getDecoderOutputQueue().poll());
        Assert.assertEquals("", session.getDecoderOutputQueue().poll());
        Assert.assertEquals("", session.getDecoderOutputQueue().poll());

        // Test splitted long delimiter (\r\r\n)
        in.clear();
        in.putString("PQR\r", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, session.getDecoderOutputQueue().size());
        in.clear();
        in.putString("\r", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, session.getDecoderOutputQueue().size());
        in.clear();
        in.putString("\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(1, session.getDecoderOutputQueue().size());
        Assert.assertEquals("PQR", session.getDecoderOutputQueue().poll());

        // Test splitted long delimiter (\r\r\n) which produces two output
        in.clear();
        in.putString("PQR\r", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, session.getDecoderOutputQueue().size());
        in.clear();
        in.putString("\r", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, session.getDecoderOutputQueue().size());
        in.clear();
        in.putString("\nSTU\r\r\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(2, session.getDecoderOutputQueue().size());
        Assert.assertEquals("PQR", session.getDecoderOutputQueue().poll());
        Assert.assertEquals("STU", session.getDecoderOutputQueue().poll());

        // Test splitted long delimiter mixed with partial non-delimiter.
        in.clear();
        in.putString("PQR\r", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, session.getDecoderOutputQueue().size());
        in.clear();
        in.putString("X\r", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(0, session.getDecoderOutputQueue().size());
        in.clear();
        in.putString("\r\nSTU\r\r\n", encoder);
        in.flip();
        decoder.decode(session, in, out);
        Assert.assertEquals(2, session.getDecoderOutputQueue().size());
        Assert.assertEquals("PQR\rX", session.getDecoderOutputQueue().poll());
        Assert.assertEquals("STU", session.getDecoderOutputQueue().poll());
    }

    public void testOverflow() throws Exception {
        TextLineDecoder decoder = new TextLineDecoder(Charset.forName("UTF-8"),
                LineDelimiter.AUTO);
        decoder.setMaxLineLength(3);

        CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
        ProtocolCodecSession session = new ProtocolCodecSession();
        ProtocolDecoderOutput out = session.getDecoderOutput();
        IoBuffer in = IoBuffer.allocate(16);

        // Make sure the overflow exception is not thrown until
        // the delimiter is encountered.
        in.putString("A", encoder).flip().mark();
        decoder.decode(session, in.reset().mark(), out);
        Assert.assertEquals(0, session.getDecoderOutputQueue().size());
        decoder.decode(session, in.reset().mark(), out);
        Assert.assertEquals(0, session.getDecoderOutputQueue().size());
        decoder.decode(session, in.reset().mark(), out);
        Assert.assertEquals(0, session.getDecoderOutputQueue().size());
        decoder.decode(session, in.reset().mark(), out);
        Assert.assertEquals(0, session.getDecoderOutputQueue().size());

        in.clear().putString("A\r\nB\r\n", encoder).flip();
        try {
            decoder.decode(session, in, out);
            Assert.fail();
        } catch (RecoverableProtocolDecoderException e) {
            // Success!
        }

        decoder.decode(session, in, out);
        Assert.assertEquals(1, session.getDecoderOutputQueue().size());
        Assert.assertEquals("B", session.getDecoderOutputQueue().poll());

        // Make sure OOM is not thrown.
        System.gc();
        long oldFreeMemory = Runtime.getRuntime().freeMemory();
        in = IoBuffer.allocate(1048576 * 16).sweep((byte) ' ').mark();
        for (int i = 0; i < 10; i ++) {
            decoder.decode(session, in.reset().mark(), out);
            Assert.assertEquals(0, session.getDecoderOutputQueue().size());

            // Memory consumption should be minimal.
            Assert.assertTrue(Runtime.getRuntime().freeMemory() - oldFreeMemory < 1048576);
        }

        in.clear().putString("C\r\nD\r\n", encoder).flip();
        try {
            decoder.decode(session, in, out);
            Assert.fail();
        } catch (RecoverableProtocolDecoderException e) {
            // Success!
        }

        decoder.decode(session, in, out);
        Assert.assertEquals(1, session.getDecoderOutputQueue().size());
        Assert.assertEquals("D", session.getDecoderOutputQueue().poll());

        // Memory consumption should be minimal.
        Assert.assertTrue(Runtime.getRuntime().freeMemory() - oldFreeMemory < 1048576);
    }
    
    public void testSMTPDataBounds() throws Exception {
        TextLineDecoder decoder = new TextLineDecoder(Charset.forName("ISO-8859-1"),
                new LineDelimiter("\r\n.\r\n"));

        CharsetEncoder encoder = Charset.forName("ISO-8859-1").newEncoder();
        ProtocolCodecSession session = new ProtocolCodecSession();
        IoBuffer in = IoBuffer.allocate(16).setAutoExpand(true);

        in.putString("\r\n", encoder).flip().mark();
        decoder.decode(session, in.reset().mark(), session.getDecoderOutput());
        Assert.assertEquals(0, session.getDecoderOutputQueue().size());
        in.putString("Body\r\n.\r\n", encoder).flip().mark();
        decoder.decode(session, in.reset().mark(), session.getDecoderOutput());
        Assert.assertEquals(1, session.getDecoderOutputQueue().size());
        Assert.assertEquals("\r\n\r\nBody", session.getDecoderOutputQueue().poll());
    }
}
