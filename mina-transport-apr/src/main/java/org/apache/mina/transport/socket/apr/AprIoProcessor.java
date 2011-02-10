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
package org.apache.mina.transport.socket.apr;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.FileRegion;
import org.apache.mina.core.polling.AbstractPollingIoProcessor;
import org.apache.mina.core.session.SessionState;
import org.apache.tomcat.jni.File;
import org.apache.tomcat.jni.Poll;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.Socket;
import org.apache.tomcat.jni.Status;

/**
 * The class in charge of processing socket level IO events for the
 * {@link AprSocketConnector}
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public final class AprIoProcessor extends AbstractPollingIoProcessor<AprSession> {
    private static final int POLLSET_SIZE = 1024;

    private final Map<Long, AprSession> allSessions = new HashMap<Long, AprSession>(POLLSET_SIZE);

    private final Object wakeupLock = new Object();
    private final long wakeupSocket;
    private volatile boolean toBeWakenUp;

    private final long pool;
    private final long bufferPool; // memory pool
    private final long pollset; // socket poller
    private final long[] polledSockets = new long[POLLSET_SIZE << 1];
    private final Queue<AprSession> polledSessions = new ConcurrentLinkedQueue<AprSession>();

    /**
     * Create a new instance of {@link AprIoProcessor} with a given Exector for
     * handling I/Os events.
     *
     * @param executor
     *            the {@link Executor} for handling I/O events
     */
    public AprIoProcessor(Executor executor) {
        super(executor);

        // initialize a memory pool for APR functions
        pool = Pool.create(AprLibrary.getInstance().getRootPool());
        bufferPool = Pool.create(AprLibrary.getInstance().getRootPool());

        try {
            wakeupSocket = Socket.create(Socket.APR_INET, Socket.SOCK_DGRAM, Socket.APR_PROTO_UDP, pool);
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeIoException("Failed to create a wakeup socket.", e);
        }

        boolean success = false;
        long newPollset;
        try {
            newPollset = Poll.create(POLLSET_SIZE, pool, Poll.APR_POLLSET_THREADSAFE, Long.MAX_VALUE);

            if (newPollset == 0) {
                newPollset = Poll.create(62, pool, Poll.APR_POLLSET_THREADSAFE, Long.MAX_VALUE);
            }

            pollset = newPollset;
            if (pollset < 0) {
                if (Status.APR_STATUS_IS_ENOTIMPL(-(int) pollset)) {
                    throw new RuntimeIoException("Thread-safe pollset is not supported in this platform.");
                }
            }
            success = true;
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeIoException("Failed to create a pollset.", e);
        } finally {
            if (!success) {
                dispose();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doDispose() {
        Poll.destroy(pollset);
        Socket.close(wakeupSocket);
        Pool.destroy(bufferPool);
        Pool.destroy(pool);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int select() throws Exception {
        return select(Integer.MAX_VALUE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int select(long timeout) throws Exception {
        int rv = Poll.poll(pollset, 1000 * timeout, polledSockets, false);
        if (rv <= 0) {
            if (rv != -120001) {
                throwException(rv);
            }

            rv = Poll.maintain(pollset, polledSockets, true);
            if (rv > 0) {
                for (int i = 0; i < rv; i++) {
                    long socket = polledSockets[i];
                    AprSession session = allSessions.get(socket);
                    if (session == null) {
                        continue;
                    }

                    int flag = (session.isInterestedInRead() ? Poll.APR_POLLIN : 0)
                            | (session.isInterestedInWrite() ? Poll.APR_POLLOUT : 0);

                    Poll.add(pollset, socket, flag);
                }
            } else if (rv < 0) {
                throwException(rv);
            }

            return 0;
        } else {
            rv <<= 1;
            if (!polledSessions.isEmpty()) {
                polledSessions.clear();
            }
            for (int i = 0; i < rv; i++) {
                long flag = polledSockets[i];
                long socket = polledSockets[++i];
                if (socket == wakeupSocket) {
                    synchronized (wakeupLock) {
                        Poll.remove(pollset, wakeupSocket);
                        toBeWakenUp = false;
                    }
                    continue;
                }
                AprSession session = allSessions.get(socket);
                if (session == null) {
                    continue;
                }

                session.setReadable((flag & Poll.APR_POLLIN) != 0);
                session.setWritable((flag & Poll.APR_POLLOUT) != 0);

                polledSessions.add(session);
            }

            return polledSessions.size();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isSelectorEmpty() {
        return allSessions.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void wakeup() {
        if (toBeWakenUp) {
            return;
        }

        // Add a dummy socket to the pollset.
        synchronized (wakeupLock) {
            toBeWakenUp = true;
            Poll.add(pollset, wakeupSocket, Poll.APR_POLLOUT);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Iterator<AprSession> allSessions() {
        return allSessions.values().iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Iterator<AprSession> selectedSessions() {
        return polledSessions.iterator();
    }

    @Override
    protected void init(AprSession session) throws Exception {
        long s = session.getDescriptor();
        Socket.optSet(s, Socket.APR_SO_NONBLOCK, 1);
        Socket.timeoutSet(s, 0);

        int rv = Poll.add(pollset, s, Poll.APR_POLLIN);
        if (rv != Status.APR_SUCCESS) {
            throwException(rv);
        }

        session.setInterestedInRead(true);
        allSessions.put(s, session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void destroy(AprSession session) throws Exception {
        if (allSessions.remove(session.getDescriptor()) == null) {
            // Already destroyed.
            return;
        }

        int ret = Poll.remove(pollset, session.getDescriptor());
        try {
            if (ret != Status.APR_SUCCESS) {
                throwException(ret);
            }
        } finally {
            ret = Socket.close(session.getDescriptor());

            // destroying the session because it won't be reused
            // after this point
            Socket.destroy(session.getDescriptor());
            session.setDescriptor(0);

            if (ret != Status.APR_SUCCESS) {
                throwException(ret);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SessionState getState(AprSession session) {
        long socket = session.getDescriptor();

        if (socket != 0) {
            return SessionState.OPENED;
        } else if (allSessions.get(socket) != null) {
            return SessionState.OPENING; // will occur ?
        } else {
            return SessionState.CLOSING;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isReadable(AprSession session) {
        return session.isReadable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isWritable(AprSession session) {
        return session.isWritable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isInterestedInRead(AprSession session) {
        return session.isInterestedInRead();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isInterestedInWrite(AprSession session) {
        return session.isInterestedInWrite();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInterestedInRead(AprSession session, boolean isInterested) throws Exception {
        if (session.isInterestedInRead() == isInterested) {
            return;
        }

        int rv = Poll.remove(pollset, session.getDescriptor());

        if (rv != Status.APR_SUCCESS) {
            throwException(rv);
        }

        int flags = (isInterested ? Poll.APR_POLLIN : 0) | (session.isInterestedInWrite() ? Poll.APR_POLLOUT : 0);

        rv = Poll.add(pollset, session.getDescriptor(), flags);

        if (rv == Status.APR_SUCCESS) {
            session.setInterestedInRead(isInterested);
        } else {
            throwException(rv);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInterestedInWrite(AprSession session, boolean isInterested) throws Exception {
        if (session.isInterestedInWrite() == isInterested) {
            return;
        }

        int rv = Poll.remove(pollset, session.getDescriptor());

        if (rv != Status.APR_SUCCESS) {
            throwException(rv);
        }

        int flags = (session.isInterestedInRead() ? Poll.APR_POLLIN : 0) | (isInterested ? Poll.APR_POLLOUT : 0);

        rv = Poll.add(pollset, session.getDescriptor(), flags);

        if (rv == Status.APR_SUCCESS) {
            session.setInterestedInWrite(isInterested);
        } else {
            throwException(rv);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int read(AprSession session, IoBuffer buffer) throws Exception {
        int bytes;
        int capacity = buffer.remaining();
        // Using Socket.recv() directly causes memory leak. :-(
        ByteBuffer b = Pool.alloc(bufferPool, capacity);

        try {
            bytes = Socket.recvb(session.getDescriptor(), b, 0, capacity);

            if (bytes > 0) {
                b.position(0);
                b.limit(bytes);
                buffer.put(b);
            } else if (bytes < 0) {
                if (Status.APR_STATUS_IS_EOF(-bytes)) {
                    bytes = -1;
                } else if (Status.APR_STATUS_IS_EAGAIN(-bytes)) {
                    bytes = 0;
                } else {
                    throwException(bytes);
                }
            }
        } finally {
            Pool.clear(bufferPool);
        }

        return bytes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int write(AprSession session, IoBuffer buf, int length) throws Exception {
        int writtenBytes;
        if (buf.isDirect()) {
            writtenBytes = Socket.sendb(session.getDescriptor(), buf.buf(), buf.position(), length);
        } else {
            writtenBytes = Socket.send(session.getDescriptor(), buf.array(), buf.position(), length);
            if (writtenBytes > 0) {
                buf.skip(writtenBytes);
            }
        }

        if (writtenBytes < 0) {
            if (Status.APR_STATUS_IS_EAGAIN(-writtenBytes)) {
                writtenBytes = 0;
            } else if (Status.APR_STATUS_IS_EOF(-writtenBytes)) {
                writtenBytes = 0;
            } else {
                throwException(writtenBytes);
            }
        }
        return writtenBytes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int transferFile(AprSession session, FileRegion region, int length) throws Exception {
        if (region.getFilename() == null) {
            throw new UnsupportedOperationException();
        }

        long fd = File.open(region.getFilename(),
                            File.APR_FOPEN_READ
                                | File.APR_FOPEN_SENDFILE_ENABLED
                                | File.APR_FOPEN_BINARY,
                            0,
                            Socket.pool(session.getDescriptor()));
        long numWritten = Socket.sendfilen(session.getDescriptor(), fd, region.getPosition(), length, 0);
        File.close(fd);

        if (numWritten < 0) {
            if (numWritten == -Status.EAGAIN) {
                return 0;
            }
            throw new IOException(org.apache.tomcat.jni.Error.strerror((int) -numWritten) + " (code: " + numWritten + ")");
        }
        return (int) numWritten;
    }

    private void throwException(int code) throws IOException {
        throw new IOException(org.apache.tomcat.jni.Error.strerror(-code) + " (code: " + code + ")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerNewSelector() {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isBrokenConnection() throws IOException {
        // Here, we assume that this is the case.
        return true;
    }
}