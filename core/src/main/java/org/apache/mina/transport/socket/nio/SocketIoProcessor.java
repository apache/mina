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
package org.apache.mina.transport.socket.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.mina.common.AbstractIoProcessor;
import org.apache.mina.common.AbstractIoSession;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.FileRegion;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;

/**
 * Performs all I/O operations for sockets which is connected or bound. This class is used by MINA internally.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$,
 */
class SocketIoProcessor extends AbstractIoProcessor {

    private final Selector selector;

    SocketIoProcessor(String threadName, Executor executor) {
        super(threadName, executor);

        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeIOException("Failed to open a selector.", e);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        try {
            selector.close();
        } catch (IOException e) {
            ExceptionMonitor.getInstance().exceptionCaught(e);
        }
    }

    protected int select(int timeout) throws Exception {
        return selector.select(1000);
    }

    @Override
    protected void wakeup() {
        selector.wakeup();
    }

    @Override
    protected Iterator<AbstractIoSession> allSessions() throws Exception {
        return new IoSessionIterator(selector.keys());
    }

    protected Iterator<AbstractIoSession> selectedSessions() throws Exception {
        return new IoSessionIterator(selector.selectedKeys());
    }

    @Override
    protected SessionState state(IoSession session) {
        SelectionKey key = getSelectionKey(session);
        if (key == null) {
            return SessionState.PREPARING;
        }
        
        return key.isValid()? SessionState.OPEN : SessionState.CLOSED;
    }

    @Override
    protected int readyOps(IoSession session) throws Exception {
        return getSelectionKey(session).readyOps();
    }

    @Override
    protected int interestOps(IoSession session) throws Exception {
        return getSelectionKey(session).interestOps();
    }

    @Override
    protected void interestOps(IoSession session, int interestOps) throws Exception {
        getSelectionKey(session).interestOps(interestOps);
    }

    @Override
    protected void doAdd(IoSession session) throws Exception {
        SocketSessionImpl s = (SocketSessionImpl) session;
        SocketChannel ch = s.getChannel();
        ch.configureBlocking(false);
        s.setSelectionKey(
                ch.register(selector, SelectionKey.OP_READ, session));
    }
    
    @Override
    protected void doRemove(IoSession session) throws Exception {
        SocketSessionImpl s = (SocketSessionImpl) session;
        SocketChannel ch = s.getChannel();
        SelectionKey key = s.getSelectionKey();
        key.cancel();
        ch.close();
    }

    @Override
    protected int read(IoSession session, ByteBuffer buf) throws Exception {
        return getChannel(session).read(buf.buf());
    }
    
    @Override
    protected int write(IoSession session, ByteBuffer buf) throws Exception {
        return getChannel(session).write(buf.buf());
    }
    
    @Override
    protected long transferFile(IoSession session, FileRegion region) throws Exception {
        return region.getFileChannel().transferTo(region.getPosition(), region.getCount(), getChannel(session));
    }

    private SocketChannel getChannel(IoSession session) {
        return ((SocketSessionImpl) session).getChannel();
    }
    
    private SelectionKey getSelectionKey(IoSession session) {
        return ((SocketSessionImpl) session).getSelectionKey();
    }
    
    private static class IoSessionIterator implements Iterator<AbstractIoSession> {
        private final Iterator<SelectionKey> i;
        private IoSessionIterator(Set<SelectionKey> keys) {
            i = keys.iterator();
        }
        public boolean hasNext() {
            return i.hasNext();
        }

        public AbstractIoSession next() {
            SelectionKey key = i.next();
            return (AbstractIoSession) key.attachment();
        }

        public void remove() {
            i.remove();
        }
    }
}