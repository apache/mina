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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolCodecSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.junit.Test;

/**
 * Tests object serialization codec and streams.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ObjectSerializationTest {
    @Test
    public void testEncoder() throws Exception {
        final String expected = "1234";

        ProtocolCodecSession session = new ProtocolCodecSession();
        ProtocolEncoderOutput out = session.getEncoderOutput();

        ProtocolEncoder encoder = new ObjectSerializationEncoder();
        encoder.encode(session, expected, out);

        assertEquals(1, session.getEncoderOutputQueue().size());
        IoBuffer buf = (IoBuffer) session.getEncoderOutputQueue().poll();

        testDecoderAndInputStream(expected, buf);
    }

    @Test
    public void testOutputStream() throws Exception {
        final String expected = "1234";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectSerializationOutputStream osos = new ObjectSerializationOutputStream(
                baos);

        osos.writeObject(expected);
        osos.flush();

        testDecoderAndInputStream(expected, IoBuffer.wrap(baos.toByteArray()));
    }

    private void testDecoderAndInputStream(String expected, IoBuffer in)
            throws Exception {
        // Test InputStream
        ObjectSerializationInputStream osis = new ObjectSerializationInputStream(
                in.duplicate().asInputStream());

        Object actual = osis.readObject();
        assertEquals(expected, actual);

        // Test ProtocolDecoder
        ProtocolDecoder decoder = new ObjectSerializationDecoder();
        ProtocolCodecSession session = new ProtocolCodecSession();
        ProtocolDecoderOutput decoderOut = session.getDecoderOutput();
        decoder.decode(session, in.duplicate(), decoderOut);

        assertEquals(1, session.getDecoderOutputQueue().size());
        assertEquals(expected, session.getDecoderOutputQueue().poll());
    }
}
