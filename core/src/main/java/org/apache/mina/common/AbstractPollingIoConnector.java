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
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * TODO Add documentation
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractPollingIoConnector<T extends AbstractIoSession, H>
        extends AbstractIoConnector {

    private final Object lock = new Object();
    private final Queue<ConnectionRequest> connectQueue = new ConcurrentLinkedQueue<ConnectionRequest>();
    private final Queue<ConnectionRequest> cancelQueue = new ConcurrentLinkedQueue<ConnectionRequest>();
    private final IoProcessor<T> processor;
    private final boolean createdProcessor;

    private final ServiceOperationFuture disposalFuture =
        new ServiceOperationFuture();
    private volatile boolean selectable;
    private Worker worker;

    /**
     * Constructor for {@link AbstractPollingIoConnector}. You need to provide a default
     * session configuration, a class of {@link IoProcessor} which will be instantiated in a
     * {@link SimpleIoProcessorPool} for better scaling in multiprocessor systems. The default
     * pool size will be used.
     * 
     * @see SimpleIoProcessorPool
     * 
     * @param sessionConfig
     *            the default configuration for the managed {@link IoSession}
     * @param processorClass a {@link Class} of {@link IoProcessor} for the associated {@link IoSession}
     *            type.
     */
    protected AbstractPollingIoConnector(IoSessionConfig sessionConfig, Class<? extends IoProcessor<T>> processorClass) {
        this(sessionConfig, null, new SimpleIoProcessorPool<T>(processorClass), true);
    }

    /**
     * Constructor for {@link AbstractPollingIoConnector}. You need to provide a default
     * session configuration, a class of {@link IoProcessor} which will be instantiated in a
     * {@link SimpleIoProcessorPool} for using multiple thread for better scaling in multiprocessor
     * systems.
     * 
     * @see SimpleIoProcessorPool
     * 
     * @param sessionConfig
     *            the default configuration for the managed {@link IoSession}
     * @param processorClass a {@link Class} of {@link IoProcessor} for the associated {@link IoSession}
     *            type.
     * @param processorCount the amount of processor to instantiate for the pool
     */
    protected AbstractPollingIoConnector(IoSessionConfig sessionConfig, Class<? extends IoProcessor<T>> processorClass, int processorCount) {
        this(sessionConfig, null, new SimpleIoProcessorPool<T>(processorClass, processorCount), true);
    }

    /**
     * Constructor for {@link AbstractPollingIoConnector}. You need to provide a default
     * session configuration, a default {@link Executor} will be created using
     * {@link Executors#newCachedThreadPool()}.
     * 
     * {@see AbstractIoService#AbstractIoService(IoSessionConfig, Executor)}
     * 
     * @param sessionConfig
     *            the default configuration for the managed {@link IoSession}
     * @param processor the {@link IoProcessor} for processing the {@link IoSession} of this transport, triggering 
     *            events to the bound {@link IoHandler} and processing the chains of {@link IoFilter} 
     */
    protected AbstractPollingIoConnector(IoSessionConfig sessionConfig, IoProcessor<T> processor) {
        this(sessionConfig, null, processor, false);
    }

    /**
     * Constructor for {@link AbstractPollingIoConnector}. You need to provide a default
     * session configuration and an {@link Executor} for handling I/O events. If
     * null {@link Executor} is provided, a default one will be created using
     * {@link Executors#newCachedThreadPool()}.
     * 
     * {@see AbstractIoService#AbstractIoService(IoSessionConfig, Executor)}
     * 
     * @param sessionConfig
     *            the default configuration for the managed {@link IoSession}
     * @param executor
     *            the {@link Executor} used for handling asynchronous execution of I/O
     *            events. Can be <code>null</code>.
     * @param processor the {@link IoProcessor} for processing the {@link IoSession} of this transport, triggering 
     *            events to the bound {@link IoHandler} and processing the chains of {@link IoFilter} 
     */
    protected AbstractPollingIoConnector(IoSessionConfig sessionConfig, Executor executor, IoProcessor<T> processor) {
        this(sessionConfig, executor, processor, false);
    }

    /**
     * Constructor for {@link AbstractPollingIoAcceptor}. You need to provide a default
     * session configuration and an {@link Executor} for handling I/O events. If
     * null {@link Executor} is provided, a default one will be created using
     * {@link Executors#newCachedThreadPool()}.
     * 
     * {@see AbstractIoService#AbstractIoService(IoSessionConfig, Executor)}
     * 
     * @param sessionConfig
     *            the default configuration for the managed {@link IoSession}
     * @param executor
     *            the {@link Executor} used for handling asynchronous execution of I/O
     *            events. Can be <code>null</code>.
     * @param processor the {@link IoProcessor} for processing the {@link IoSession} of this transport, triggering 
     *            events to the bound {@link IoHandler} and processing the chains of {@link IoFilter}
     * @param createdProcessor tagging the processor as automatically created, so it will be automatically disposed 
     */
    private AbstractPollingIoConnector(IoSessionConfig sessionConfig, Executor executor, IoProcessor<T> processor, boolean createdProcessor) {
        super(sessionConfig, executor);

        if (processor == null) {
            throw new NullPointerException("processor");
        }

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

    /**
     * Initialize the polling system, will be called at construction time.
     * @throws Exception any exception thrown by the underlying system calls  
     */
    protected abstract void init() throws Exception;

    /**
     * Destroy the polling system, will be called when this {@link IoConnector}
     * implementation will be disposed.  
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract void destroy() throws Exception;
    
    protected abstract H newHandle(SocketAddress localAddress) throws Exception;
    protected abstract boolean connect(H handle, SocketAddress remoteAddress) throws Exception;
    
    /**
     * Finish the connection process of a client socket after it was marked as ready to process
     * by the {@link #select(int)} call. The socket will be connected or reported as connection
     * failed.
     * @param handle the client socket handle to finsh to connect
     * @return true if the socket is connected
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract boolean finishConnect(H handle) throws Exception;
    protected abstract T newSession(IoProcessor<T> processor, H handle) throws Exception;

    /**
     * Close a client socket.
     * @param handle the client socket
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract void close(H handle) throws Exception;
    
    /**
     * Interrupt the {@link #select()} method. Used when the poll set need to be modified.
     */
    protected abstract void wakeup();
    
    /**
     * Check for connected sockets, interrupt when at least a connection is processed (connected or
     * failed to connect). All the client socket descriptors processed need to be returned by 
     * {@link #selectedHandles()}
     * @return true if one server socket have got incoming client
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract boolean select(int timeout) throws Exception;
    
    /**
     * {@link Iterator} for the set of client sockets found connected or 
     * failed to connect during the last {@link #select()} call.
     * @return the list of client socket handles to process
     */
    protected abstract Iterator<H> selectedHandles();
    
    protected abstract Iterator<H> allHandles();
    
    protected abstract void register(H handle, ConnectionRequest request) throws Exception;
    
    protected abstract ConnectionRequest connectionRequest(H handle);

    /**
     * {@inheritDoc}
     */
    @Override
    protected final IoFuture dispose0() throws Exception {
        if (!disposalFuture.isDone()) {
            startupWorker();
            wakeup();
        }
        return disposalFuture;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    protected final ConnectFuture connect0(
            SocketAddress remoteAddress, SocketAddress localAddress,
            IoSessionInitializer<? extends ConnectFuture> sessionInitializer) {
        H handle = null;
        boolean success = false;
        try {
            handle = newHandle(localAddress);
            if (connect(handle, remoteAddress)) {
                ConnectFuture future = new DefaultConnectFuture();
                T session = newSession(processor, handle);
                finishSessionInitialization(session, future, sessionInitializer);
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

        ConnectionRequest request = new ConnectionRequest(handle, sessionInitializer);
        connectQueue.add(request);
        startupWorker();
        wakeup();

        return request;
    }

    private void startupWorker() {
        if (!selectable) {
            connectQueue.clear();
            cancelQueue.clear();
        }

        synchronized (lock) {
            if (worker == null) {
                worker = new Worker();
                executeWorker(worker);
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
            } finally {
                nHandles ++;
            }
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
                if (finishConnect(handle)) {
                    T session = newSession(processor, handle);
                    finishSessionInitialization(session, entry, entry.getSessionInitializer());
                    // Forward the remaining process to the IoProcessor.
                    session.getProcessor().add(session);
                    nHandles ++;
                }
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
                entry.setException(
                        new ConnectException("Connection timed out."));
                cancelQueue.offer(entry);
            }
        }
    }

    private class Worker implements Runnable {

        public void run() {
            int nHandles = 0;
            while (selectable) {
                try {
                    // the timeout for select shall be smaller of the connect
                    // timeout or 1 second...
                    int timeout = (int)Math.min(getConnectTimeoutMillis(), 1000L);
                    boolean selected = select(timeout);

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

            if (selectable && isDisposing()) {
                selectable = false;
                try {
                    if (createdProcessor) {
                        processor.dispose();
                    }
                } finally {
                    try {
                        synchronized (disposalLock) {
                            if (isDisposing()) {
                                destroy();
                            }
                        }
                    } catch (Exception e) {
                        ExceptionMonitor.getInstance().exceptionCaught(e);
                    } finally {
                        disposalFuture.setDone();
                    }
                }
            }
        }
    }

    public final class ConnectionRequest extends DefaultConnectFuture {
        private final H handle;
        private final long deadline;
        private final IoSessionInitializer<? extends ConnectFuture> sessionInitializer;

        public ConnectionRequest(H handle, IoSessionInitializer<? extends ConnectFuture> callback) {
            this.handle = handle;
            long timeout = getConnectTimeoutMillis();
            if (timeout <= 0L) {
                this.deadline = Long.MAX_VALUE;
            } else {
                this.deadline = System.currentTimeMillis() + timeout;
            }
            this.sessionInitializer = callback;
        }

        public H getHandle() {
            return handle;
        }

        public long getDeadline() {
            return deadline;
        }

        public IoSessionInitializer<? extends ConnectFuture> getSessionInitializer() {
            return sessionInitializer;
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
