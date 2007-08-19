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

    protected interface FilterAction {
        void execute();
    }

    abstract protected void wrapFilterAction(IoEventType eventType, IoSession session, FilterAction action);

    public void sessionCreated(final NextFilter nextFilter, final IoSession session) throws Exception {
        wrapFilterAction(IoEventType.SESSION_CREATED, session, new FilterAction() {
            public void execute() {
                nextFilter.sessionCreated(session);
            }
        });
    }

    public void sessionOpened(final NextFilter nextFilter, final IoSession session) throws Exception {
        wrapFilterAction(IoEventType.SESSION_OPENED, session, new FilterAction() {
            public void execute() {
                nextFilter.sessionOpened(session);
            }
        });
    }

    public void sessionClosed(final NextFilter nextFilter, final IoSession session) throws Exception {
        wrapFilterAction(IoEventType.SESSION_CLOSED, session, new FilterAction() {
            public void execute() {
                nextFilter.sessionClosed(session);
            }
        });
    }

    public void sessionIdle(final NextFilter nextFilter, final IoSession session, final IdleStatus status) throws Exception {
        wrapFilterAction(IoEventType.SESSION_IDLE, session, new FilterAction() {
            public void execute() {
                nextFilter.sessionIdle(session, status);
            }
        });
    }

    public void exceptionCaught(final NextFilter nextFilter, final IoSession session, final Throwable cause) throws Exception {
        wrapFilterAction(IoEventType.EXCEPTION_CAUGHT, session, new FilterAction() {
            public void execute() {
                nextFilter.exceptionCaught(session, cause);
            }
        });
    }

    public void messageReceived(final NextFilter nextFilter, final IoSession session, final Object message) throws Exception {
        wrapFilterAction(IoEventType.MESSAGE_RECEIVED, session, new FilterAction() {
            public void execute() {
                nextFilter.messageReceived(session, message);
            }
        });
    }

    public void messageSent(final NextFilter nextFilter, final IoSession session, final WriteRequest writeRequest) throws Exception {
        wrapFilterAction(IoEventType.MESSAGE_SENT, session, new FilterAction() {
            public void execute() {
                nextFilter.messageSent(session, writeRequest);
            }
        });
    }

    public void filterWrite(final NextFilter nextFilter, final IoSession session, final WriteRequest writeRequest) throws Exception {
        wrapFilterAction(IoEventType.WRITE, session, new FilterAction() {
            public void execute() {
                nextFilter.filterWrite(session, writeRequest);
            }
        });
    }

    public void filterClose(final NextFilter nextFilter, final IoSession session) throws Exception {
        wrapFilterAction(IoEventType.CLOSE, session, new FilterAction() {
            public void execute() {
                nextFilter.filterClose(session);
            }
        });
    }
}
