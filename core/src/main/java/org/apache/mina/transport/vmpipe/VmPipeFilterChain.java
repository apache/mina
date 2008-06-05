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
package org.apache.mina.transport.vmpipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.mina.common.AbstractIoSession;
import org.apache.mina.common.DefaultIoFilterChain;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoEvent;
import org.apache.mina.common.IoEventType;
import org.apache.mina.common.IoProcessor;
import org.apache.mina.common.WriteRequest;
import org.apache.mina.common.WriteRequestQueue;
import org.apache.mina.common.WriteToClosedSessionException;

/**
 * TODO Add documentation
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
class VmPipeFilterChain extends DefaultIoFilterChain {

    private final Queue<IoEvent> eventQueue = new ConcurrentLinkedQueue<IoEvent>();
    private final IoProcessor<VmPipeSessionImpl> processor = new VmPipeIoProcessor();

    private volatile boolean flushEnabled;
    private volatile boolean sessionOpened;

    VmPipeFilterChain(AbstractIoSession session) {
        super(session);
    }

    IoProcessor<VmPipeSessionImpl> getProcessor() {
        return processor;
    }

    public void start() {
        flushEnabled = true;
        flushEvents();
        flushPendingDataQueues((VmPipeSessionImpl) getSession());
    }

    private void pushEvent(IoEvent e) {
        pushEvent(e, flushEnabled);
    }

    private void pushEvent(IoEvent e, boolean flushNow) {
        eventQueue.add(e);
        if (flushNow) {
            flushEvents();
        }
    }

    private void flushEvents() {
        IoEvent e;
        while ((e = eventQueue.poll()) != null) {
            fireEvent(e);
        }
    }

    private void fireEvent(IoEvent e) {
        VmPipeSessionImpl session = (VmPipeSessionImpl) getSession();
        IoEventType type = e.getType();
        Object data = e.getParameter();

        if (type == IoEventType.MESSAGE_RECEIVED) {
            if (sessionOpened && session.getTrafficMask().isReadable() && session.getLock().tryLock()) {
                try {
                    if (!session.getTrafficMask().isReadable()) {
                        session.receivedMessageQueue.add(data);
                    } else {
                        super.fireMessageReceived(data);
                    }
                } finally {
                    session.getLock().unlock();
                }
            } else {
                session.receivedMessageQueue.add(data);
            }
        } else if (type == IoEventType.WRITE) {
            super.fireFilterWrite((WriteRequest) data);
        } else if (type == IoEventType.MESSAGE_SENT) {
            super.fireMessageSent((WriteRequest) data);
        } else if (type == IoEventType.EXCEPTION_CAUGHT) {
            super.fireExceptionCaught((Throwable) data);
        } else if (type == IoEventType.SESSION_IDLE) {
            super.fireSessionIdle((IdleStatus) data);
        } else if (type == IoEventType.SESSION_OPENED) {
            super.fireSessionOpened();
            sessionOpened = true;
        } else if (type == IoEventType.SESSION_CREATED) {
            session.getLock().lock();
            try {
                super.fireSessionCreated();
            } finally {
                session.getLock().unlock();
            }
        } else if (type == IoEventType.SESSION_CLOSED) {
            flushPendingDataQueues(session);
            super.fireSessionClosed();
        } else if (type == IoEventType.CLOSE) {
            super.fireFilterClose();
        }
    }

    private static void flushPendingDataQueues(VmPipeSessionImpl s) {
        s.getProcessor().updateTrafficMask(s);
        s.getRemoteSession().getProcessor().updateTrafficMask(s);
    }

    @Override
    public void fireFilterClose() {
        pushEvent(new IoEvent(IoEventType.CLOSE, getSession(), null));
    }

    @Override
    public void fireFilterWrite(WriteRequest writeRequest) {
        pushEvent(new IoEvent(IoEventType.WRITE, getSession(), writeRequest));
    }

    @Override
    public void fireExceptionCaught(Throwable cause) {
        pushEvent(new IoEvent(IoEventType.EXCEPTION_CAUGHT, getSession(), cause));
    }

    @Override
    public void fireMessageSent(WriteRequest request) {
        pushEvent(new IoEvent(IoEventType.MESSAGE_SENT, getSession(), request));
    }

    @Override
    public void fireSessionClosed() {
        pushEvent(new IoEvent(IoEventType.SESSION_CLOSED, getSession(), null));
    }

    @Override
    public void fireSessionCreated() {
        pushEvent(new IoEvent(IoEventType.SESSION_CREATED, getSession(), null));
    }

    @Override
    public void fireSessionIdle(IdleStatus status) {
        pushEvent(new IoEvent(IoEventType.SESSION_IDLE, getSession(), status));
    }

    @Override
    public void fireSessionOpened() {
        pushEvent(new IoEvent(IoEventType.SESSION_OPENED, getSession(), null));
    }

    @Override
    public void fireMessageReceived(Object message) {
        pushEvent(new IoEvent(IoEventType.MESSAGE_RECEIVED, getSession(), message));
    }

    private class VmPipeIoProcessor implements IoProcessor<VmPipeSessionImpl> {
        public void flush(VmPipeSessionImpl session) {
            WriteRequestQueue queue = session.getWriteRequestQueue0();
            if (!session.isClosing()) {
                session.getLock().lock();
                try {
                    if (queue.isEmpty(session)) {
                        return;
                    }
                    WriteRequest req;
                    long currentTime = System.currentTimeMillis();
                    while ((req = queue.poll(session)) != null) {
                        Object m = req.getMessage();
                        pushEvent(new IoEvent(IoEventType.MESSAGE_SENT, session, req), false);
                        session.getRemoteSession().getFilterChain().fireMessageReceived(
                                getMessageCopy(m));
                        if (m instanceof IoBuffer) {
                            session.increaseWrittenBytes0(
                                    ((IoBuffer) m).remaining(), currentTime);
                        }
                    }
                } finally {
                    if (flushEnabled) {
                        flushEvents();
                    }
                    session.getLock().unlock();
                }

                flushPendingDataQueues(session);
            } else {
                List<WriteRequest> failedRequests = new ArrayList<WriteRequest>();
                WriteRequest req;
                while ((req = queue.poll(session)) != null) {
                    failedRequests.add(req);
                }

                if (!failedRequests.isEmpty()) {
                    WriteToClosedSessionException cause = new WriteToClosedSessionException(failedRequests);
                    for (WriteRequest r: failedRequests) {
                        r.getFuture().setException(cause);
                    }
                    session.getFilterChain().fireExceptionCaught(cause);
                }
            }
        }

        private Object getMessageCopy(Object message) {
            Object messageCopy = message;
            if (message instanceof IoBuffer) {
                IoBuffer rb = (IoBuffer) message;
                rb.mark();
                IoBuffer wb = IoBuffer.allocate(rb.remaining());
                wb.put(rb);
                wb.flip();
                rb.reset();
                messageCopy = wb;
            }
            return messageCopy;
        }

        public void remove(VmPipeSessionImpl session) {
            try {
                session.getLock().lock();
                if (!session.getCloseFuture().isClosed()) {
                    session.getServiceListeners().fireSessionDestroyed(session);
                    session.getRemoteSession().close();
                }
            } finally {
                session.getLock().unlock();
            }
        }

        public void add(VmPipeSessionImpl session) {
            // Unused
        }

        public void updateTrafficMask(VmPipeSessionImpl session) {
            if (session.getTrafficMask().isReadable()) {
                List<Object> data = new ArrayList<Object>();
                session.receivedMessageQueue.drainTo(data);
                for (Object aData : data) {
                    VmPipeFilterChain.this.fireMessageReceived(aData);
                }
            }

            if (session.getTrafficMask().isWritable()) {
                flush(session);
            }
        }

        public void dispose() {
            // Nothing to dispose
        }

        public boolean isDisposed() {
            return false;
        }

        public boolean isDisposing() {
            return false;
        }
    }
}
