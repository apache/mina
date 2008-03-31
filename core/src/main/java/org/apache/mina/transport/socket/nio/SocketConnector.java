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
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoConnectorConfig;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.support.AbstractIoFilterChain;
import org.apache.mina.common.support.BaseIoConnector;
import org.apache.mina.common.support.DefaultConnectFuture;
import org.apache.mina.util.NamePreservingRunnable;
import org.apache.mina.util.NewThreadExecutor;

/**
 * {@link IoConnector} for socket transport (TCP/IP).
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev: 389042 $, $Date: 2006-03-27 07:49:41Z $
 */
public class SocketConnector extends BaseIoConnector {
    private static final AtomicInteger nextId = new AtomicInteger();

    private final Object lock = new Object();

    private final int id = nextId.getAndIncrement();

    private final String threadName = "SocketConnector-" + id;

    private SocketConnectorConfig defaultConfig = new SocketConnectorConfig();

    private final Queue<ConnectionRequest> connectQueue = new ConcurrentLinkedQueue<ConnectionRequest>();

    private final SocketIoProcessor[] ioProcessors;

    private final int processorCount;

    private final Executor executor;

    private volatile Selector selector;

    private Worker worker;

    private int processorDistributor = 0;

    private int workerTimeout = 60; // 1 min.

    /**
     * Create a connector with a single processing thread using a NewThreadExecutor
     */
    public SocketConnector() {
        this(1, new NewThreadExecutor());
    }

    /**
     * Create a connector with the desired number of processing threads
     *
     * @param processorCount Number of processing threads
     * @param executor Executor to use for launching threads
     */
    public SocketConnector(int processorCount, Executor executor) {
        if (processorCount < 1) {
            throw new IllegalArgumentException(
                    "Must have at least one processor");
        }

        this.executor = executor;
        this.processorCount = processorCount;
        ioProcessors = new SocketIoProcessor[processorCount];

        for (int i = 0; i < processorCount; i++) {
            ioProcessors[i] = new SocketIoProcessor(
                    "SocketConnectorIoProcessor-" + id + "." + i, executor);
        }
    }

    /**
     * How many seconds to keep the connection thread alive between connection requests
     *
     * @return the number of seconds to keep connection thread alive.
     *         0 means that the connection thread will terminate immediately
     *         when there's no connection to make.
     */
    public int getWorkerTimeout() {
        return workerTimeout;
    }

    /**
     * Set how many seconds the connection worker thread should remain alive once idle before terminating itself.
     *
     * @param workerTimeout the number of seconds to keep thread alive.
     *                      Must be >=0.  If 0 is specified, the connection
     *                      worker thread will terminate immediately when
     *                      there's no connection to make.
     */
    public void setWorkerTimeout(int workerTimeout) {
        if (workerTimeout < 0) {
            throw new IllegalArgumentException("Must be >= 0");
        }
        this.workerTimeout = workerTimeout;
    }

    public ConnectFuture connect(SocketAddress address, IoHandler handler,
            IoServiceConfig config) {
        return connect(address, null, handler, config);
    }

    public ConnectFuture connect(SocketAddress address,
            SocketAddress localAddress, IoHandler handler,
            IoServiceConfig config) {
        if (address == null) {
            throw new NullPointerException("address");
        }
        if (handler == null) {
            throw new NullPointerException("handler");
        }

        if (!(address instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unexpected address type: "
                    + address.getClass());
        }

        if (localAddress != null
                && !(localAddress instanceof InetSocketAddress)) {
            throw new IllegalArgumentException(
                    "Unexpected local address type: " + localAddress.getClass());
        }

        if (config == null) {
            config = getDefaultConfig();
        }

        SocketChannel ch = null;
        boolean success = false;
        try {
            ch = SocketChannel.open();
            ch.socket().setReuseAddress(true);

            // Receive buffer size must be set BEFORE the socket is connected
            // in order for the TCP window to be sized accordingly
            if (config instanceof SocketConnectorConfig) {
                int receiveBufferSize =
                    ((SocketSessionConfig) config.getSessionConfig()).getReceiveBufferSize();
                if (receiveBufferSize > 65535) {
                    ch.socket().setReceiveBufferSize(receiveBufferSize);
                }
            }

            if (localAddress != null) {
                ch.socket().bind(localAddress);
            }

            ch.configureBlocking(false);

            if (ch.connect(address)) {
                DefaultConnectFuture future = new DefaultConnectFuture();
                newSession(ch, handler, config, future);
                success = true;
                return future;
            }

            success = true;
        } catch (IOException e) {
            return DefaultConnectFuture.newFailedFuture(e);
        } finally {
            if (!success && ch != null) {
                try {
                    ch.close();
                } catch (IOException e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        }

        ConnectionRequest request = new ConnectionRequest(ch, handler, config);
        synchronized (lock) {
            try {
                startupWorker();
            } catch (IOException e) {
                try {
                    ch.close();
                } catch (IOException e2) {
                    ExceptionMonitor.getInstance().exceptionCaught(e2);
                }

                return DefaultConnectFuture.newFailedFuture(e);
            }

            connectQueue.add(request);
            selector.wakeup();
        }

        return request;
    }

    public SocketConnectorConfig getDefaultConfig() {
        return defaultConfig;
    }

    /**
     * Sets the config this connector will use by default.
     *
     * @param defaultConfig the default config.
     * @throws NullPointerException if the specified value is <code>null</code>.
     */
    public void setDefaultConfig(SocketConnectorConfig defaultConfig) {
        if (defaultConfig == null) {
            throw new NullPointerException("defaultConfig");
        }
        this.defaultConfig = defaultConfig;
    }

    private void startupWorker() throws IOException {
        synchronized (lock) {
            if (worker == null) {
                selector = Selector.open();
                worker = new Worker();
                executor.execute(new NamePreservingRunnable(worker, threadName));
            }
        }
    }

    private void registerNew() {
        if (connectQueue.isEmpty()) {
            return;
        }

        Selector selector = this.selector;
        for (;;) {
            ConnectionRequest req = connectQueue.poll();

            if (req == null) {
                break;
            }

            SocketChannel ch = req.channel;
            try {
                ch.register(selector, SelectionKey.OP_CONNECT, req);
            } catch (IOException e) {
                req.setException(e);
                try {
                    ch.close();
                } catch (IOException e2) {
                    ExceptionMonitor.getInstance().exceptionCaught(e2);
                }
            }
        }
    }

    private void processSessions(Set<SelectionKey> keys) {
        for (SelectionKey key : keys) {
            if (!key.isConnectable()) {
                continue;
            }

            SocketChannel ch = (SocketChannel) key.channel();
            ConnectionRequest entry = (ConnectionRequest) key.attachment();

            boolean success = false;
            try {
                if (ch.finishConnect()) {
                    key.cancel();
                    newSession(ch, entry.handler, entry.config, entry);
                }
                success = true;
            } catch (Throwable e) {
                entry.setException(e);
            } finally {
                if (!success) {
                    key.cancel();
                    try {
                        ch.close();
                    } catch (IOException e) {
                        ExceptionMonitor.getInstance().exceptionCaught(e);
                    }
                }
            }
        }

        keys.clear();
    }

    private void processTimedOutSessions(Set<SelectionKey> keys) {
        long currentTime = System.currentTimeMillis();

        for (SelectionKey key : keys) {
            if (!key.isValid()) {
                continue;
            }

            ConnectionRequest entry = (ConnectionRequest) key.attachment();

            if (currentTime >= entry.deadline) {
                entry.setException(new ConnectException());
                try {
                    key.channel().close();
                } catch (IOException e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                } finally {
                    key.cancel();
                }
            }
        }
    }

    private void newSession(SocketChannel ch, IoHandler handler,
            IoServiceConfig config, ConnectFuture connectFuture)
            throws IOException {
        SocketSessionImpl session = new SocketSessionImpl(this,
                nextProcessor(), getListeners(), config, ch, handler, ch
                        .socket().getRemoteSocketAddress());
        try {
            getFilterChainBuilder().buildFilterChain(session.getFilterChain());
            config.getFilterChainBuilder().buildFilterChain(
                    session.getFilterChain());
            config.getThreadModel().buildFilterChain(session.getFilterChain());
        } catch (Throwable e) {
            throw (IOException) new IOException("Failed to create a session.")
                    .initCause(e);
        }

        // Set the ConnectFuture of the specified session, which will be
        // removed and notified by AbstractIoFilterChain eventually.
        session.setAttribute(AbstractIoFilterChain.CONNECT_FUTURE,
                connectFuture);

        // Forward the remaining process to the SocketIoProcessor.
        session.getIoProcessor().addNew(session);
    }

    private SocketIoProcessor nextProcessor() {
        if (this.processorDistributor == Integer.MAX_VALUE) {
            this.processorDistributor = Integer.MAX_VALUE % this.processorCount;
        }

        return ioProcessors[processorDistributor++ % processorCount];
    }

    private class Worker implements Runnable {
        private long lastActive = System.currentTimeMillis();

        public void run() {
            Selector selector = SocketConnector.this.selector;
            for (;;) {
                try {
                    int nKeys = selector.select(1000);

                    registerNew();

                    if (nKeys > 0) {
                        processSessions(selector.selectedKeys());
                    }

                    processTimedOutSessions(selector.keys());

                    if (selector.keys().isEmpty()) {
                        if (System.currentTimeMillis() - lastActive > workerTimeout * 1000L) {
                            synchronized (lock) {
                                if (selector.keys().isEmpty()
                                        && connectQueue.isEmpty()) {
                                    worker = null;
                                    try {
                                        selector.close();
                                    } catch (IOException e) {
                                        ExceptionMonitor.getInstance()
                                                .exceptionCaught(e);
                                    } finally {
                                        SocketConnector.this.selector = null;
                                    }
                                    break;
                                }
                            }
                        }
                    } else {
                        lastActive = System.currentTimeMillis();
                    }
                } catch (IOException e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        ExceptionMonitor.getInstance().exceptionCaught(e1);
                    }
                }
            }
        }
    }

    private class ConnectionRequest extends DefaultConnectFuture {
        private final SocketChannel channel;

        private final long deadline;

        private final IoHandler handler;

        private final IoServiceConfig config;

        private ConnectionRequest(SocketChannel channel, IoHandler handler,
                IoServiceConfig config) {
            this.channel = channel;
            long timeout;
            if (config instanceof IoConnectorConfig) {
                timeout = ((IoConnectorConfig) config)
                        .getConnectTimeoutMillis();
            } else {
                timeout = ((IoConnectorConfig) getDefaultConfig())
                        .getConnectTimeoutMillis();
            }
            this.deadline = System.currentTimeMillis() + timeout;
            this.handler = handler;
            this.config = config;
        }
    }
}
