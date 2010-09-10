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
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.file.FileRegion;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.easymock.EasyMock;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;


/**
 * Simple Unit Test showing that the DemuxingProtocolDecoder has
 * inconsistent behavior if used with a non fragmented transport.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
*/
public class DemuxingProtocolDecoderBugTest
{

    private static void doTest(IoSession session) throws Exception
    {
        ProtocolDecoderOutput mock = EasyMock.createMock(ProtocolDecoderOutput.class);
        mock.write(Character.valueOf('A'));
        mock.write(Character.valueOf('B'));
        mock.write(Integer.valueOf(1));
        mock.write(Integer.valueOf(2));
        mock.write(Character.valueOf('C'));
        EasyMock.replay(mock);

        IoBuffer buffer = IoBuffer.allocate(1000);
        buffer.putString("AB12C", Charset.defaultCharset().newEncoder());
        buffer.flip();

        DemuxingProtocolDecoder decoder = new DemuxingProtocolDecoder();
        decoder.addMessageDecoder(CharacterMessageDecoder.class);
        decoder.addMessageDecoder(IntegerMessageDecoder.class);

        decoder.decode(session,buffer,mock);

        EasyMock.verify(mock);
    }

    public static class CharacterMessageDecoder extends MessageDecoderAdapter
    {
        public MessageDecoderResult decodable(IoSession session, IoBuffer in)
        {
            return Character.isDigit((char)in.get())
                    ? MessageDecoderResult.NOT_OK
                    : MessageDecoderResult.OK;
        }

        public MessageDecoderResult decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception
        {
            out.write(Character.valueOf((char)in.get()));
            return MessageDecoderResult.OK;
        }
    }

    public static class IntegerMessageDecoder extends MessageDecoderAdapter
    {
        public MessageDecoderResult decodable(IoSession session, IoBuffer in)
        {
            return Character.isDigit((char)in.get())
                    ? MessageDecoderResult.OK
                    : MessageDecoderResult.NOT_OK;
        }

        public MessageDecoderResult decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception
        {
            out.write(Integer.parseInt("" + (char)in.get()));
            return MessageDecoderResult.OK;
        }
    }

    private static class SessionStub extends DummySession
    {
        public SessionStub(boolean fragmented)
        {
            setTransportMetadata(
                new DefaultTransportMetadata(
                        "nio", "socket", false, fragmented,
                        InetSocketAddress.class,
                        SocketSessionConfig.class,
                        IoBuffer.class, FileRegion.class)
            );
        }
    }

    /**
     * Test a decoding with fragmentation
     */
    @Test
    public void testFragmentedTransport() throws Exception
    {
        doTest(new SessionStub(true));
    }

    /**
     * Test a decoding without fragmentation
     */
    @Test
    public void testNonFragmentedTransport() throws Exception
    {
        doTest(new SessionStub(false));
    }
}
