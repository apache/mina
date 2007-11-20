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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.apache.mina.common.AbstractIoProcessor;
import org.apache.mina.common.FileRegion;
import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.RuntimeIoException;
import org.apache.mina.util.CircularQueue;
import org.apache.tomcat.jni.Poll;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.Socket;
import org.apache.tomcat.jni.Status;

/**
 * The class in charge of processing socket level IO events for the {@link AprConnector}
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */

public class AprIoProcessor extends AbstractIoProcessor<AprSession> {
    private static final int INITIAL_CAPACITY = 32;
    
    private final Map<Long, AprSession> allSessions =
        new HashMap<Long, AprSession>(INITIAL_CAPACITY);
    
    private final Object wakeupLock = new Object();
    private long wakeupSocket;
    private volatile boolean toBeWakenUp;

    private final long bufferPool; // memory pool
    private final long pollset; // socket poller
    private long[] polledSockets = new long[INITIAL_CAPACITY << 1];
    private final List<AprSession> polledSessions =
        new CircularQueue<AprSession>(INITIAL_CAPACITY);

    public AprIoProcessor(Executor executor) {
        super(executor);
        
        try {
            wakeupSocket = Socket.create(
                    Socket.APR_INET, Socket.SOCK_DGRAM, Socket.APR_PROTO_UDP, AprLibrary
                    .getInstance().getRootPool());
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeIoException("Failed to create a wakeup socket.", e);
        }

        // initialize a memory pool for APR functions
        bufferPool = Pool.create(AprLibrary.getInstance().getRootPool());
        
        boolean success = false;
        try {
            // TODO : optimize/parameterize those values
            pollset = Poll
                    .create(
                            INITIAL_CAPACITY,
                            AprLibrary.getInstance().getRootPool(),
                            Poll.APR_POLLSET_THREADSAFE,
                            10000000);
            if (pollset < 0) {
                if (Status.APR_STATUS_IS_ENOTIMPL(- (int) pollset)) {
                    throw new RuntimeIoException(
                            "Thread-safe pollset is not supported in this platform.");
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

    @Override
    protected void doDispose() {
        Poll.destroy(pollset);
        Pool.destroy(bufferPool);
        Socket.close(wakeupSocket);
    }

    @Override
    protected boolean select(int timeout) throws Exception {
        int rv = Poll.poll(pollset, 1000 * timeout, polledSockets, false);
        if (rv > 0) {
            rv <<= 1;
            if (!polledSessions.isEmpty()) {
                polledSessions.clear();
            }
            for (int i = 0; i < rv; i ++) {
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
            
            return !polledSessions.isEmpty();
        } else if (rv < 0 && rv != -120001) {
            throwException(rv);
        }

        return false;
    }

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

    @Override
    protected Iterator<AprSession> allSessions() throws Exception {
        return allSessions.values().iterator();
    }

    @Override
    protected Iterator<AprSession> selectedSessions() throws Exception {
        return polledSessions.iterator();
    }

    @Override
    protected void init(AprSession session) throws Exception {
        long s = session.getAprSocket();
        Socket.optSet(s, Socket.APR_SO_NONBLOCK, 1);
        Socket.timeoutSet(s, 0);
        
        int rv = Poll.add(pollset, s, Poll.APR_POLLIN);
        if (rv != Status.APR_SUCCESS) {
            throwException(rv);
        }
        
        session.setInterestedInRead(true);
        if (allSessions.size() > polledSockets.length >>> 2) {
            this.polledSockets = new long[polledSockets.length << 1];
        }
        allSessions.put(s, session);
    }

    @Override
    protected void destroy(AprSession session) throws Exception {
        allSessions.remove(session.getAprSocket());
        if (polledSockets.length >= (INITIAL_CAPACITY << 2) &&
            allSessions.size() <= polledSockets.length >>> 3) {
            this.polledSockets = new long[polledSockets.length >>> 1];
        }
    
        int ret = Poll.remove(pollset, session.getAprSocket());
        if (ret != Status.APR_SUCCESS) {
            try {
                throwException(ret);
            } finally {
                System.out.println("CLOSE");
                ret = Socket.close(session.getAprSocket());
                if (ret != Status.APR_SUCCESS) {
                    throwException(ret);
                }
            }
        }
    }

    @Override
    protected SessionState state(AprSession session) {
        long socket = session.getAprSocket();
        if (socket > 0) {
            return SessionState.OPEN;
        } else if (allSessions.get(socket) != null) {
            return SessionState.PREPARING; // will occur ?
        } else {
            return SessionState.CLOSED;
        }
    }

    @Override
    protected boolean isReadable(AprSession session) throws Exception {
        return session.isReadable();
    }

    @Override
    protected boolean isWritable(AprSession session) throws Exception {
        return session.isWritable();
    }

    @Override
    protected boolean isInterestedInRead(AprSession session) throws Exception {
        return session.isInterestedInRead();
    }

    @Override
    protected boolean isInterestedInWrite(AprSession session) throws Exception {
        return session.isInterestedInWrite();
    }

    @Override
    protected void setInterestedInRead(AprSession session, boolean value)
            throws Exception {
        int rv = Poll.remove(pollset, session.getAprSocket());
        if (rv != Status.APR_SUCCESS) {
            throwException(rv);
        }

        int flags = (value ? Poll.APR_POLLIN : 0)
                | (session.isInterestedInWrite() ? Poll.APR_POLLOUT : 0);

        rv = Poll.add(pollset, session.getAprSocket(), flags);
        if (rv == Status.APR_SUCCESS) {
            session.setInterestedInRead(value);
        } else {
            throwException(rv);
        }
    }

    @Override
    protected void setInterestedInWrite(AprSession session, boolean value)
            throws Exception {
        int rv = Poll.remove(pollset, session.getAprSocket());
        if (rv != Status.APR_SUCCESS) {
            throwException(rv);
        }

        int flags = (session.isInterestedInRead() ? Poll.APR_POLLIN : 0)
                | (value ? Poll.APR_POLLOUT : 0);

        rv = Poll.add(pollset, session.getAprSocket(), flags);
        if (rv == Status.APR_SUCCESS) {
            session.setInterestedInWrite(value);
        } else {
            throwException(rv);
        }
    }

    @Override
    protected int read(AprSession session, IoBuffer buffer) throws Exception {
        int bytes;
        // Using Socket.recv() directly causes memory leak. :-(
        ByteBuffer b = Pool.alloc(bufferPool, buffer.remaining());
        try {
            bytes = Socket.recvb(
                    session.getAprSocket(), b, 0, b.remaining());
            b.flip();
            buffer.put(b);
            if (bytes > 0) {
                buffer.skip(bytes);
            }
            
            if (bytes < 0) {
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

    @Override
    protected int write(AprSession session, IoBuffer buf) throws Exception {
        int writtenBytes;
        if (buf.isDirect()) {
            writtenBytes = Socket.sendb(session.getAprSocket(), buf.buf(), buf
                    .position(), buf.remaining());
        } else {
            writtenBytes = Socket.send(session.getAprSocket(), buf.array(), buf
                    .position(), buf.remaining());
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

    @Override
    protected long transferFile(AprSession session, FileRegion region)
            throws Exception {
        throw new UnsupportedOperationException();
    }

    private void throwException(int code) throws IOException {
        throw new IOException(
                org.apache.tomcat.jni.Error.strerror(-code) +
                " (code: " + code + ")");
    }
}