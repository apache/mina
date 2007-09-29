package org.apache.mina.filter.util;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoEvent;
import org.apache.mina.common.IoEventType;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteRequest;

/**
 * Extend this class when you want to create a filter that
 * wraps the same logic around all 9 IoEvents
 */
public abstract class WrappingFilter extends IoFilterAdapter {

    public WrappingFilter() {
    }

    protected abstract void filter(NextFilter nextFilter, IoEvent event) throws Exception;

    @Override
    public final void sessionCreated(NextFilter nextFilter, IoSession session) throws Exception {
        filter(nextFilter, new IoEvent(IoEventType.SESSION_CREATED, session, null));
    }

    @Override
    public final void sessionOpened(NextFilter nextFilter, IoSession session) throws Exception {
        filter(nextFilter, new IoEvent(IoEventType.SESSION_OPENED, session, null));
    }

    @Override
    public final void sessionClosed(NextFilter nextFilter, IoSession session) throws Exception {
        filter(nextFilter, new IoEvent(IoEventType.SESSION_CLOSED, session, null));
    }

    @Override
    public final void sessionIdle(NextFilter nextFilter, IoSession session, IdleStatus status) throws Exception {
        filter(nextFilter, new IoEvent(IoEventType.SESSION_IDLE, session, status));
    }

    @Override
    public final void exceptionCaught(NextFilter nextFilter, IoSession session, Throwable cause) throws Exception {
        filter(nextFilter, new IoEvent(IoEventType.EXCEPTION_CAUGHT, session, cause));
    }

    @Override
    public final void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        filter(nextFilter, new IoEvent(IoEventType.MESSAGE_RECEIVED, session, message));
    }

    @Override
    public final void messageSent(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        filter(nextFilter, new IoEvent(IoEventType.MESSAGE_SENT, session, writeRequest));
    }

    @Override
    public final void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        filter(nextFilter, new IoEvent(IoEventType.WRITE, session, writeRequest));
    }

    @Override
    public final void filterClose(NextFilter nextFilter, IoSession session) throws Exception {
        filter(nextFilter, new IoEvent(IoEventType.CLOSE, session, null));
    }
}
