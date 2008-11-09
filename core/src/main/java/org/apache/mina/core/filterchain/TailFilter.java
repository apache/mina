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
                        session.getFilterChainIn().clear();
                        session.getFilterChainOut().clear();
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
    
    public void filterWrite(int index, IoSession session,
            WriteRequest writeRequest) throws Exception {

        AbstractIoSession s = (AbstractIoSession) session;

        // Maintain counters.
        if (writeRequest.getMessage() instanceof IoBuffer) {
            IoBuffer buffer = (IoBuffer) writeRequest.getMessage();
            // I/O processor implementation will call buffer.reset()
            // it after the write operation is finished, because
            // the buffer will be specified with messageSent event.
            buffer.mark();
            int remaining = buffer.remaining();
            if (remaining == 0) {
                // Zero-sized buffer means the internal message
                // delimiter.
                s.increaseScheduledWriteMessages();
            } else {
                s.increaseScheduledWriteBytes(remaining);
            }
        } else {
            s.increaseScheduledWriteMessages();
        }

        s.getWriteRequestQueue().offer(s, writeRequest);
        if (s.getTrafficMask().isWritable()) {
            s.getProcessor().flush(s);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void filterClose(int index, IoSession session)
            throws Exception {
        ((AbstractIoSession) session).getProcessor().remove(session);
    }
}
