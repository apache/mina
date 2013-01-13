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
package org.apache.mina.service.executor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.apache.mina.api.IoSession;
import org.junit.Test;

/**
 * Unit test for {@link OrderedHandlerExecutor}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class OrderedHandlerExecutorTest {
    private OrderedHandlerExecutor executor;

    @Test
    public void execute_open_events() throws InterruptedException {
        // prepare
        executor = new OrderedHandlerExecutor(1, 1);
        IoSession session = mock(IoSession.class);
        when(session.getId()).thenReturn(12345L);

        Event evt = mock(Event.class);
        when(evt.getSession()).thenReturn(session);

        // run

        executor.execute(evt);

        // verify
        verify(session).getId();
        verify(evt).getSession();
        Thread.sleep(200);
        verify(evt).visit(any(EventVisitor.class));
        verifyNoMoreInteractions(evt, session);
    }
}
