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
package org.apache.mina.filter.query;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoSession;
import org.apache.mina.filterchain.ReadFilterChainController;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * Unit test for {@link RequestFilter}
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class RequestFilterTest {

    @SuppressWarnings("rawtypes")
    private RequestFilter rq = new RequestFilter();

    @Test
    public void session_open_initialize_in_flight_container() {
        IoSession session = mock(IoSession.class);

        // run
        rq.sessionOpened(session);

        // verify
        verify(session).setAttribute(same(RequestFilter.IN_FLIGHT_REQUESTS), any(Map.class));
        verifyNoMoreInteractions(session);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void request_and_produce_a_future() {
        IoSession session = mock(IoSession.class);

        Request r = mock(Request.class);
        when(r.requestId()).thenReturn("ID");

        Map m = mock(Map.class);

        when(session.getAttribute(RequestFilter.IN_FLIGHT_REQUESTS)).thenReturn(m);

        // run
        IoFuture f = rq.request(session, r, 12345);

        // verify
        Assert.assertFalse(f.isDone());
        Assert.assertFalse(f.isCancelled());

        verify(r, times(2)).requestId();
        verify(session).write(r);
        verify(m).put("ID", f);
        verify(session).getAttribute(RequestFilter.IN_FLIGHT_REQUESTS);
        verifyNoMoreInteractions(session, m);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void receive_a_messagre_and_find_the_future_to_complete() {
        IoSession session = mock(IoSession.class);

        Response r = mock(Response.class);
        when(r.requestId()).thenReturn("ID");

        Map m = mock(Map.class);

        when(session.getAttribute(RequestFilter.IN_FLIGHT_REQUESTS)).thenReturn(m);

        RequestFuture f = mock(RequestFuture.class);

        when(m.remove("ID")).thenReturn(f);

        ReadFilterChainController ctl = mock(ReadFilterChainController.class);

        // run
        rq.messageReceived(session, r, ctl);

        // verify
        verify(session).getAttribute(RequestFilter.IN_FLIGHT_REQUESTS);
        verify(m).remove("ID");
        verify(r).requestId();
        verify(f).complete(r);

        verify(ctl).callReadNextFilter(r);
        verifyNoMoreInteractions(r, m, session, f, ctl);
    }
}
