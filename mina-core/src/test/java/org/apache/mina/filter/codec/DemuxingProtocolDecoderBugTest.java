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
package org.apache.mina.filter.codec;

import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.apache.mina.filter.codec.demux.MessageDecoderAdapter;
import org.apache.mina.filter.codec.demux.DemuxingProtocolDecoder;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.file.FileRegion;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Simple Unit Test showing that the DemuxingProtocolDecoder has
 * inconsistent behavior if used with a non fragmented transport.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
*/
public class DemuxingProtocolDecoderBugTest {

    private static void doTest(IoSession session) throws Exception {
        ProtocolDecoderOutput output = mock(ProtocolDecoderOutput.class);

        IoBuffer buffer = IoBuffer.allocate(1000);
        buffer.putString("AB12C", Charset.defaultCharset().newEncoder());
        buffer.flip();

        DemuxingProtocolDecoder decoder = new DemuxingProtocolDecoder();
        decoder.addMessageDecoder(CharacterMessageDecoder.class);
        decoder.addMessageDecoder(IntegerMessageDecoder.class);

        decoder.decode(session, buffer, output);

        verify(output).write(Character.valueOf('A'));
        verify(output).write(Character.valueOf('B'));
        verify(output).write(Integer.valueOf(1));
        verify(output).write(Integer.valueOf(2));
        verify(output).write(Character.valueOf('C'));

        verifyNoMoreInteractions(output);
    }

    public static class CharacterMessageDecoder extends MessageDecoderAdapter {
        public MessageDecoderResult decodable(IoSession session, IoBuffer in) {
            return Character.isDigit((char) in.get()) ? MessageDecoderResult.NOT_OK : MessageDecoderResult.OK;
        }

        public MessageDecoderResult decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) {
            out.write(Character.valueOf((char) in.get()));
            return MessageDecoderResult.OK;
        }
    }

    public static class IntegerMessageDecoder extends MessageDecoderAdapter {
        public MessageDecoderResult decodable(IoSession session, IoBuffer in) {
            return Character.isDigit((char) in.get()) ? MessageDecoderResult.OK : MessageDecoderResult.NOT_OK;
        }

        public MessageDecoderResult decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) {
            out.write(Integer.parseInt("" + (char) in.get()));
            return MessageDecoderResult.OK;
        }
    }

    private static class SessionStub extends DummySession {
        public SessionStub(boolean fragmented) {
            setTransportMetadata(new DefaultTransportMetadata("nio", "socket", false, fragmented,
                    InetSocketAddress.class, SocketSessionConfig.class, IoBuffer.class, FileRegion.class));
        }
    }

    /**
     * Test a decoding with fragmentation
     * @throws Exception If the test failed
     */
    @Test
    public void testFragmentedTransport() throws Exception {
        doTest(new SessionStub(true));
    }

    /**
     * Test a decoding without fragmentation
     * @throws Exception If the test failed
     */
    @Test
    public void testNonFragmentedTransport() throws Exception {
        doTest(new SessionStub(false));
    }
}
