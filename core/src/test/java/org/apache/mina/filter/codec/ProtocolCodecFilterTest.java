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

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.nio.ByteBuffer;

import org.apache.mina.api.IoSession;
import org.apache.mina.codec.ProtocolDecoder;
import org.apache.mina.codec.ProtocolEncoder;
import org.apache.mina.filterchain.ReadFilterChainController;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.apache.mina.session.AttributeKey;
import org.apache.mina.session.WriteRequest;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ProtocolCodecFilter}
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ProtocolCodecFilterTest {

    private ProtocolCodecFilter filter;

    private ProtocolEncoder encoder = mock(ProtocolEncoder.class);

    private ProtocolDecoder decoder = mock(ProtocolDecoder.class);

    @Before
    public void setup() {
        filter = new ProtocolCodecFilter(encoder, decoder);
    }

    @Test
    public void null_check() {
        try {
            new ProtocolCodecFilter(encoder, null);
            fail();
        } catch (IllegalArgumentException ex) {
            // happy
        }

        try {
            new ProtocolCodecFilter(null, decoder);
            fail();
        } catch (IllegalArgumentException ex) {
            // happy
        }
    }

    @Test
    public void create_states() {
        // prepare
        Object decodingState = new Object();
        Object encodingState = new Object();
        IoSession session = mock(IoSession.class);

        when(decoder.createDecoderState()).thenReturn(decodingState);
        when(encoder.createEncoderState()).thenReturn(encodingState);

        // run
        filter.sessionOpened(session);

        // verify
        verify(decoder).createDecoderState();
        verify(session).setAttribute(new AttributeKey<Object>(Object.class, "internal_decoder"), decodingState);
        verify(encoder).createEncoderState();
        verify(session).setAttribute(new AttributeKey<Object>(Object.class, "internal_encoder"), encodingState);
        verifyNoMoreInteractions(encoder, decoder, session);
    }

    @Test
    public void loop_decode_twice() {
        // prepare
        IoSession session = mock(IoSession.class);
        ByteBuffer buff = ByteBuffer.wrap("test".getBytes());

        Object decodingState = new Object();

        when(session.getAttribute(new AttributeKey<Object>(Object.class, "internal_decoder")))
                .thenReturn(decodingState);

        Object decoded = new Object();

        when(decoder.decode(buff, decodingState)).thenReturn(decoded).thenReturn(decoded).thenReturn(null);

        ReadFilterChainController ctrl = mock(ReadFilterChainController.class);

        // run
        filter.messageReceived(session, buff, ctrl);

        // verify
        verify(decoder, times(3)).decode(buff, decodingState);
        verify(ctrl, times(2)).callReadNextFilter(decoded);
        verify(session).getAttribute(new AttributeKey<Object>(Object.class, "internal_decoder"));
        verifyNoMoreInteractions(encoder, decoder, session, ctrl);
    }

    @Test
    public void encode() {
        // prepare
        IoSession session = mock(IoSession.class);
        ByteBuffer buff = ByteBuffer.wrap("test".getBytes());
        Object encodingState = new Object();

        when(session.getAttribute(new AttributeKey<Object>(Object.class, "internal_encoder")))
                .thenReturn(encodingState);

        Object toEncode = new Object();
        WriteRequest wrq = mock(WriteRequest.class);
        when(wrq.getMessage()).thenReturn(toEncode);

        when(encoder.encode(toEncode, encodingState)).thenReturn(buff);

        WriteFilterChainController ctrl = mock(WriteFilterChainController.class);

        // run
        filter.messageWriting(session, wrq, ctrl);

        // verify

        verify(encoder).encode(toEncode, encodingState);
        verify(wrq).getMessage();
        verify(wrq).setMessage(buff);

        verify(ctrl).callWriteNextFilter(wrq);
        verify(session).getAttribute(new AttributeKey<Object>(Object.class, "internal_encoder"));
        verifyNoMoreInteractions(encoder, decoder, session, ctrl, wrq);
    }
}
