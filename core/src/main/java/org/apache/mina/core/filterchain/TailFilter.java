package org.apache.mina.core.filterchain;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

public class TailFilter extends IoFilterAdapter {
    @Override
    public void sessionCreated(int index, IoSession session)
            throws Exception {
        try {
            session.getHandler().sessionCreated(session);
        } finally {
            // Notify the related future.
            ConnectFuture future = (ConnectFuture) session
                    .removeAttribute(AbstractIoSession.SESSION_CREATED_FUTURE);
            if (future != null) {
                future.setSession(session);
            }
        }
    }

    @Override
    public void sessionOpened(int index, IoSession session)
            throws Exception {
        session.getHandler().sessionOpened(session);
    }

    @Override
    public void sessionClosed(int index, IoSession session)
            throws Exception {
        AbstractIoSession s = (AbstractIoSession) session;
        try {
            s.getHandler().sessionClosed(session);
        } finally {
            try {
                s.getWriteRequestQueue().dispose(session);
            } finally {
                try {
                    s.getAttributeMap().dispose(session);
                } finally {
                    try {
                        // Remove all filters.
                        session.getFilterChain().clear();
                    } finally {
                        if (s.getConfig().isUseReadOperation()) {
                            s.offerClosedReadFuture();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void sessionIdle(int index, IoSession session,
            IdleStatus status) throws Exception {
        session.getHandler().sessionIdle(session, status);
    }

    @Override
    public void exceptionCaught(int index, IoSession session,
            Throwable cause) throws Exception {
        AbstractIoSession s = (AbstractIoSession) session;
        try {
            s.getHandler().exceptionCaught(s, cause);
        } finally {
            if (s.getConfig().isUseReadOperation()) {
                s.offerFailedReadFuture(cause);
            }
        }
    }

    @Override
    public void messageReceived(int index, IoSession session,
            Object message) throws Exception {
        AbstractIoSession s = (AbstractIoSession) session;
        if (!(message instanceof IoBuffer)) {
            s.increaseReadMessages(System.currentTimeMillis());
        } else if (!((IoBuffer) message).hasRemaining()) {
            s.increaseReadMessages(System.currentTimeMillis());
        }

        try {
            session.getHandler().messageReceived(s, message);
        } finally {
            if (s.getConfig().isUseReadOperation()) {
                s.offerReadFuture(message);
            }
        }
    }

    @Override
    public void messageSent(int index, IoSession session,
            WriteRequest writeRequest) throws Exception {
        session.getHandler()
                .messageSent(session, writeRequest.getMessage());
    }

    @Override
    public void filterWrite(int index, IoSession session,
            WriteRequest writeRequest) throws Exception {
        session.getFilter(index).filterWrite(0, session, writeRequest);
    }

    @Override
    public void filterClose(int index, IoSession session)
            throws Exception {
    	session.getFilter(index).filterClose(0, session);
    }
}
