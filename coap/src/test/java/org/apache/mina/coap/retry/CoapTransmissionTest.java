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

import static org.junit.Assert.*;

import org.apache.mina.api.IoSession;
import org.apache.mina.coap.CoapMessage;
import org.apache.mina.coap.MessageType;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link CoapTransmission}
 */
public class CoapTransmissionTest {

    private static final long MIN_INIT_TIMEOUT = 2000L;
    private static final long MAX_INIT_TIMEOUT = 3000L;

    @Test
    public void timeout() {

        IoSession session = Mockito.mock(IoSession.class);
        Mockito.when(session.getId()).thenReturn(1L);

        CoapTransmission transmission = new CoapTransmission(session, new CoapMessage(1, MessageType.CONFIRMABLE, 1,
                1234, "token".getBytes(), null, "payload".getBytes()));

        assertTrue(transmission.getNextTimeout() > MIN_INIT_TIMEOUT);
        assertTrue(transmission.getNextTimeout() < MAX_INIT_TIMEOUT);

        // timeout #1
        assertTrue(transmission.timeout());
        assertTrue(transmission.getNextTimeout() > MIN_INIT_TIMEOUT * 2);
        assertTrue(transmission.getNextTimeout() < MAX_INIT_TIMEOUT * 2);

        // timeout #2
        assertTrue(transmission.timeout());
        assertTrue(transmission.getNextTimeout() > MIN_INIT_TIMEOUT * 4);
        assertTrue(transmission.getNextTimeout() < MAX_INIT_TIMEOUT * 4);

        // timeout #3
        assertTrue(transmission.timeout());
        assertTrue(transmission.getNextTimeout() > MIN_INIT_TIMEOUT * 8);
        assertTrue(transmission.getNextTimeout() < MAX_INIT_TIMEOUT * 8);

        // timeout #4
        assertTrue(transmission.timeout());
        assertTrue(transmission.getNextTimeout() > MIN_INIT_TIMEOUT * 16);
        assertTrue(transmission.getNextTimeout() < MAX_INIT_TIMEOUT * 16);

        // timeout #5 - no retry
        assertFalse(transmission.timeout());
    }
}
