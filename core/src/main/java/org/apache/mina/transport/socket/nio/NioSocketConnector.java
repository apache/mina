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
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.common.AbstractIoConnector;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.DefaultConnectFuture;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoProcessor;
import org.apache.mina.common.RuntimeIoException;
import org.apache.mina.common.SimpleIoProcessorPool;
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

    private static final AtomicInteger id = new AtomicInteger();

    private final Object lock = new Object();
    private final String threadName;
    private final Executor executor;
    private final Selector selector;
    private final Queue<ConnectionRequest> connectQueue = new ConcurrentLinkedQueue<ConnectionRequest>();
    private final Queue<ConnectionRequest> cancelQueue = new ConcurrentLinkedQueue<ConnectionRequest>();
    private final IoProcessor<NioSession> processor;
    private final boolean createdProcessor;

    private Worker worker;

    public NioSocketConnector() {
        this(null, new SimpleIoProcessorPool<NioSession>(NioProcessor.class), true);
    }

    public NioSocketConnector(int processorCount) {
        this(null, new SimpleIoProcessorPool<NioSession>(NioProcessor.class, processorCount), true);
    }

    public NioSocketConnector(IoProcessor<NioSession> processor) {
        this(null, processor, false);
    }

    public NioSocketConnector(Executor executor, IoProcessor<NioSession> processor) {
        this(executor, processor, false);
    }

    private NioSocketConnector(Executor executor, IoProcessor<NioSession> processor, boolean createdProcessor) {
        super(new DefaultSocketSessionConfig());
        
        if (executor == null) {
            executor = new NewThreadExecutor();
        }
        if (processor == null) {
            throw new NullPointerException("processor");
        }
        
        this.executor = executor;
        this.threadName = getClass().getSimpleName() + '-' + id.incrementAndGet();
        this.processor = processor;
        this.createdProcessor = createdProcessor;

        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            disposeNow();
            throw new RuntimeIoException("Failed to open a selector.", e);
        }
    }
    
    @Override
    protected void doDispose() throws Exception {
        startupWorker();
    }

    private void disposeNow() {
        try {
            if (createdProcessor) {
                processor.dispose();
            }
        } finally {
            if (selector != null) {
                try {
                    selector.close();
                } catch (IOException e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        }
    }

    public TransportMetadata getTransportMetadata() {
        return NioSocketSession.METADATA;
    }

    @Override
    public SocketSessionConfig getSessionConfig() {
        return (SocketSessionConfig) super.getSessionConfig();
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
        if (!selector.isOpen()) {
            connectQueue.clear();
            cancelQueue.clear();
            throw new ClosedSelectorException();
        }
        synchronized (lock) {
            if (worker == null) {
                worker = new Worker();
                executor.execute(new NamePreservingRunnable(worker, threadName));
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
                try {
                    ch.close();
                } catch (IOException e2) {
                    ExceptionMonitor.getInstance().exceptionCaught(e2);
                }
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
        NioSocketSession session = new NioSocketSession(this, processor, ch);

        finishSessionInitialization(session, connectFuture);

        // Forward the remaining process to the SocketIoProcessor.
        session.getProcessor().add(session);
    }

    private class Worker implements Runnable {

        public void run() {
            for (;;) {
                try {
                    int nKeys = selector.select(1000);

                    registerNew();

                    if (nKeys > 0) {
                        processSessions(selector.selectedKeys());
                    }

                    processTimedOutSessions(selector.keys());

                    cancelKeys();

                    if (selector.keys().isEmpty() && isDisposed()) {
                        synchronized (lock) {
                            if (selector.keys().isEmpty() &&
                                connectQueue.isEmpty() &&
                                isDisposed()) {
                                worker = null;
                                break;
                            }
                        }
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
            
            if (isDisposed()) {
                disposeNow();
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
