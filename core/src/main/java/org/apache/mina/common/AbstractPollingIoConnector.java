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

import java.net.ConnectException;
import java.net.SocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.util.NamePreservingRunnable;

/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractPollingIoConnector<T extends AbstractIoSession, H>
        extends AbstractIoConnector {

    private static final AtomicInteger id = new AtomicInteger();

    private final Object lock = new Object();
    private final String threadName;
    private final Executor executor;
    private final boolean createdExecutor;
    private final Queue<ConnectionRequest> connectQueue = new ConcurrentLinkedQueue<ConnectionRequest>();
    private final Queue<ConnectionRequest> cancelQueue = new ConcurrentLinkedQueue<ConnectionRequest>();
    private final IoProcessor<T> processor;
    private final boolean createdProcessor;

    private final ServiceOperationFuture disposalFuture =
        new ServiceOperationFuture();
    private volatile boolean selectable;
    private Worker worker;

    protected AbstractPollingIoConnector(IoSessionConfig sessionConfig, Class<? extends IoProcessor<T>> processorClass) {
        this(sessionConfig, null, new SimpleIoProcessorPool<T>(processorClass), true);
    }

    protected AbstractPollingIoConnector(IoSessionConfig sessionConfig, Class<? extends IoProcessor<T>> processorClass, int processorCount) {
        this(sessionConfig, null, new SimpleIoProcessorPool<T>(processorClass, processorCount), true);
    }

    protected AbstractPollingIoConnector(IoSessionConfig sessionConfig, IoProcessor<T> processor) {
        this(sessionConfig, null, processor, false);
    }

    protected AbstractPollingIoConnector(IoSessionConfig sessionConfig, Executor executor, IoProcessor<T> processor) {
        this(sessionConfig, executor, processor, false);
    }

    private AbstractPollingIoConnector(IoSessionConfig sessionConfig, Executor executor, IoProcessor<T> processor, boolean createdProcessor) {
        super(sessionConfig);
        
        if (processor == null) {
            throw new NullPointerException("processor");
        }
        
        if (executor == null) {
            this.executor = new ThreadPoolExecutor(
                    1, 1, 1L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>());
            this.createdExecutor = true;
        } else {
            this.executor = executor;
            this.createdExecutor = false;
        }

        this.threadName = getClass().getSimpleName() + '-' + id.incrementAndGet();
        this.processor = processor;
        this.createdProcessor = createdProcessor;

        try {
            init();
            selectable = true;
        } catch (RuntimeException e){
            throw e;
        } catch (Exception e) {
            throw new RuntimeIoException("Failed to initialize.", e);
        } finally {
            if (!selectable) {
                try {
                    destroy();
                } catch (Exception e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        }
    }

    protected abstract void init() throws Exception;
    protected abstract void destroy() throws Exception;
    protected abstract H newHandle(SocketAddress localAddress) throws Exception;
    protected abstract boolean connect(H handle, SocketAddress remoteAddress) throws Exception;
    protected abstract void finishConnect(H handle) throws Exception;
    protected abstract T newSession(IoProcessor<T> processor, H handle) throws Exception;
    protected abstract void close(H handle) throws Exception;
    protected abstract void wakeup();
    protected abstract boolean select(int timeout) throws Exception;
    protected abstract Iterator<H> selectedHandles();
    protected abstract Iterator<H> allHandles();
    protected abstract void register(H handle, ConnectionRequest request) throws Exception;
    protected abstract ConnectionRequest connectionRequest(H handle);

    @Override
    protected final IoFuture dispose0() throws Exception {
        startupWorker();
        wakeup();
        return disposalFuture;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final ConnectFuture doConnect(SocketAddress remoteAddress,
                                      SocketAddress localAddress) {
        H handle = null;
        boolean success = false;
        try {
            handle = newHandle(localAddress);
            if (connect(handle, remoteAddress)) {
                ConnectFuture future = new DefaultConnectFuture();
                T session = newSession(processor, handle);
                finishSessionInitialization(session, future);
                // Forward the remaining process to the IoProcessor.
                session.getProcessor().add(session);
                success = true;
                return future;
            }

            success = true;
        } catch (Exception e) {
            return DefaultConnectFuture.newFailedFuture(e);
        } finally {
            if (!success && handle != null) {
                try {
                    close(handle);
                } catch (Exception e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        }

        ConnectionRequest request = new ConnectionRequest(handle);
        connectQueue.add(request);
        startupWorker();
        wakeup();

        return request;
    }

    private void startupWorker() {
        if (!selectable) {
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

    private int registerNew() {
        int nHandles = 0;
        for (; ;) {
            ConnectionRequest req = connectQueue.poll();
            if (req == null) {
                break;
            }

            H handle = req.handle;
            try {
                register(handle, req);
                nHandles ++;
            } catch (Exception e) {
                req.setException(e);
                try {
                    close(handle);
                } catch (Exception e2) {
                    ExceptionMonitor.getInstance().exceptionCaught(e2);
                }
            }
        }
        return nHandles;
    }

    private int cancelKeys() {
        int nHandles = 0;
        for (; ;) {
            ConnectionRequest req = cancelQueue.poll();
            if (req == null) {
                break;
            }

            H handle = req.handle;

            try {
                close(handle);
            } catch (Exception e) {
                ExceptionMonitor.getInstance().exceptionCaught(e);
            }
            
            nHandles ++;
        }
        return nHandles;
    }

    @SuppressWarnings("unchecked")
    private int processSessions(Iterator<H> handlers) {
        int nHandles = 0;
        while (handlers.hasNext()) {
            H handle = handlers.next();
            handlers.remove();

            ConnectionRequest entry = connectionRequest(handle);
            boolean success = false;
            try {
                finishConnect(handle);
                T session = newSession(processor, handle);
                finishSessionInitialization(session, entry);
                // Forward the remaining process to the IoProcessor.
                session.getProcessor().add(session);
                nHandles ++;
                success = true;
            } catch (Throwable e) {
                entry.setException(e);
            } finally {
                if (!success) {
                    cancelQueue.offer(entry);
                }
            }
        }
        return nHandles;
    }

    private void processTimedOutSessions(Iterator<H> handles) {
        long currentTime = System.currentTimeMillis();

        while (handles.hasNext()) {
            H handle = handles.next();
            ConnectionRequest entry = connectionRequest(handle);

            if (currentTime >= entry.deadline) {
                entry.setException(new ConnectException());
                cancelQueue.offer(entry);
            }
        }
    }

    private class Worker implements Runnable {

        public void run() {
            int nHandles = 0;
            while (selectable) {
                try {
                    boolean selected = select(1000);

                    nHandles += registerNew();

                    if (selected) {
                        nHandles -= processSessions(selectedHandles());
                    }

                    processTimedOutSessions(allHandles());

                    nHandles -= cancelKeys();

                    if (nHandles == 0) {
                        synchronized (lock) {
                            if (connectQueue.isEmpty()) {
                                worker = null;
                                break;
                            }
                        }
                    }
                } catch (Throwable e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        ExceptionMonitor.getInstance().exceptionCaught(e1);
                    }
                }
            }
            
            if (isDisposing()) {
                try {
                    if (createdProcessor) {
                        processor.dispose();
                    }
                } finally {
                    selectable = false;
                    if (createdExecutor) {
                        ((ExecutorService) executor).shutdown();
                    }
                    try {
                        destroy();
                    } catch (Exception e) {
                        ExceptionMonitor.getInstance().exceptionCaught(e);
                    }
                    
                    disposalFuture.setDone();
                }
            }
        }
    }

    protected final class ConnectionRequest extends DefaultConnectFuture {
        private final H handle;
        private final long deadline;

        public ConnectionRequest(H handle) {
            this.handle = handle;
            this.deadline = System.currentTimeMillis()
                    + getConnectTimeoutMillis();
        }

        public H getHandle() {
            return handle;
        }

        public long getDeadline() {
            return deadline;
        }

        @Override
        public void cancel() {
            super.cancel();
            cancelQueue.add(this);
            startupWorker();
            wakeup();
        }
    }
}
