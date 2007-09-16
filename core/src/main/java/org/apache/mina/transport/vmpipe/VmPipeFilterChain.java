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
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.DefaultIoFilterChain;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoEvent;
import org.apache.mina.common.IoEventType;
import org.apache.mina.common.IoProcessor;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteRequest;

/**
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
class VmPipeFilterChain extends DefaultIoFilterChain {

    private final Queue<IoEvent> eventQueue = new ConcurrentLinkedQueue<IoEvent>();
    private final IoProcessor processor = new VmPipeIoProcessor();

    private volatile boolean flushEnabled;
    private volatile boolean sessionOpened;

    VmPipeFilterChain(AbstractIoSession session) {
        super(session);
    }
    
    IoProcessor getProcessor() {
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
                        int byteCount = 1;
                        if (data instanceof ByteBuffer) {
                            byteCount = ((ByteBuffer) data).remaining();
                        }

                        s.increaseReadBytes(byteCount);

                        super.fireMessageReceived(s, data);
                    }
                } finally {
                    s.getLock().unlock();
                }
            } else {
                s.receivedMessageQueue.add(data);
            }
        } else if (type == IoEventType.WRITE) {
            super.fireFilterWrite(session, (WriteRequest) data);
        } else if (type == IoEventType.MESSAGE_SENT) {
            super.fireMessageSent(session, (WriteRequest) data);
        } else if (type == IoEventType.EXCEPTION_CAUGHT) {
            super.fireExceptionCaught(session, (Throwable) data);
        } else if (type == IoEventType.SESSION_IDLE) {
            super.fireSessionIdle(session, (IdleStatus) data);
        } else if (type == IoEventType.SESSION_OPENED) {
            super.fireSessionOpened(session);
            sessionOpened = true;
        } else if (type == IoEventType.SESSION_CREATED) {
            super.fireSessionCreated(session);
        } else if (type == IoEventType.SESSION_CLOSED) {
            super.fireSessionClosed(session);
        } else if (type == IoEventType.CLOSE) {
            super.fireFilterClose(session);
        }
    }

    private static void flushPendingDataQueues(VmPipeSessionImpl s) {
        s.getProcessor().updateTrafficMask(s);
        s.getRemoteSession().getProcessor().updateTrafficMask(s);
    }

    @Override
    public void fireFilterClose(IoSession session) {
        pushEvent(new IoEvent(IoEventType.CLOSE, session, null));
    }

    @Override
    public void fireFilterWrite(IoSession session, WriteRequest writeRequest) {
        pushEvent(new IoEvent(IoEventType.WRITE, session, writeRequest));
    }

    @Override
    public void fireExceptionCaught(IoSession session, Throwable cause) {
        pushEvent(new IoEvent(IoEventType.EXCEPTION_CAUGHT, session, cause));
    }

    @Override
    public void fireMessageSent(IoSession session, WriteRequest request) {
        pushEvent(new IoEvent(IoEventType.MESSAGE_SENT, session, request));
    }

    @Override
    public void fireSessionClosed(IoSession session) {
        pushEvent(new IoEvent(IoEventType.SESSION_CLOSED, session, null));
    }

    @Override
    public void fireSessionCreated(IoSession session) {
        pushEvent(new IoEvent(IoEventType.SESSION_CREATED, session, null));
    }

    @Override
    public void fireSessionIdle(IoSession session, IdleStatus status) {
        pushEvent(new IoEvent(IoEventType.SESSION_IDLE, session, status));
    }

    @Override
    public void fireSessionOpened(IoSession session) {
        pushEvent(new IoEvent(IoEventType.SESSION_OPENED, session, null));
    }

    @Override
    public void fireMessageReceived(IoSession session, Object message) {
        pushEvent(new IoEvent(IoEventType.MESSAGE_RECEIVED, session, message));
    }
    
    private class VmPipeIoProcessor implements IoProcessor {
        public void flush(IoSession session, WriteRequest writeRequest) {
            VmPipeSessionImpl s = (VmPipeSessionImpl) session;
            Queue<WriteRequest> queue = s.getWriteRequestQueue();
            if (queue.isEmpty()) {
                return;
            }
            if (s.isConnected()) {
                if (s.getLock().tryLock()) {
                    try {
                        WriteRequest req;
                        while ((req = queue.poll()) != null) {
                            int byteCount = 0;
                            Object message = req.getMessage();
                            Object messageCopy = message;
                            if (message instanceof ByteBuffer) {
                                ByteBuffer rb = (ByteBuffer) message;
                                rb.mark();
                                byteCount = rb.remaining();
                                ByteBuffer wb = ByteBuffer.allocate(rb.remaining());
                                wb.put(rb);
                                wb.flip();
                                rb.reset();
                                messageCopy = wb;
                            }
    
                            s.increaseWrittenBytes(byteCount);
                            s.increaseWrittenMessages();
    
                            s.getRemoteSession().getFilterChain().fireMessageReceived(
                                    s.getRemoteSession(), messageCopy);
                            s.getFilterChain().fireMessageSent(s, req);
                        }
                    } finally {
                        s.getLock().unlock();
                    }

                    flushPendingDataQueues(s);
                }
            } else {
                WriteRequest req;
                while ((req = queue.poll()) != null) {
                    req.getFuture().setWritten(false);
                }
            }
        }

        public void remove(IoSession session) {
            VmPipeSessionImpl s = (VmPipeSessionImpl) session;
            try {
                s.getLock().lock();
                if (!session.getCloseFuture().isClosed()) {
                    s.getServiceListeners().fireSessionDestroyed(s);
                    s.getRemoteSession().close();
                }
            } finally {
                s.getLock().unlock();
            }
        }

        public void add(IoSession session) {
        }

        public void updateTrafficMask(IoSession session) {
            VmPipeSessionImpl s = (VmPipeSessionImpl) session;
            if (s.getTrafficMask().isReadable()) {
                List<Object> data = new ArrayList<Object>();
                s.receivedMessageQueue.drainTo(data);
                for (Object aData : data) {
                    // TODO Optimize inefficient data transfer.
                    // Data will be returned to pendingDataQueue
                    // if getTraffic().isReadable() is false.
                    VmPipeFilterChain.this.fireMessageReceived(s, aData);
                }
            }
            
            if (s.getTrafficMask().isWritable()) {
                flush(s, null); // The second parameter is unused.
            }
        }
    }
}
