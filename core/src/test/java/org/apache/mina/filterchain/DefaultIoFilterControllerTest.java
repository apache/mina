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
package org.apache.mina.filterchain;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.nio.ByteBuffer;

import org.apache.mina.api.DefaultIoFilter;
import org.apache.mina.api.IoFilter;
import org.apache.mina.api.IoSession;
import org.junit.Test;

public class DefaultIoFilterControllerTest {

    @Test
    public void constructor() {

        // null chain
        try {
            new DefaultIoFilterController(null);
            fail();
        } catch (IllegalArgumentException e) {
            // happy
        }

        // nominal
        try {
            DefaultIoFilterController ctrl = new DefaultIoFilterController(new IoFilter[] {});
        } catch (IllegalArgumentException e) {
            fail();
        }
    }

    @Test
    public void chain_reads() {
        IoFilter filter1 = spy(new PassthruFilter());
        IoFilter filter2 = spy(new PassthruFilter());
        IoFilter filter3 = spy(new PassthruFilter());
        DefaultIoFilterController ctrl = new DefaultIoFilterController(new IoFilter[] { filter1, filter2, filter3 });
        IoSession session = mock(IoSession.class);
        ByteBuffer buffer = mock(ByteBuffer.class);
        ctrl.processMessageReceived(session, buffer);
        verify(filter1).messageReceived(eq(session), eq(buffer), any(ReadFilterChainController.class));
        verify(filter2).messageReceived(eq(session), eq(buffer), any(ReadFilterChainController.class));
        verify(filter3).messageReceived(eq(session), eq(buffer), any(ReadFilterChainController.class));
    }

    @Test
    public void chain_writes() {
        IoFilter filter1 = spy(new PassthruFilter());
        IoFilter filter2 = spy(new PassthruFilter());
        IoFilter filter3 = spy(new PassthruFilter());
        DefaultIoFilterController ctrl = new DefaultIoFilterController(new IoFilter[] { filter1, filter2, filter3 });
        IoSession session = mock(IoSession.class);
        ByteBuffer buffer = mock(ByteBuffer.class);
        ctrl.processMessageWriting(session, buffer);
        verify(filter1).messageWriting(eq(session), eq(buffer), any(WriteFilterChainController.class));
        verify(filter2).messageWriting(eq(session), eq(buffer), any(WriteFilterChainController.class));
        verify(filter3).messageWriting(eq(session), eq(buffer), any(WriteFilterChainController.class));
        verify(session).enqueueWriteRequest(eq(buffer));
    }

    @Test
    public void chain_created() {
        IoFilter filter1 = spy(new PassthruFilter());
        IoFilter filter2 = spy(new PassthruFilter());
        IoFilter filter3 = spy(new PassthruFilter());
        DefaultIoFilterController ctrl = new DefaultIoFilterController(new IoFilter[] { filter1, filter2, filter3 });
        IoSession session = mock(IoSession.class);
        ctrl.processSessionCreated(session);
        verify(filter1).sessionCreated(eq(session));
        verify(filter2).sessionCreated(eq(session));
        verify(filter3).sessionCreated(eq(session));
    }

    @Test
    public void chain_open() {
        IoFilter filter1 = spy(new PassthruFilter());
        IoFilter filter2 = spy(new PassthruFilter());
        IoFilter filter3 = spy(new PassthruFilter());
        DefaultIoFilterController ctrl = new DefaultIoFilterController(new IoFilter[] { filter1, filter2, filter3 });
        IoSession session = mock(IoSession.class);
        ctrl.processSessionOpened(session);
        verify(filter1).sessionOpened(eq(session));
        verify(filter2).sessionOpened(eq(session));
        verify(filter3).sessionOpened(eq(session));
    }

    @Test
    public void chain_close() {
        IoFilter filter1 = spy(new PassthruFilter());
        IoFilter filter2 = spy(new PassthruFilter());
        IoFilter filter3 = spy(new PassthruFilter());
        DefaultIoFilterController ctrl = new DefaultIoFilterController(new IoFilter[] { filter1, filter2, filter3 });
        IoSession session = mock(IoSession.class);
        ctrl.processSessionClosed(session);
        verify(filter1).sessionClosed(eq(session));
        verify(filter2).sessionClosed(eq(session));
        verify(filter3).sessionClosed(eq(session));
    }

    private class PassthruFilter extends DefaultIoFilter {

    }

}
