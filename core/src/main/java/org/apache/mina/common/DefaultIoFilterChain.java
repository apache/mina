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
package org.apache.mina.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.mina.common.IoFilter.NextFilter;

/**
 * A default implementation of {@link IoFilterChain} that provides
 * all operations for developers who want to implement their own
 * transport layer once used with {@link AbstractIoSession}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultIoFilterChain implements IoFilterChain {
    /**
     * A session attribute that stores a {@link ConnectFuture} related with
     * the {@link IoSession}.  {@link DefaultIoFilterChain} clears this
     * attribute and notifies the future when {@link #fireSessionOpened()}
     * or {@link #fireExceptionCaught(Throwable)} is invoked
     */
    public static final String CONNECT_FUTURE = DefaultIoFilterChain.class
            .getName()
            + ".connectFuture";

    private final AbstractIoSession session;

    private final Map<String, Entry> name2entry = new HashMap<String, Entry>();

    private final EntryImpl head;

    private final EntryImpl tail;

    public DefaultIoFilterChain(AbstractIoSession session) {
        if (session == null) {
            throw new NullPointerException("session");
        }

        this.session = session;
        head = new EntryImpl(null, null, "head", new HeadFilter());
        tail = new EntryImpl(head, null, "tail", new TailFilter());
        head.nextEntry = tail;
    }

    public IoSession getSession() {
        return session;
    }

    public Entry getEntry(String name) {
        Entry e = name2entry.get(name);
        if (e == null) {
            return null;
        }
        return e;
    }

    public IoFilter get(String name) {
        Entry e = getEntry(name);
        if (e == null) {
            return null;
        }

        return e.getFilter();
    }

    public NextFilter getNextFilter(String name) {
        Entry e = getEntry(name);
        if (e == null) {
            return null;
        }

        return e.getNextFilter();
    }

    public synchronized void addFirst(String name, IoFilter filter) {
        checkAddable(name);
        register(head, name, filter);
    }

    public synchronized void addLast(String name, IoFilter filter) {
        checkAddable(name);
        register(tail.prevEntry, name, filter);
    }

    public synchronized void addBefore(String baseName, String name,
            IoFilter filter) {
        EntryImpl baseEntry = checkOldName(baseName);
        checkAddable(name);
        register(baseEntry.prevEntry, name, filter);
    }

    public synchronized void addAfter(String baseName, String name,
            IoFilter filter) {
        EntryImpl baseEntry = checkOldName(baseName);
        checkAddable(name);
        register(baseEntry, name, filter);
    }

    public synchronized IoFilter remove(String name) {
        EntryImpl entry = checkOldName(name);
        deregister(entry);
        return entry.getFilter();
    }

    public synchronized void remove(IoFilter filter) {
        EntryImpl e = head.nextEntry;
        while (e != tail) {
            if (e.getFilter() == filter) {
                deregister(e);
                return;
            }
            e = e.nextEntry;
        }
        throw new IllegalArgumentException("Filter not found: "
                + filter.getClass().getName());
    }

    public synchronized IoFilter remove(Class<? extends IoFilter> filterType) {
        EntryImpl e = head.nextEntry;
        while (e != tail) {
            if (filterType.isAssignableFrom(e.getFilter().getClass())) {
                IoFilter oldFilter = e.getFilter();
                deregister(e);
                return oldFilter;
            }
            e = e.nextEntry;
        }
        throw new IllegalArgumentException("Filter not found: "
                + filterType.getName());
    }

    public synchronized IoFilter replace(String name, IoFilter newFilter) {
        EntryImpl entry = checkOldName(name);
        IoFilter oldFilter = entry.getFilter();
        entry.setFilter(newFilter);
        return oldFilter;
    }

    public synchronized void replace(IoFilter oldFilter, IoFilter newFilter) {
        EntryImpl e = head.nextEntry;
        while (e != tail) {
            if (e.getFilter() == oldFilter) {
                e.setFilter(newFilter);
                return;
            }
            e = e.nextEntry;
        }
        throw new IllegalArgumentException("Filter not found: "
                + oldFilter.getClass().getName());
    }

    public synchronized IoFilter replace(Class<? extends IoFilter> oldFilterType,
            IoFilter newFilter) {
        EntryImpl e = head.nextEntry;
        while (e != tail) {
            if (oldFilterType.isAssignableFrom(e.getFilter().getClass())) {
                IoFilter oldFilter = e.getFilter();
                e.setFilter(newFilter);
                return oldFilter;
            }
            e = e.nextEntry;
        }
        throw new IllegalArgumentException("Filter not found: "
                + oldFilterType.getName());
    }

    public synchronized void clear() throws Exception {
        Iterator<String> it = new ArrayList<String>(name2entry.keySet())
                .iterator();
        while (it.hasNext()) {
            this.remove(it.next());
        }
    }

    private void register(EntryImpl prevEntry, String name, IoFilter filter) {
        EntryImpl newEntry = new EntryImpl(prevEntry, prevEntry.nextEntry,
                name, filter);

        try {
            filter.onPreAdd(this, name, newEntry.getNextFilter());
        } catch (Exception e) {
            throw new IoFilterLifeCycleException("onPreAdd(): " + name + ':'
                    + filter + " in " + getSession(), e);
        }

        prevEntry.nextEntry.prevEntry = newEntry;
        prevEntry.nextEntry = newEntry;
        name2entry.put(name, newEntry);

        try {
            filter.onPostAdd(this, name, newEntry.getNextFilter());
        } catch (Exception e) {
            deregister0(newEntry);
            throw new IoFilterLifeCycleException("onPostAdd(): " + name + ':'
                    + filter + " in " + getSession(), e);
        }
    }

    private void deregister(EntryImpl entry) {
        IoFilter filter = entry.getFilter();

        try {
            filter.onPreRemove(this, entry.getName(), entry.getNextFilter());
        } catch (Exception e) {
            throw new IoFilterLifeCycleException("onPreRemove(): "
                    + entry.getName() + ':' + filter + " in " + getSession(), e);
        }

        deregister0(entry);

        try {
            filter.onPostRemove(this, entry.getName(), entry.getNextFilter());
        } catch (Exception e) {
            throw new IoFilterLifeCycleException("onPostRemove(): "
                    + entry.getName() + ':' + filter + " in " + getSession(), e);
        }
    }

    private void deregister0(EntryImpl entry) {
        EntryImpl prevEntry = entry.prevEntry;
        EntryImpl nextEntry = entry.nextEntry;
        prevEntry.nextEntry = nextEntry;
        nextEntry.prevEntry = prevEntry;

        name2entry.remove(entry.name);
    }

    /**
     * Throws an exception when the specified filter name is not registered in this chain.
     *
     * @return An filter entry with the specified name.
     */
    private EntryImpl checkOldName(String baseName) {
        EntryImpl e = (EntryImpl) name2entry.get(baseName);
        if (e == null) {
            throw new IllegalArgumentException("Filter not found:" + baseName);
        }
        return e;
    }

    /**
     * Checks the specified filter name is already taken and throws an exception if already taken.
     */
    private void checkAddable(String name) {
        if (name2entry.containsKey(name)) {
            throw new IllegalArgumentException(
                    "Other filter is using the same name '" + name + "'");
        }
    }
    
    public void fireSessionCreated() {
        Entry head = this.head;
        callNextSessionCreated(head, session);
    }

    private void callNextSessionCreated(Entry entry, IoSession session) {
        try {
            entry.getFilter().sessionCreated(entry.getNextFilter(), session);
        } catch (Throwable e) {
            fireExceptionCaught(e);
        }
    }

    public void fireSessionOpened() {
        Entry head = this.head;
        callNextSessionOpened(head, session);
    }

    private void callNextSessionOpened(Entry entry, IoSession session) {
        try {
            entry.getFilter().sessionOpened(entry.getNextFilter(), session);
        } catch (Throwable e) {
            fireExceptionCaught(e);
        }
    }

    public void fireSessionClosed() {
        // Update future.
        try {
            session.getCloseFuture().setClosed();
        } catch (Throwable t) {
            fireExceptionCaught(t);
        }

        // And start the chain.
        Entry head = this.head;
        callNextSessionClosed(head, session);
    }

    private void callNextSessionClosed(Entry entry, IoSession session) {
        try {
            entry.getFilter().sessionClosed(entry.getNextFilter(), session);

        } catch (Throwable e) {
            fireExceptionCaught(e);
        }
    }

    public void fireSessionIdle(IdleStatus status) {
        Entry head = this.head;
        callNextSessionIdle(head, session, status);
    }

    private void callNextSessionIdle(Entry entry, IoSession session,
            IdleStatus status) {
        try {
            entry.getFilter().sessionIdle(entry.getNextFilter(), session,
                    status);
        } catch (Throwable e) {
            fireExceptionCaught(e);
        }
    }

    public void fireMessageReceived(Object message) {
        if (message instanceof ByteBuffer) {
            session.increaseReadBytes(((ByteBuffer) message).remaining());
        }

        Entry head = this.head;
        callNextMessageReceived(head, session, message);
    }

    private void callNextMessageReceived(Entry entry, IoSession session,
            Object message) {
        try {
            entry.getFilter().messageReceived(entry.getNextFilter(), session,
                    message);
        } catch (Throwable e) {
            fireExceptionCaught(e);
        }
    }

    public void fireMessageSent(WriteRequest request) {
        Object message = request.getMessage();
        if (message instanceof ByteBuffer) {
            ByteBuffer b = (ByteBuffer) message;
            if (b.hasRemaining()) {
                session.increaseWrittenBytes(((ByteBuffer) message).remaining());
            } else {
                session.increaseWrittenMessages();
            }
        } else {
            session.increaseWrittenMessages();
        }

        try {
            request.getFuture().setWritten(true);
        } catch (Throwable t) {
            fireExceptionCaught(t);
        }

        Entry head = this.head;
        callNextMessageSent(head, session, request);
    }

    private void callNextMessageSent(Entry entry, IoSession session,
            WriteRequest writeRequest) {
        try {
            entry.getFilter().messageSent(entry.getNextFilter(), session,
                    writeRequest);
        } catch (Throwable e) {
            fireExceptionCaught(e);
        }
    }

    public void fireExceptionCaught(Throwable cause) {
        // Notify the related ConnectFuture
        // if the session is created from SocketConnector.
        ConnectFuture future = (ConnectFuture) session
                .removeAttribute(CONNECT_FUTURE);
        if (future == null) {
            Entry head = this.head;
            callNextExceptionCaught(head, session, cause);
        } else {
            // Please note that this place is not the only place that
            // calls ConnectFuture.setException().
            future.setException(cause);
        }
    }

    private void callNextExceptionCaught(Entry entry, IoSession session,
            Throwable cause) {
        try {
            entry.getFilter().exceptionCaught(entry.getNextFilter(), session,
                    cause);
        } catch (Throwable e) {
            IoSessionLogger.warn(session,
                    "Unexpected exception from exceptionCaught handler.", e);
        }
    }

    public void fireFilterWrite(WriteRequest writeRequest) {
        Entry tail = this.tail;
        callPreviousFilterWrite(tail, session, writeRequest);
    }

    private void callPreviousFilterWrite(Entry entry, IoSession session,
            WriteRequest writeRequest) {
        try {
            entry.getFilter().filterWrite(entry.getNextFilter(), session,
                    writeRequest);
        } catch (Throwable e) {
            writeRequest.getFuture().setWritten(false);
            fireExceptionCaught(e);
        }
    }

    public void fireFilterClose() {
        Entry tail = this.tail;
        callPreviousFilterClose(tail, session);
    }

    private void callPreviousFilterClose(Entry entry, IoSession session) {
        try {
            entry.getFilter().filterClose(entry.getNextFilter(), session);
        } catch (Throwable e) {
            fireExceptionCaught(e);
        }
    }

    public List<Entry> getAll() {
        List<Entry> list = new ArrayList<Entry>();
        EntryImpl e = head.nextEntry;
        while (e != tail) {
            list.add(e);
            e = e.nextEntry;
        }

        return list;
    }

    public List<Entry> getAllReversed() {
        List<Entry> list = new ArrayList<Entry>();
        EntryImpl e = tail.prevEntry;
        while (e != head) {
            list.add(e);
            e = e.prevEntry;
        }
        return list;
    }

    public boolean contains(String name) {
        return getEntry(name) != null;
    }

    public boolean contains(IoFilter filter) {
        EntryImpl e = head.nextEntry;
        while (e != tail) {
            if (e.getFilter() == filter) {
                return true;
            }
            e = e.nextEntry;
        }
        return false;
    }

    public boolean contains(Class<? extends IoFilter> filterType) {
        EntryImpl e = head.nextEntry;
        while (e != tail) {
            if (filterType.isAssignableFrom(e.getFilter().getClass())) {
                return true;
            }
            e = e.nextEntry;
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("{ ");

        boolean empty = true;

        EntryImpl e = head.nextEntry;
        while (e != tail) {
            if (!empty) {
                buf.append(", ");
            } else {
                empty = false;
            }

            buf.append('(');
            buf.append(e.getName());
            buf.append(':');
            buf.append(e.getFilter());
            buf.append(')');

            e = e.nextEntry;
        }

        if (empty) {
            buf.append("empty");
        }

        buf.append(" }");

        return buf.toString();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            this.clear();
        } finally {
            super.finalize();
        }
    }

    private class HeadFilter extends IoFilterAdapter {
        @Override
        public void sessionCreated(NextFilter nextFilter, IoSession session) {
            nextFilter.sessionCreated(session);
        }

        @Override
        public void sessionOpened(NextFilter nextFilter, IoSession session) {
            nextFilter.sessionOpened(session);
        }

        @Override
        public void sessionClosed(NextFilter nextFilter, IoSession session) {
            nextFilter.sessionClosed(session);
        }

        @Override
        public void sessionIdle(NextFilter nextFilter, IoSession session,
                IdleStatus status) {
            nextFilter.sessionIdle(session, status);
        }

        @Override
        public void exceptionCaught(NextFilter nextFilter, IoSession session,
                Throwable cause) {
            nextFilter.exceptionCaught(session, cause);
        }

        @Override
        public void messageReceived(NextFilter nextFilter, IoSession session,
                Object message) {
            nextFilter.messageReceived(session, message);
        }

        @Override
        public void messageSent(NextFilter nextFilter, IoSession session,
                WriteRequest writeRequest) {
            nextFilter.messageSent(session, writeRequest);
        }

        @Override
        public void filterWrite(NextFilter nextFilter, IoSession session,
                WriteRequest writeRequest) throws Exception {

            AbstractIoSession s = (AbstractIoSession) session;

            // Maintain counters.
            if (writeRequest.getMessage() instanceof ByteBuffer) {
                ByteBuffer buffer = (ByteBuffer) writeRequest.getMessage();
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
                    s.increaseScheduledWriteBytes(buffer.remaining());
                }
            }

            s.getWriteRequestQueue().add(writeRequest);
            if (s.getTrafficMask().isWritable()) {
                s.getProcessor().flush(s);
            }
        }

        @Override
        public void filterClose(NextFilter nextFilter, IoSession session)
                throws Exception {
            AbstractIoSession s = (AbstractIoSession) session;
            s.getProcessor().remove(s);
        }
    }

    private static class TailFilter extends IoFilterAdapter {
        @Override
        public void sessionCreated(NextFilter nextFilter, IoSession session)
                throws Exception {
            session.getHandler().sessionCreated(session);
        }

        @Override
        public void sessionOpened(NextFilter nextFilter, IoSession session)
                throws Exception {
            try {
                session.getHandler().sessionOpened(session);
            } finally {
                // Notify the related ConnectFuture
                // if the session is created from SocketConnector.
                ConnectFuture future = (ConnectFuture) session
                        .removeAttribute(CONNECT_FUTURE);
                if (future != null) {
                    future.setSession(session);
                }
            }
        }

        @Override
        public void sessionClosed(NextFilter nextFilter, IoSession session)
                throws Exception {
            try {
                session.getHandler().sessionClosed(session);
            } finally {
                // Remove all filters.
                session.getFilterChain().clear();
            }
        }

        @Override
        public void sessionIdle(NextFilter nextFilter, IoSession session,
                IdleStatus status) throws Exception {
            session.getHandler().sessionIdle(session, status);
        }

        @Override
        public void exceptionCaught(NextFilter nextFilter, IoSession session,
                Throwable cause) throws Exception {
            session.getHandler().exceptionCaught(session, cause);
        }

        @Override
        public void messageReceived(NextFilter nextFilter, IoSession session,
                Object message) throws Exception {
            session.getHandler().messageReceived(session, message);
        }

        @Override
        public void messageSent(NextFilter nextFilter, IoSession session,
                WriteRequest writeRequest) throws Exception {
            session.getHandler()
                    .messageSent(session, writeRequest.getMessage());
        }

        @Override
        public void filterWrite(NextFilter nextFilter, IoSession session,
                WriteRequest writeRequest) throws Exception {
            nextFilter.filterWrite(session, writeRequest);
        }

        @Override
        public void filterClose(NextFilter nextFilter, IoSession session)
                throws Exception {
            nextFilter.filterClose(session);
        }
    }

    private class EntryImpl implements Entry {
        private EntryImpl prevEntry;

        private EntryImpl nextEntry;

        private final String name;

        private IoFilter filter;

        private final NextFilter nextFilter;

        private EntryImpl(EntryImpl prevEntry, EntryImpl nextEntry,
                String name, IoFilter filter) {
            if (filter == null) {
                throw new NullPointerException("filter");
            }
            if (name == null) {
                throw new NullPointerException("name");
            }

            this.prevEntry = prevEntry;
            this.nextEntry = nextEntry;
            this.name = name;
            this.filter = filter;
            this.nextFilter = new NextFilter() {
                public void sessionCreated(IoSession session) {
                    Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextSessionCreated(nextEntry, session);
                }

                public void sessionOpened(IoSession session) {
                    Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextSessionOpened(nextEntry, session);
                }

                public void sessionClosed(IoSession session) {
                    Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextSessionClosed(nextEntry, session);
                }

                public void sessionIdle(IoSession session, IdleStatus status) {
                    Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextSessionIdle(nextEntry, session, status);
                }

                public void exceptionCaught(IoSession session, Throwable cause) {
                    Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextExceptionCaught(nextEntry, session, cause);
                }

                public void messageReceived(IoSession session, Object message) {
                    Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextMessageReceived(nextEntry, session, message);
                }

                public void messageSent(IoSession session,
                        WriteRequest writeRequest) {
                    Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextMessageSent(nextEntry, session, writeRequest);
                }

                public void filterWrite(IoSession session,
                        WriteRequest writeRequest) {
                    Entry nextEntry = EntryImpl.this.prevEntry;
                    callPreviousFilterWrite(nextEntry, session, writeRequest);
                }

                public void filterClose(IoSession session) {
                    Entry nextEntry = EntryImpl.this.prevEntry;
                    callPreviousFilterClose(nextEntry, session);
                }
            };
        }

        public String getName() {
            return name;
        }

        public IoFilter getFilter() {
            return filter;
        }

        private void setFilter(IoFilter filter) {
            if (filter == null) {
                throw new NullPointerException("filter");
            }

            this.filter = filter;
        }

        public NextFilter getNextFilter() {
            return nextFilter;
        }

        @Override
        public String toString() {
            return "(" + getName() + ':' + filter + ')';
        }
    }
}
