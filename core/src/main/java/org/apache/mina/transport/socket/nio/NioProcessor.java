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
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.mina.common.AbstractIoProcessor;
import org.apache.mina.common.FileRegion;
import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.RuntimeIoException;

/**
 *
 * @author Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class NioProcessor extends AbstractIoProcessor<NioSession> {

    private static Selector newSelector() {
        try {
            return Selector.open();
        } catch (IOException e) {
            throw new RuntimeIoException("Failed to open a selector.", e);
        }
    }
    
    private final Selector selector;

    public NioProcessor(Executor executor) {
        super(executor);
        this.selector = newSelector();
    }

    @Override
    protected void doDispose() throws Exception {
        selector.close();
    }

    @Override
    protected boolean select(int timeout) throws Exception {
        return selector.select(1000) > 0;
    }

    @Override
    protected void wakeup() {
        selector.wakeup();
    }

    @Override
    protected Iterator<NioSession> allSessions() throws Exception {
        return new IoSessionIterator(selector.keys());
    }

    @Override
    protected Iterator<NioSession> selectedSessions() throws Exception {
        return new IoSessionIterator(selector.selectedKeys());
    }

    @Override
    protected void init(NioSession session) throws Exception {
        SelectableChannel ch = (SelectableChannel) session.getChannel();
        ch.configureBlocking(false);
        session.setSelectionKey(ch.register(selector, SelectionKey.OP_READ, session));
    }

    @Override
    protected void destroy(NioSession session) throws Exception {
        ByteChannel ch = session.getChannel();
        SelectionKey key = session.getSelectionKey();
        if (key != null) {
            key.cancel();
        }
        ch.close();
    }

    @Override
    protected SessionState state(NioSession session) {
        SelectionKey key = session.getSelectionKey();
        if (key == null) {
            return SessionState.PREPARING;
        }

        return key.isValid()? SessionState.OPEN : SessionState.CLOSED;
    }

    @Override
    protected boolean isReadable(NioSession session) throws Exception {
        SelectionKey key = session.getSelectionKey();
        return key.isValid() && key.isReadable();
    }

    @Override
    protected boolean isWritable(NioSession session) throws Exception {
        SelectionKey key = session.getSelectionKey();
        return key.isValid() && key.isWritable();
    }

    @Override
    protected boolean isInterestedInRead(NioSession session) throws Exception {
        return (session.getSelectionKey().interestOps() & SelectionKey.OP_READ) != 0;
    }

    @Override
    protected boolean isInterestedInWrite(NioSession session) throws Exception {
        return (session.getSelectionKey().interestOps() & SelectionKey.OP_WRITE) != 0;
    }

    @Override
    protected void setInterestedInRead(NioSession session, boolean value) throws Exception {
        SelectionKey key = session.getSelectionKey();
        if (value) {
            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
        } else {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
        }
    }

    @Override
    protected void setInterestedInWrite(NioSession session, boolean value)
            throws Exception {
        SelectionKey key = session.getSelectionKey();
        if (value) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        } else {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    @Override
    protected int read(NioSession session, IoBuffer buf) throws Exception {
        return session.getChannel().read(buf.buf());
    }

    @Override
    protected int write(NioSession session, IoBuffer buf) throws Exception {
        return session.getChannel().write(buf.buf());
    }

    @Override
    protected long transferFile(NioSession session, FileRegion region) throws Exception {
        return region.getFileChannel().transferTo(region.getPosition(), region.getCount(), session.getChannel());
    }

    protected static class IoSessionIterator implements Iterator<NioSession> {
        private final Iterator<SelectionKey> i;
        private IoSessionIterator(Set<SelectionKey> keys) {
            i = keys.iterator(); 
        }
        public boolean hasNext() {
            return i.hasNext();
        }

        public NioSession next() {
            SelectionKey key = i.next();
            return (NioSession) key.attachment();
        }

        public void remove() {
            i.remove();
        }
    }
}