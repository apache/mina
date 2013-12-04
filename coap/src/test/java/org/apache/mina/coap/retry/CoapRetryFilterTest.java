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
package org.apache.mina.coap.retry;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.apache.mina.api.IoSession;
import org.apache.mina.coap.CoapMessage;
import org.apache.mina.coap.MessageType;
import org.apache.mina.filterchain.ReadFilterChainController;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.apache.mina.session.DefaultWriteRequest;
import org.apache.mina.session.WriteRequest;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link CoapRetryFilter}
 */
public class CoapRetryFilterTest {

    private CoapRetryFilter filter = new CoapRetryFilter();

    private ReadFilterChainController readController = mock(ReadFilterChainController.class);

    private WriteFilterChainController writeController = mock(WriteFilterChainController.class);

    private IoSession session = mock(IoSession.class);

    @Test
    public void non_confirmable_message_received() {
        CoapMessage in = new CoapMessage(1, MessageType.NON_CONFIRMABLE, 1, 1234, "token".getBytes(), null,
                "payload".getBytes());

        filter.messageReceived(session, in, readController);

        // verify
        verify(readController).callReadNextFilter(in);

        Mockito.verifyNoMoreInteractions(readController);
    }

    @Test
    public void first_time_confirmable_message_received() {
        CoapMessage in = new CoapMessage(1, MessageType.CONFIRMABLE, 1, 1234, "token".getBytes(), null,
                "payload".getBytes());

        filter.messageReceived(session, in, readController);

        // verify
        verify(readController).callReadNextFilter(in);

        Mockito.verifyNoMoreInteractions(readController);
    }

    @Test
    public void duplicate_confirmable_processed_once() {
        CoapMessage in = new CoapMessage(1, MessageType.CONFIRMABLE, 1, 1234, "token".getBytes(), null,
                "payload".getBytes());

        // first confirmable
        filter.messageReceived(session, in, readController);

        // ack
        CoapMessage ack = new CoapMessage(1, MessageType.ACK, 1, 1234, null, null, null);
        filter.messageWriting(session, new DefaultWriteRequest(ack), writeController);

        // duplicate confirmable
        filter.messageReceived(session, in, readController);

        // verify
        verify(readController).callReadNextFilter(in);
        verify(readController).callWriteMessageForRead(ack);

        Mockito.verifyNoMoreInteractions(readController);
    }

    @Test
    public void retry_confirmable_message() throws InterruptedException {
        CoapMessage msg = new CoapMessage(1, MessageType.CONFIRMABLE, 1, 1234, null, null, null);

        WriteRequest writeRequest = new DefaultWriteRequest(msg);
        filter.messageWriting(session, writeRequest, writeController);

        // verify

        // wait more than the first timeout
        Thread.sleep(3500L);

        // first write
        verify(writeController).callWriteNextFilter(writeRequest);

        // retry
        session.write(msg);
    }

    @Test
    public void no_retry_if_ack_received() throws InterruptedException {

        // confirmable
        CoapMessage msg = new CoapMessage(1, MessageType.CONFIRMABLE, 1, 1234, null, null, null);
        WriteRequest writeRequest = new DefaultWriteRequest(msg);
        filter.messageWriting(session, writeRequest, writeController);

        // ack
        CoapMessage ack = new CoapMessage(1, MessageType.ACK, 1, 1234, null, null, null);
        filter.messageReceived(session, ack, readController);

        // wait more than the first timeout
        Thread.sleep(3500L);

        // first write
        verify(writeController).callWriteNextFilter(writeRequest);

        // no retry
        verify(session, Mockito.never()).write(any(CoapMessage.class));
    }

    @Test
    public void no_retry_if_reset_received() throws InterruptedException {

        // confirmable
        CoapMessage msg = new CoapMessage(1, MessageType.CONFIRMABLE, 1, 1234, null, null, null);
        WriteRequest writeRequest = new DefaultWriteRequest(msg);
        filter.messageWriting(session, writeRequest, writeController);

        // reset
        CoapMessage ack = new CoapMessage(1, MessageType.RESET, 1, 1234, null, null, null);
        filter.messageReceived(session, ack, readController);

        // wait more than the first timeout
        Thread.sleep(3500L);

        // first write
        verify(writeController).callWriteNextFilter(writeRequest);

        // no retry
        verify(session, Mockito.never()).write(any(CoapMessage.class));
    }

    @Test
    public void non_confirmable_message_writing() {
        CoapMessage msg = new CoapMessage(1, MessageType.NON_CONFIRMABLE, 1, 1234, "token".getBytes(), null,
                "payload".getBytes());
        WriteRequest writeRequest = new DefaultWriteRequest(msg);

        filter.messageWriting(session, writeRequest, writeController);

        // verify
        verify(writeController).callWriteNextFilter(writeRequest);

        Mockito.verifyNoMoreInteractions(writeController);
    }

}
