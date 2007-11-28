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

import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
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
public abstract class AbstractPollingIoAcceptor<T extends AbstractIoSession, H>
        extends AbstractIoAcceptor {

    private static final AtomicInteger id = new AtomicInteger();

    private final Executor executor;
    private final boolean createdExecutor;
    private final String threadName;
    private final IoProcessor<T> processor;
    private final boolean createdProcessor;

    private final Object lock = new Object();

    private final Queue<ServiceOperationFuture> registerQueue =
        new ConcurrentLinkedQueue<ServiceOperationFuture>();
    private final Queue<ServiceOperationFuture> cancelQueue =
        new ConcurrentLinkedQueue<ServiceOperationFuture>();

    private final Map<SocketAddress, H> boundHandles =
        Collections.synchronizedMap(new HashMap<SocketAddress, H>());
    
    private final ServiceOperationFuture disposalFuture =
        new ServiceOperationFuture();
    private volatile boolean selectable;
    private Worker worker;

    /**
     * Create an acceptor with a single processing thread using a NewThreadExecutor
     */
    protected AbstractPollingIoAcceptor(IoSessionConfig sessionConfig, Class<? extends IoProcessor<T>> processorClass) {
        this(sessionConfig, null, new SimpleIoProcessorPool<T>(processorClass), true);
    }

    protected AbstractPollingIoAcceptor(IoSessionConfig sessionConfig, Class<? extends IoProcessor<T>> processorClass, int processorCount) {
        this(sessionConfig, null, new SimpleIoProcessorPool<T>(processorClass, processorCount), true);
    }
    
    protected AbstractPollingIoAcceptor(IoSessionConfig sessionConfig, IoProcessor<T> processor) {
        this(sessionConfig, null, processor, false);
    }

    protected AbstractPollingIoAcceptor(IoSessionConfig sessionConfig, Executor executor, IoProcessor<T> processor) {
        this(sessionConfig, executor, processor, false);
    }

    private AbstractPollingIoAcceptor(IoSessionConfig sessionConfig, Executor executor, IoProcessor<T> processor, boolean createdProcessor) {
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
    protected abstract boolean select() throws Exception;
    protected abstract void wakeup();
    protected abstract Iterator<H> selectedHandles();
    protected abstract H bind(SocketAddress localAddress) throws Exception;
    protected abstract SocketAddress localAddress(H handle) throws Exception;
    protected abstract T accept(IoProcessor<T> processor, H handle) throws Exception;
    protected abstract void unbind(H handle) throws Exception;

    @Override
    protected IoFuture dispose0() throws Exception {
        unbind();
        if (!disposalFuture.isDone()) {
            startupWorker();
            wakeup();
        }
        return disposalFuture;
    }

    @Override
    protected final void bind0() throws Exception {
        ServiceOperationFuture request = new ServiceOperationFuture();

        // adds the Registration request to the queue for the Workers
        // to handle
        registerQueue.add(request);

        // creates an instance of a Worker and has the local
        // executor kick it off.
        startupWorker();
        wakeup();
        request.awaitUninterruptibly();

        if (request.getException() != null) {
            throw request.getException();
        }

        // Update the local addresses.
        // setLocalAddresses() shouldn't be called from the worker thread
        // because of deadlock.
        Set<SocketAddress> newLocalAddresses = new HashSet<SocketAddress>();
        for (H handle: boundHandles.values()) {
            newLocalAddresses.add(localAddress(handle));
        }
        setLocalAddresses(newLocalAddresses);
    }

    /**
     * This method is called by the doBind() and doUnbind()
     * methods.  If the worker object is not null, presumably
     * the acceptor is starting up, then the worker object will
     * be created and kicked off by the executor.  If the worker
     * object is not null, probably already created and this class
     * is now working, then nothing will happen and the method
     * will just return.
     */
    private void startupWorker() {
        if (!selectable) {
            registerQueue.clear();
            cancelQueue.clear();
        }

        synchronized (lock) {
            if (worker == null) {
                worker = new Worker();
                executor.execute(new NamePreservingRunnable(worker, threadName));
            }
        }
    }

    @Override
    protected final void unbind0() throws Exception {
        ServiceOperationFuture future = new ServiceOperationFuture();

        cancelQueue.add(future);
        startupWorker();
        wakeup();

        future.awaitUninterruptibly();
        if (future.getException() != null) {
            throw future.getException();
        }
    }

    /**
     * This class is called by the startupWorker() method and is
     * placed into a NamePreservingRunnable class.
     */
    private class Worker implements Runnable {
        public void run() {
            int nHandles = 0;
            
            while (selectable) {
                try {
                    // gets the number of keys that are ready to go
                    boolean selected = select();

                    // this actually sets the selector to OP_ACCEPT,
                    // and binds to the port in which this class will
                    // listen on
                    nHandles += registerHandles();

                    if (selected) {
                        processHandles(selectedHandles());
                    }

                    // check to see if any cancellation request has been made.
                    nHandles -= unregisterHandles();

                    if (nHandles == 0) {
                        synchronized (lock) {
                            if (registerQueue.isEmpty() &&
                                cancelQueue.isEmpty()) {
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
                    if (createdExecutor) {
                        ((ExecutorService) executor).shutdown();
                    }
                    try {
                        destroy();
                    } catch (Exception e) {
                        ExceptionMonitor.getInstance().exceptionCaught(e);
                    } finally {
                        disposalFuture.setDone();
                    }
                }
            }
        }

        /**
         * This method will process new sessions for the Worker class.  All
         * keys that have had their status updates as per the Selector.selectedKeys()
         * method will be processed here.  Only keys that are ready to accept
         * connections are handled here.
         * <p/>
         * Session objects are created by making new instances of SocketSessionImpl
         * and passing the session object to the SocketIoProcessor class.
         */
        @SuppressWarnings("unchecked")
        private void processHandles(Iterator<H> handles) throws Exception {
            while (handles.hasNext()) {
                H handle = handles.next();
                handles.remove();

                T session = accept(processor, handle);
                if (session == null) {
                    break;
                }
                
                finishSessionInitialization(session, null);

                // add the session to the SocketIoProcessor
                session.getProcessor().add(session);
            }
        }
    }

    /**
     * Sets up the socket communications.  Sets items such as:
     * <p/>
     * Blocking
     * Reuse address
     * Receive buffer size
     * Bind to listen port
     * Registers OP_ACCEPT for selector
     */
    private int registerHandles() {
        for (;;) {
            ServiceOperationFuture future = registerQueue.poll();
            if (future == null) {
                break;
            }
            
            Map<SocketAddress, H> newHandles = new HashMap<SocketAddress, H>();
            List<SocketAddress> localAddresses = getLocalAddresses();
            
            try {
                for (SocketAddress a: localAddresses) {
                    H handle = bind(a);
                    newHandles.put(localAddress(handle), handle);
                }
                
                boundHandles.putAll(newHandles);

                // and notify.
                future.setDone();
                return boundHandles.size();
            } catch (Exception e) {
                future.setException(e);
            } finally {
                // Roll back if failed to bind all addresses.
                if (future.getException() != null) {
                    for (H handle: newHandles.values()) {
                        try {
                            unbind(handle);
                        } catch (Exception e) {
                            ExceptionMonitor.getInstance().exceptionCaught(e);
                        }
                    }
                    wakeup();
                }
            }
        }
        
        return 0;
    }

    /**
     * This method just checks to see if anything has been placed into the
     * cancellation queue.  The only thing that should be in the cancelQueue
     * is CancellationRequest objects and the only place this happens is in
     * the doUnbind() method.
     */
    private int unregisterHandles() {
        int cancelledHandles = 0;
        for (; ;) {
            ServiceOperationFuture future = cancelQueue.poll();
            if (future == null) {
                break;
            }

            // close the channels
            for (H handle: boundHandles.values()) {
                try {
                    unbind(handle);
                    wakeup(); // wake up again to trigger thread death
                } catch (Exception e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
                
                cancelledHandles ++;
            }
            
            boundHandles.clear();
            future.setDone();
        }
        
        return cancelledHandles;
    }

    public final IoSession newSession(SocketAddress remoteAddress, SocketAddress localAddress) {
        throw new UnsupportedOperationException();
    }
}
