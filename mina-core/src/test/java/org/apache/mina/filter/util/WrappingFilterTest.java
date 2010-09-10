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
package org.apache.mina.filter.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterEvent;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoEventType;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link CommonEventFilter}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */

public class WrappingFilterTest {
    private IoSession session;

    private IoFilter.NextFilter nextFilter;

    @Before
    public void setUp() throws Exception {
        /*
         * Create the mocks.
         */
        session = new DummySession();
        nextFilter = EasyMock.createMock(IoFilter.NextFilter.class);
        //nextFilter = (IoFilter.NextFilter) mockNextFilter.getClass();
    }

    @Test
    public void testFilter() throws Exception {
        MyWrappingFilter wrappingFilter = new MyWrappingFilter();

        /* record expectations */
        Object message1 = "message one";
        Object message2 = "message two";
        WriteRequest writeRequest1 = new DefaultWriteRequest("test1");
        WriteRequest writeRequest2 = new DefaultWriteRequest("test2");
        Throwable cause = new Throwable("testing");

        nextFilter.sessionCreated(session);
        nextFilter.sessionOpened(session);
        nextFilter.sessionIdle(session, IdleStatus.READER_IDLE);
        nextFilter.messageReceived(session, message1);
        nextFilter.messageSent(session, writeRequest1);
        nextFilter.messageSent(session, writeRequest2);
        nextFilter.messageReceived(session, message2);
        nextFilter.filterWrite(session, writeRequest1);
        nextFilter.filterClose(session);
        nextFilter.exceptionCaught(session, cause);
        nextFilter.sessionClosed(session);

        /* replay */
        EasyMock.replay( nextFilter );
        wrappingFilter.sessionCreated(nextFilter, session);
        wrappingFilter.sessionOpened(nextFilter, session);
        wrappingFilter.sessionIdle(nextFilter, session, IdleStatus.READER_IDLE);
        wrappingFilter.messageReceived(nextFilter, session, message1);
        wrappingFilter.messageSent(nextFilter, session, writeRequest1);
        wrappingFilter.messageSent(nextFilter, session, writeRequest2);
        wrappingFilter.messageReceived(nextFilter, session, message2);
        wrappingFilter.filterWrite(nextFilter,session, writeRequest1);
        wrappingFilter.filterClose(nextFilter, session);
        wrappingFilter.exceptionCaught(nextFilter, session, cause);
        wrappingFilter.sessionClosed(nextFilter, session);

        /* verify */
        EasyMock.verify( nextFilter );

        /* check event lists */
        assertEquals(11, wrappingFilter.eventsBefore.size());
        assertEquals(IoEventType.SESSION_CREATED, wrappingFilter.eventsBefore.get(0));
        assertEquals(IoEventType.SESSION_OPENED, wrappingFilter.eventsBefore.get(1));
        assertEquals(IoEventType.SESSION_IDLE, wrappingFilter.eventsBefore.get(2));
        assertEquals(IoEventType.MESSAGE_RECEIVED, wrappingFilter.eventsBefore.get(3));
        assertEquals(IoEventType.MESSAGE_SENT, wrappingFilter.eventsBefore.get(4));
        assertEquals(IoEventType.MESSAGE_SENT, wrappingFilter.eventsBefore.get(5));
        assertEquals(IoEventType.MESSAGE_RECEIVED, wrappingFilter.eventsBefore.get(6));
        assertEquals(IoEventType.WRITE, wrappingFilter.eventsBefore.get(7));
        assertEquals(IoEventType.CLOSE, wrappingFilter.eventsBefore.get(8));
        assertEquals(IoEventType.EXCEPTION_CAUGHT, wrappingFilter.eventsBefore.get(9));
        assertEquals(IoEventType.SESSION_CLOSED, wrappingFilter.eventsBefore.get(10));
        assertEquals(wrappingFilter.eventsBefore,  wrappingFilter.eventsAfter);
    }


    private static class MyWrappingFilter extends CommonEventFilter {
        List<IoEventType> eventsBefore = new ArrayList<IoEventType>();

        List<IoEventType> eventsAfter = new ArrayList<IoEventType>();

        /**
         * Default constructor
         */
        public MyWrappingFilter() {
            super();
        }
        
        @Override
        protected void filter(IoFilterEvent event) {
            eventsBefore.add(event.getType());
            event.fire();
            eventsAfter.add(event.getType());
        }
    }
}
