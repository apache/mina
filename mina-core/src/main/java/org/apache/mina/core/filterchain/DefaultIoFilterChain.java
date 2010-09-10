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
package org.apache.mina.core.filterchain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A default implementation of {@link IoFilterChain} that provides
 * all operations for developers who want to implement their own
 * transport layer once used with {@link AbstractIoSession}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultIoFilterChain implements IoFilterChain {
    /**
     * A session attribute that stores an {@link IoFuture} related with
     * the {@link IoSession}.  {@link DefaultIoFilterChain} clears this
     * attribute and notifies the future when {@link #fireSessionCreated()}
     * or {@link #fireExceptionCaught(Throwable)} is invoked.
     */
    public static final AttributeKey SESSION_CREATED_FUTURE = new AttributeKey(
            DefaultIoFilterChain.class, "connectFuture");

    /** The associated session */
    private final AbstractIoSession session;

    private final Map<String, Entry> name2entry = new ConcurrentHashMap<String, Entry>();

    /** The chain head */
    private final EntryImpl head;

    /** The chain tail */
    private final EntryImpl tail;

    /** The logger for this class */
    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultIoFilterChain.class);


    /**
     * Create a new default chain, associated with a session. It will only contain a
     * HeadFilter and a TailFilter.
     *
     * @param session The session associated with the created filter chain
     */
    public DefaultIoFilterChain(AbstractIoSession session) {
        if (session == null) {
            throw new IllegalArgumentException("session");
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

    public Entry getEntry(IoFilter filter) {
        EntryImpl e = head.nextEntry;
        while (e != tail) {
            if (e.getFilter() == filter) {
                return e;
            }
            e = e.nextEntry;
        }
        return null;
    }

    public Entry getEntry(Class<? extends IoFilter> filterType) {
        EntryImpl e = head.nextEntry;
        while (e != tail) {
            if (filterType.isAssignableFrom(e.getFilter().getClass())) {
                return e;
            }
            e = e.nextEntry;
        }
        return null;
    }

    public IoFilter get(String name) {
        Entry e = getEntry(name);
        if (e == null) {
            return null;
        }

        return e.getFilter();
    }

    public IoFilter get(Class<? extends IoFilter> filterType) {
        Entry e = getEntry(filterType);
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

    public NextFilter getNextFilter(IoFilter filter) {
        Entry e = getEntry(filter);
        if (e == null) {
            return null;
        }

        return e.getNextFilter();
    }

    public NextFilter getNextFilter(Class<? extends IoFilter> filterType) {
        Entry e = getEntry(filterType);
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

    public synchronized IoFilter replace(
            Class<? extends IoFilter> oldFilterType, IoFilter newFilter) {
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
        List<IoFilterChain.Entry> l = new ArrayList<IoFilterChain.Entry>(
                name2entry.values());
        for (IoFilterChain.Entry entry : l) {
            try {
                deregister((EntryImpl) entry);
            } catch (Exception e) {
                throw new IoFilterLifeCycleException("clear(): "
                        + entry.getName() + " in " + getSession(), e);
            }
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
            IoFilter filter = entry.getFilter();
            NextFilter nextFilter = entry.getNextFilter();
            filter.sessionCreated(nextFilter, session);
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
            IoFilter filter = entry.getFilter();
            NextFilter nextFilter = entry.getNextFilter();
            filter.sessionOpened(nextFilter, session);
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
            IoFilter filter = entry.getFilter();
            NextFilter nextFilter = entry.getNextFilter();
            filter.sessionClosed(nextFilter, session);
        } catch (Throwable e) {
            fireExceptionCaught(e);
        }
    }

    public void fireSessionIdle(IdleStatus status) {
        session.increaseIdleCount(status, System.currentTimeMillis());
        Entry head = this.head;
        callNextSessionIdle(head, session, status);
    }

    private void callNextSessionIdle(Entry entry, IoSession session,
            IdleStatus status) {
        try {
            IoFilter filter = entry.getFilter();
            NextFilter nextFilter = entry.getNextFilter();
            filter.sessionIdle(nextFilter, session,
                    status);
        } catch (Throwable e) {
            fireExceptionCaught(e);
        }
    }

    public void fireMessageReceived(Object message) {
        if (message instanceof IoBuffer) {
            session.increaseReadBytes(((IoBuffer) message).remaining(), System
                    .currentTimeMillis());
        }

        Entry head = this.head;
        callNextMessageReceived(head, session, message);
    }

    private void callNextMessageReceived(Entry entry, IoSession session,
            Object message) {
        try {
            IoFilter filter = entry.getFilter();
            NextFilter nextFilter = entry.getNextFilter();
            filter.messageReceived(nextFilter, session,
                    message);
        } catch (Throwable e) {
            fireExceptionCaught(e);
        }
    }

    public void fireMessageSent(WriteRequest request) {
        session.increaseWrittenMessages(request, System.currentTimeMillis());

        try {
            request.getFuture().setWritten();
        } catch (Throwable t) {
            fireExceptionCaught(t);
        }

        Entry head = this.head;
        
        if (!request.isEncoded()) {
            callNextMessageSent(head, session, request);
        }
    }

    private void callNextMessageSent(Entry entry, IoSession session,
            WriteRequest writeRequest) {
        try {
            IoFilter filter = entry.getFilter();
            NextFilter nextFilter = entry.getNextFilter();
            filter.messageSent(nextFilter, session,
                    writeRequest);
        } catch (Throwable e) {
            fireExceptionCaught(e);
        }
    }

    public void fireExceptionCaught(Throwable cause) {
        Entry head = this.head;
        callNextExceptionCaught(head, session, cause);
    }

    private void callNextExceptionCaught(Entry entry, IoSession session,
            Throwable cause) {
        // Notify the related future.
        ConnectFuture future = (ConnectFuture) session
                .removeAttribute(SESSION_CREATED_FUTURE);
        if (future == null) {
            try {
                IoFilter filter = entry.getFilter();
                NextFilter nextFilter = entry.getNextFilter();
                filter.exceptionCaught(nextFilter,
                        session, cause);
            } catch (Throwable e) {
                LOGGER
                        .warn(
                                "Unexpected exception from exceptionCaught handler.",
                                e);
            }
        } else {
            // Please note that this place is not the only place that
            // calls ConnectFuture.setException().
            session.close(true);
            future.setException(cause);
        }
    }

    public void fireFilterWrite(WriteRequest writeRequest) {
        Entry tail = this.tail;
        callPreviousFilterWrite(tail, session, writeRequest);
    }

    private void callPreviousFilterWrite(Entry entry, IoSession session,
            WriteRequest writeRequest) {
        try {
            IoFilter filter = entry.getFilter();
            NextFilter nextFilter = entry.getNextFilter();
            filter.filterWrite(nextFilter, session, writeRequest);
        } catch (Throwable e) {
            writeRequest.getFuture().setException(e);
            fireExceptionCaught(e);
        }
    }

    public void fireFilterClose() {
        Entry tail = this.tail;
        callPreviousFilterClose(tail, session);
    }

    private void callPreviousFilterClose(Entry entry, IoSession session) {
        try {
            IoFilter filter = entry.getFilter();
            NextFilter nextFilter = entry.getNextFilter();
            filter.filterClose(nextFilter, session);
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
        return getEntry(filter) != null;
    }

    public boolean contains(Class<? extends IoFilter> filterType) {
        return getEntry(filterType) != null;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
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

    private class HeadFilter extends IoFilterAdapter {
        @SuppressWarnings("unchecked")
        @Override
        public void filterWrite(NextFilter nextFilter, IoSession session,
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
            if (!s.isWriteSuspended()) {
                s.getProcessor().flush(s);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void filterClose(NextFilter nextFilter, IoSession session)
                throws Exception {
            ((AbstractIoSession) session).getProcessor().remove(((AbstractIoSession) session));
        }
    }

    private static class TailFilter extends IoFilterAdapter {
        @Override
        public void sessionCreated(NextFilter nextFilter, IoSession session)
                throws Exception {
            try {
                session.getHandler().sessionCreated(session);
            } finally {
                // Notify the related future.
                ConnectFuture future = (ConnectFuture) session
                        .removeAttribute(SESSION_CREATED_FUTURE);
                if (future != null) {
                    future.setSession(session);
                }
            }
        }

        @Override
        public void sessionOpened(NextFilter nextFilter, IoSession session)
                throws Exception {
            session.getHandler().sessionOpened(session);
        }

        @Override
        public void sessionClosed(NextFilter nextFilter, IoSession session)
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
        public void sessionIdle(NextFilter nextFilter, IoSession session,
                IdleStatus status) throws Exception {
            session.getHandler().sessionIdle(session, status);
        }

        @Override
        public void exceptionCaught(NextFilter nextFilter, IoSession session,
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
        public void messageReceived(NextFilter nextFilter, IoSession session,
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
                throw new IllegalArgumentException("filter");
            }
            if (name == null) {
                throw new IllegalArgumentException("name");
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

                public String toString() {
                    return EntryImpl.this.nextEntry.name;
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
                throw new IllegalArgumentException("filter");
            }

            this.filter = filter;
        }

        public NextFilter getNextFilter() {
            return nextFilter;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            // Add the current filter
            sb.append("('").append(getName()).append('\'');

            // Add the previous filter
            sb.append(", prev: '");

            if (prevEntry != null) {
                sb.append(prevEntry.name);
                sb.append(':');
                sb.append(prevEntry.getFilter().getClass().getSimpleName());
            } else {
                sb.append("null");
            }

            // Add the next filter
            sb.append("', next: '");

            if (nextEntry != null) {
                sb.append(nextEntry.name);
                sb.append(':');
                sb.append(nextEntry.getFilter().getClass().getSimpleName());
            } else {
                sb.append("null");
            }

            sb.append("')");
            return sb.toString();
        }

        public void addAfter(String name, IoFilter filter) {
            DefaultIoFilterChain.this.addAfter(getName(), name, filter);
        }

        public void addBefore(String name, IoFilter filter) {
            DefaultIoFilterChain.this.addBefore(getName(), name, filter);
        }

        public void remove() {
            DefaultIoFilterChain.this.remove(getName());
        }

        public void replace(IoFilter newFilter) {
            DefaultIoFilterChain.this.replace(getName(), newFilter);
        }
    }
}
