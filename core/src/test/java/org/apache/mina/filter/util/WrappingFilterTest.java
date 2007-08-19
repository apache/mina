package org.apache.mina.filter.util;

import junit.framework.TestCase;
import org.apache.mina.common.*;
import org.apache.mina.util.DummySession;
import org.easymock.MockControl;

import java.util.List;
import java.util.ArrayList;

/**
 * Tests {@link WrappingFilter}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 567208 $, $Date: 2007-08-18 04:27:05 +0200 (za, 18 aug 2007) $
 */

public class WrappingFilterTest extends TestCase {

    private MockControl mockNextFilter;

    private IoSession session;

    private IoFilter.NextFilter nextFilter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        /*
         * Create the mocks.
         */
        session = new DummySession();
        mockNextFilter = MockControl.createControl(IoFilter.NextFilter.class);
        nextFilter = (IoFilter.NextFilter) mockNextFilter.getMock();
    }

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
        mockNextFilter.replay();
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
        mockNextFilter.verify();

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


    private static class MyWrappingFilter extends WrappingFilter {

        private List<IoEventType> eventsBefore = new ArrayList<IoEventType>();

        private List<IoEventType> eventsAfter = new ArrayList<IoEventType>();

        protected void wrapFilterAction(IoEventType eventType, IoSession session, WrappingFilter.FilterAction action) {
            eventsBefore.add(eventType);
            action.execute();
            eventsAfter.add(eventType);
        }
    }
}
