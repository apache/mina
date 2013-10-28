/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.mina.transport.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class holds a Selector and handle all the incoming events for the sessions registered on this selector.ALl the
 * events will be processed by some dedicated thread, taken from a pool. It will loop forever, until the instance is
 * stopped.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NioSelectorLoop implements SelectorLoop {
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger(NioSelectorLoop.class);

    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** the selector managed by this class */
    private Selector selector;

    /** Read buffer for all the incoming bytes (default to 64Kb) */
    private final ByteBuffer readBuffer = ByteBuffer.allocateDirect(64 * 1024);

    /** The queue containing the channels to register on the selector */
    private final Queue<Registration> registrationQueue = new ConcurrentLinkedQueue<>();

    /**
     * Queue of runnable events to be run by the selector loop, used for running user code in the I/O loop and avoiding
     * concurrency issues
     */
    private final Queue<Runnable> runnableQueue = new ConcurrentLinkedQueue<>();

    /**
     * Creates an instance of the SelectorLoop.
     * 
     * @param prefix
     * @param index
     */
    public NioSelectorLoop(final String prefix) {
        this(prefix, -1);
    }

    /**
     * Creates an instance of the SelectorLoop.
     * 
     * @param prefix
     * @param index
     */
    public NioSelectorLoop(final String prefix, final int index) {
        String workerName = "SelectorWorker " + prefix;

        if (index >= 0) {
            workerName += "-" + index;
        }

        SelectorWorker worker = new SelectorWorker(workerName);

        try {
            if (IS_DEBUG) {
                LOG.debug("open a selector");
            }

            selector = Selector.open();
        } catch (final IOException ioe) {
            LOG.error("Impossible to open a new NIO selector, O/S is out of file descriptor ?");
            throw new IllegalStateException("Impossible to open a new NIO selector, O/S is out of file descriptor ?",
                    ioe);
        }

        if (IS_DEBUG) {
            LOG.debug("starting worker thread");
        }

        worker.start();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(boolean accept, boolean connect, boolean read, boolean write, SelectorListener listener,
            SelectableChannel channel, RegistrationCallback callback) {
        if (IS_DEBUG) {
            LOG.debug("registering : {} for accept : {}, connect: {}, read : {}, write : {}, channel : {}",
                    new Object[] { listener, accept, connect, read, write, channel });
        }

        int ops = 0;

        if (accept) {
            ops |= SelectionKey.OP_ACCEPT;
        }

        if (connect) {
            ops |= SelectionKey.OP_CONNECT;
        }

        if (read) {
            ops |= SelectionKey.OP_READ;
        }

        if (write) {
            ops |= SelectionKey.OP_WRITE;
        }

        // TODO : if it's the same selector/worker, we don't need to do that we could directly enqueue
        registrationQueue.add(new Registration(ops, channel, listener, callback));

        // Now, wakeup the selector in order to let it update the selectionKey status
        wakeup();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runInLoop(Runnable task) {
        runnableQueue.add(task);
        wakeup();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modifyRegistration(boolean accept, boolean read, boolean write, final SelectorListener listener,
            SelectableChannel channel, boolean wakeup) {
        if (IS_DEBUG) {
            LOG.debug("modifying registration : {} for accept : {}, read : {}, write : {}, channel : {}", new Object[] {
                                    listener, accept, read, write, channel });
        }

        final SelectionKey key = channel.keyFor(selector);

        if (key == null) {
            LOG.error("Trying to modify the registration of a not registered channel");
            return;
        }

        int ops = 0;

        if (accept) {
            ops |= SelectionKey.OP_ACCEPT;
        }

        if (read) {
            ops |= SelectionKey.OP_READ;
        }

        if (write) {
            ops |= SelectionKey.OP_WRITE;
        }

        key.interestOps(ops);

        // we need to wakeup for the registration to be modified (TODO : not needed if we are in the worker thread)
        if (wakeup) {
            wakeup();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregister(final SelectorListener listener, final SelectableChannel channel) {
        if (IS_DEBUG) {
            LOG.debug("unregistering : {}", listener);
        }

        final SelectionKey key = channel.keyFor(selector);

        if (key == null) {
            LOG.error("Trying to modify the registration of a not registered channel");
            return;
        }
        key.cancel();
        key.attach(null);

        if (IS_DEBUG) {
            LOG.debug("unregistering : {} done !", listener);
        }
    }

    /**
     * The worker processing incoming session creation, session destruction requests, session write and reads. It will
     * also bind new servers.
     */
    private class SelectorWorker extends Thread {

        public SelectorWorker(String name) {
            super(name);
            setDaemon(true);
        }

        @Override
        public void run() {

            for (;;) {
                try {
                    if (IS_DEBUG) {
                        LOG.debug("selecting...");
                    }

                    final int readyCount = selector.select();

                    if (IS_DEBUG) {
                        LOG.debug("... done selecting : {} events", readyCount);
                    }

                    if (readyCount > 0) {
                        final Iterator<SelectionKey> it = selector.selectedKeys().iterator();

                        while (it.hasNext()) {
                            final SelectionKey key = it.next();
                            final SelectorListener listener = (SelectorListener) key.attachment();
                            int ops = key.readyOps();
                            boolean isAcceptable = (ops & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT;
                            boolean isConnectable = (ops & SelectionKey.OP_CONNECT) == SelectionKey.OP_CONNECT;
                            boolean isReadable = (ops & SelectionKey.OP_READ) == SelectionKey.OP_READ;
                            boolean isWritable = (ops & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE;
                            listener.ready(isAcceptable, isConnectable, isReadable, isReadable ? readBuffer : null,
                                    isWritable);
                            // if you don't remove the event of the set, the selector will present you this event again
                            // and again
                            if (IS_DEBUG) {
                                LOG.debug("remove");
                            }

                            it.remove();
                        }
                    }

                    // new registration
                    while (!registrationQueue.isEmpty()) {
                        final Registration reg = registrationQueue.poll();

                        try {
                            SelectionKey selectionKey = reg.channel.register(selector, reg.ops, reg.listener);

                            if (reg.getCallback() != null) {
                                reg.getCallback().done(selectionKey);
                            }
                        } catch (final ClosedChannelException ex) {
                            // dead session..
                            LOG.error("socket is already dead", ex);
                        }
                    }

                    // tasks
                    while (!runnableQueue.isEmpty()) {
                        runnableQueue.poll().run();
                    }
                } catch (final Exception e) {
                    LOG.error("Unexpected exception : ", e);
                }
            }
        }
    }

    @Override
    public void wakeup() {
        selector.wakeup();
    }

    private static class Registration {

        public Registration(int ops, SelectableChannel channel, SelectorListener listener, RegistrationCallback callback) {
            this.ops = ops;
            this.channel = channel;
            this.listener = listener;
            this.callback = callback;
        }

        private final int ops;

        private final SelectableChannel channel;

        private final SelectorListener listener;

        private final RegistrationCallback callback;

        public RegistrationCallback getCallback() {
            return callback;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("Registration : [");

            boolean hasOp = false;

            if ((ops & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                sb.append("OP_READ");
                hasOp = true;
            }

            if ((ops & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
                if (hasOp) {
                    sb.append("|");
                }

                sb.append("OP_WRITE");
                hasOp = true;
            }

            if ((ops & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                if (hasOp) {
                    sb.append("|");
                }

                sb.append("OP_ACCEPT");
                hasOp = true;
            }

            if ((ops & SelectionKey.OP_CONNECT) == SelectionKey.OP_CONNECT) {
                if (hasOp) {
                    sb.append("|");
                }

                sb.append("OP_CONNECT");
                hasOp = true;
            }

            if (channel != null) {
                sb.append(", ").append(channel);
            }

            return sb.toString();
        }
    }
}