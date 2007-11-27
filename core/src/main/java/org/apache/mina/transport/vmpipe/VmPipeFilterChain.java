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
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteRequest;
import org.apache.mina.common.WriteRequestQueue;
import org.apache.mina.common.WriteToClosedSessionException;

/**
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
        eventQueue.add(e);
        if (flushEnabled) {
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
        IoSession session = getSession();
        IoEventType type = e.getType();
        Object data = e.getParameter();

        if (type == IoEventType.MESSAGE_RECEIVED) {
            VmPipeSessionImpl s = (VmPipeSessionImpl) session;
            if (sessionOpened && s.getTrafficMask().isReadable() && s.getLock().tryLock()) {
                try {
                    if (!s.getTrafficMask().isReadable()) {
                        s.receivedMessageQueue.add(data);
                    } else {
                        super.fireMessageReceived(data);
                    }
                } finally {
                    s.getLock().unlock();
                }
            } else {
                s.receivedMessageQueue.add(data);
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
            super.fireSessionCreated();
        } else if (type == IoEventType.SESSION_CLOSED) {
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
            if (queue.isEmpty(session)) {
                return;
            }
            if (session.isConnected()) {
                if (session.getLock().tryLock()) {
                    try {
                        WriteRequest req;
                        while ((req = queue.poll(session)) != null) {
                            Object message = req.getMessage();
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

                            session.getRemoteSession().getFilterChain().fireMessageReceived(
                                    messageCopy);
                            session.getFilterChain().fireMessageSent(req);
                        }
                    } finally {
                        session.getLock().unlock();
                    }

                    flushPendingDataQueues(session);
                }
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
                flush(session); // The second parameter is unused.
            }
        }

        public void dispose() {
        }

        public boolean isDisposed() {
            return false;
        }

        public boolean isDisposing() {
            return false;
        }
    }
}
