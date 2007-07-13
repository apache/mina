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
package org.apache.mina.handler.chain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.mina.common.IoSession;

/**
 * A chain of {@link IoHandlerCommand}s.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class IoHandlerChain implements IoHandlerCommand {
    private static volatile int nextId = 0;

    private final int id = nextId++;

    private final String NEXT_COMMAND = IoHandlerChain.class.getName() + '.'
            + id + ".nextCommand";

    private final Map<String, Entry> name2entry = new HashMap<String, Entry>();

    private final Entry head;

    private final Entry tail;

    /**
     * Creates a new, empty chain of {@link IoHandlerCommand}s.
     */
    public IoHandlerChain() {
        head = new Entry(null, null, "head", createHeadCommand());
        tail = new Entry(head, null, "tail", createTailCommand());
        head.nextEntry = tail;
    }

    private IoHandlerCommand createHeadCommand() {
        return new IoHandlerCommand() {
            public void execute(NextCommand next, IoSession session,
                    Object message) throws Exception {
                next.execute(session, message);
            }
        };
    }

    private IoHandlerCommand createTailCommand() {
        return new IoHandlerCommand() {
            public void execute(NextCommand next, IoSession session,
                    Object message) throws Exception {
                next = (NextCommand) session.getAttribute(NEXT_COMMAND);
                if (next != null) {
                    next.execute(session, message);
                }
            }
        };
    }

    public Entry getEntry(String name) {
        Entry e = name2entry.get(name);
        if (e == null) {
            return null;
        }
        return e;
    }

    public IoHandlerCommand get(String name) {
        Entry e = getEntry(name);
        if (e == null) {
            return null;
        }

        return e.getCommand();
    }

    public NextCommand getNextCommand(String name) {
        Entry e = getEntry(name);
        if (e == null) {
            return null;
        }

        return e.getNextCommand();
    }

    public synchronized void addFirst(String name, IoHandlerCommand command) {
        checkAddable(name);
        register(head, name, command);
    }

    public synchronized void addLast(String name, IoHandlerCommand command) {
        checkAddable(name);
        register(tail.prevEntry, name, command);
    }

    public synchronized void addBefore(String baseName, String name,
            IoHandlerCommand command) {
        Entry baseEntry = checkOldName(baseName);
        checkAddable(name);
        register(baseEntry.prevEntry, name, command);
    }

    public synchronized void addAfter(String baseName, String name,
            IoHandlerCommand command) {
        Entry baseEntry = checkOldName(baseName);
        checkAddable(name);
        register(baseEntry, name, command);
    }

    public synchronized IoHandlerCommand remove(String name) {
        Entry entry = checkOldName(name);
        deregister(entry);
        return entry.getCommand();
    }

    public synchronized void clear() throws Exception {
        Iterator<String> it = new ArrayList<String>(name2entry.keySet())
                .iterator();
        while (it.hasNext()) {
            this.remove(it.next());
        }
    }

    private void register(Entry prevEntry, String name, IoHandlerCommand command) {
        Entry newEntry = new Entry(prevEntry, prevEntry.nextEntry, name,
                command);
        prevEntry.nextEntry.prevEntry = newEntry;
        prevEntry.nextEntry = newEntry;

        name2entry.put(name, newEntry);
    }

    private void deregister(Entry entry) {
        Entry prevEntry = entry.prevEntry;
        Entry nextEntry = entry.nextEntry;
        prevEntry.nextEntry = nextEntry;
        nextEntry.prevEntry = prevEntry;

        name2entry.remove(entry.name);
    }

    /**
     * Throws an exception when the specified filter name is not registered in this chain.
     *
     * @return An filter entry with the specified name.
     */
    private Entry checkOldName(String baseName) {
        Entry e = name2entry.get(baseName);
        if (e == null) {
            throw new IllegalArgumentException("Unknown filter name:"
                    + baseName);
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

    public void execute(NextCommand next, IoSession session, Object message)
            throws Exception {
        if (next != null) {
            session.setAttribute(NEXT_COMMAND, next);
        }

        try {
            callNextCommand(head, session, message);
        } finally {
            session.removeAttribute(NEXT_COMMAND);
        }
    }

    private void callNextCommand(Entry entry, IoSession session, Object message)
            throws Exception {
        entry.getCommand().execute(entry.getNextCommand(), session, message);
    }

    public List<Entry> getAll() {
        List<Entry> list = new ArrayList<Entry>();
        Entry e = head.nextEntry;
        while (e != tail) {
            list.add(e);
            e = e.nextEntry;
        }

        return list;
    }

    public List<Entry> getAllReversed() {
        List<Entry> list = new ArrayList<Entry>();
        Entry e = tail.prevEntry;
        while (e != head) {
            list.add(e);
            e = e.prevEntry;
        }
        return list;
    }

    public boolean contains(String name) {
        return getEntry(name) != null;
    }

    public boolean contains(IoHandlerCommand command) {
        Entry e = head.nextEntry;
        while (e != tail) {
            if (e.getCommand() == command) {
                return true;
            }
            e = e.nextEntry;
        }
        return false;
    }

    public boolean contains(Class<? extends IoHandlerCommand> commandType) {
        Entry e = head.nextEntry;
        while (e != tail) {
            if (commandType.isAssignableFrom(e.getCommand().getClass())) {
                return true;
            }
            e = e.nextEntry;
        }
        return false;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("{ ");

        boolean empty = true;

        Entry e = head.nextEntry;
        while (e != tail) {
            if (!empty) {
                buf.append(", ");
            } else {
                empty = false;
            }

            buf.append('(');
            buf.append(e.getName());
            buf.append(':');
            buf.append(e.getCommand());
            buf.append(')');

            e = e.nextEntry;
        }

        if (empty) {
            buf.append("empty");
        }

        buf.append(" }");

        return buf.toString();
    }

    /**
     * Represents a name-command pair that an {@link IoHandlerChain} contains.
     *
     * @author The Apache Directory Project (mina-dev@directory.apache.org)
     * @version $Rev$, $Date$
     */
    public class Entry {
        private Entry prevEntry;

        private Entry nextEntry;

        private final String name;

        private final IoHandlerCommand command;

        private final NextCommand nextCommand;

        private Entry(Entry prevEntry, Entry nextEntry, String name,
                IoHandlerCommand command) {
            if (command == null) {
                throw new NullPointerException("command");
            }
            if (name == null) {
                throw new NullPointerException("name");
            }

            this.prevEntry = prevEntry;
            this.nextEntry = nextEntry;
            this.name = name;
            this.command = command;
            this.nextCommand = new NextCommand() {
                public void execute(IoSession session, Object message)
                        throws Exception {
                    Entry nextEntry = Entry.this.nextEntry;
                    callNextCommand(nextEntry, session, message);
                }
            };
        }

        /**
         * Returns the name of the command.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the command.
         */
        public IoHandlerCommand getCommand() {
            return command;
        }

        /**
         * Returns the {@link IoHandlerCommand.NextCommand} of the command.
         */
        public NextCommand getNextCommand() {
            return nextCommand;
        }
    }
}
