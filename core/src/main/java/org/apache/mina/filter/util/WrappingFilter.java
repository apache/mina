package org.apache.mina.filter.util;

import org.apache.mina.common.IdleStatus;
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

    protected abstract void wrap(IoEventType eventType, IoSession session, Runnable action);

    @Override
    final public void sessionCreated(final NextFilter nextFilter, final IoSession session) throws Exception {
        wrap(IoEventType.SESSION_CREATED, session, new Runnable() {
            public void run() {
                nextFilter.sessionCreated(session);
            }
        });
    }

    @Override
    final public void sessionOpened(final NextFilter nextFilter, final IoSession session) throws Exception {
        wrap(IoEventType.SESSION_OPENED, session, new Runnable() {
            public void run() {
                nextFilter.sessionOpened(session);
            }
        });
    }

    @Override
    final public void sessionClosed(final NextFilter nextFilter, final IoSession session) throws Exception {
        wrap(IoEventType.SESSION_CLOSED, session, new Runnable() {
            public void run() {
                nextFilter.sessionClosed(session);
            }
        });
    }

    @Override
    final public void sessionIdle(final NextFilter nextFilter, final IoSession session, final IdleStatus status) throws Exception {
        wrap(IoEventType.SESSION_IDLE, session, new Runnable() {
            public void run() {
                nextFilter.sessionIdle(session, status);
            }
        });
    }

    @Override
    final public void exceptionCaught(final NextFilter nextFilter, final IoSession session, final Throwable cause) throws Exception {
        wrap(IoEventType.EXCEPTION_CAUGHT, session, new Runnable() {
            public void run() {
                nextFilter.exceptionCaught(session, cause);
            }
        });
    }

    @Override
    final public void messageReceived(final NextFilter nextFilter, final IoSession session, final Object message) throws Exception {
        wrap(IoEventType.MESSAGE_RECEIVED, session, new Runnable() {
            public void run() {
                nextFilter.messageReceived(session, message);
            }
        });
    }

    @Override
    final public void messageSent(final NextFilter nextFilter, final IoSession session, final WriteRequest writeRequest) throws Exception {
        wrap(IoEventType.MESSAGE_SENT, session, new Runnable() {
            public void run() {
                nextFilter.messageSent(session, writeRequest);
            }
        });
    }

    @Override
    final public void filterWrite(final NextFilter nextFilter, final IoSession session, final WriteRequest writeRequest) throws Exception {
        wrap(IoEventType.WRITE, session, new Runnable() {
            public void run() {
                nextFilter.filterWrite(session, writeRequest);
            }
        });
    }

    @Override
    final public void filterClose(final NextFilter nextFilter, final IoSession session) throws Exception {
        wrap(IoEventType.CLOSE, session, new Runnable() {
            public void run() {
                nextFilter.filterClose(session);
            }
        });
    }
}
