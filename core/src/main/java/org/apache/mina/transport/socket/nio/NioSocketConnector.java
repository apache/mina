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
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import org.apache.mina.common.AbstractIoConnector;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.DefaultConnectFuture;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.RuntimeIoException;
import org.apache.mina.common.TransportMetadata;
import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.util.NamePreservingRunnable;
import org.apache.mina.util.NewThreadExecutor;

/**
 * {@link IoConnector} for socket transport (TCP/IP).
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 389042 $, $Date: 2006-03-27 07:49:41Z $
 */
public class NioSocketConnector extends AbstractIoConnector implements SocketConnector {

    /**
     * @noinspection StaticNonFinalField
     */
    private static volatile int nextId = 0;

    private final Object lock = new Object();
    private final int id = nextId++;
    private final String threadName = "SocketConnector-" + id;
    private final Queue<ConnectionRequest> connectQueue = new ConcurrentLinkedQueue<ConnectionRequest>();
    private final Queue<ConnectionRequest> cancelQueue = new ConcurrentLinkedQueue<ConnectionRequest>();
    private final NioProcessor[] ioProcessors;
    private final int processorCount;
    private final Executor executor;
    private final Selector selector;

    private Worker worker;
    private int processorDistributor = 0;
    private int workerTimeout = 60; // 1 min.

    /**
     * Create a connector with a single processing thread using a NewThreadExecutor
     */
    public NioSocketConnector() {
        this(1, new NewThreadExecutor());
    }

    /**
     * Create a connector with the desired number of processing threads
     *
     * @param processorCount Number of processing threads
     * @param executor       Executor to use for launching threads
     */
    public NioSocketConnector(int processorCount, Executor executor) {
        super(new DefaultSocketSessionConfig());
        if (processorCount < 1) {
            throw new IllegalArgumentException(
                    "Must have at least one processor");
        }

        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeIoException("Failed to open a selector.", e);
        }

        this.executor = executor;
        this.processorCount = processorCount;
        ioProcessors = new NioProcessor[processorCount];

        for (int i = 0; i < processorCount; i++) {
            ioProcessors[i] = new NioProcessor(
                    "SocketConnectorIoProcessor-" + id + "." + i, executor);
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

    public TransportMetadata getTransportMetadata() {
        return NioSocketSession.METADATA;
    }

    @Override
    public SocketSessionConfig getSessionConfig() {
        return (SocketSessionConfig) super.getSessionConfig();
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

    public void close(){
        setWorkerTimeout(0);
        selector.wakeup();
    }

    @Override
    protected ConnectFuture doConnect(SocketAddress remoteAddress,
                                      SocketAddress localAddress) {
        SocketChannel ch = null;
        boolean success = false;
        try {
            ch = SocketChannel.open();
            ch.socket().setReuseAddress(true);
            if (localAddress != null) {
                ch.socket().bind(localAddress);
            }

            ch.configureBlocking(false);

            if (ch.connect(remoteAddress)) {
                ConnectFuture future = new DefaultConnectFuture();
                newSession(ch, future);
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

        ConnectionRequest request = new ConnectionRequest(ch);
        connectQueue.add(request);
        startupWorker();
        selector.wakeup();

        return request;
    }

    private void startupWorker() {
        synchronized (lock) {
            if (worker == null) {
                worker = new Worker();
                executor.execute(new NamePreservingRunnable(worker));
            }
        }
    }

    private void registerNew() {
        for (; ;) {
            ConnectionRequest req = connectQueue.poll();
            if (req == null) {
                break;
            }

            SocketChannel ch = req.channel;
            try {
                ch.register(selector, SelectionKey.OP_CONNECT, req);
            } catch (IOException e) {
                req.setException(e);
            }
        }
    }

    private void cancelKeys() {
        for (; ;) {
            ConnectionRequest req = cancelQueue.poll();
            if (req == null) {
                break;
            }

            SocketChannel ch = req.channel;
            SelectionKey key = ch.keyFor(selector);
            if (key == null) {
                continue;
            }
            
            key.cancel();
            
            try {
                ch.close();
            } catch (IOException e) {
                ExceptionMonitor.getInstance().exceptionCaught(e);
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
                ch.finishConnect();
                newSession(ch, entry);
                success = true;
            } catch (Throwable e) {
                entry.setException(e);
            } finally {
                key.cancel();
                if (!success) {
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

    private void newSession(SocketChannel ch, ConnectFuture connectFuture) {
        NioSocketSession session = new NioSocketSession(
                this, nextProcessor(), ch);
        
        finishSessionInitialization(session, connectFuture);
        
        // Forward the remaining process to the SocketIoProcessor.
        session.getProcessor().add(session);
    }

    private NioProcessor nextProcessor() {
        if (this.processorDistributor == Integer.MAX_VALUE) {
            this.processorDistributor = Integer.MAX_VALUE % this.processorCount;
        }

        return ioProcessors[processorDistributor++ % processorCount];
    }

    private class Worker implements Runnable {
        private long lastActive = System.currentTimeMillis();

        public void run() {
            Thread.currentThread().setName(NioSocketConnector.this.threadName);

            for (; ;) {
                try {
                    int nKeys = selector.select(1000);

                    registerNew();

                    if (nKeys > 0) {
                        processSessions(selector.selectedKeys());
                    }

                    processTimedOutSessions(selector.keys());
                    
                    cancelKeys();

                    if (selector.keys().isEmpty()) {
                        if (System.currentTimeMillis() - lastActive > workerTimeout * 1000L) {
                            synchronized (lock) {
                                if (selector.keys().isEmpty()
                                        && connectQueue.isEmpty()) {
                                    worker = null;
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

        private ConnectionRequest(SocketChannel channel) {
            this.channel = channel;
            this.deadline = System.currentTimeMillis()
                    + getConnectTimeoutMillis();
        }

        @Override
        public void cancel() {
            super.cancel();
            cancelQueue.add(this);
            startupWorker();
            selector.wakeup();
        }
    }
}
