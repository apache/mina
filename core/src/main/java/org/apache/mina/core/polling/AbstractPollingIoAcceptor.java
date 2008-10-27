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
package org.apache.mina.core.polling;

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
import java.util.concurrent.Executors;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.AbstractIoAcceptor;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.SimpleIoProcessorPool;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.util.ExceptionMonitor;

/**
 * A base class for implementing transport using a polling strategy. The
 * underlying sockets will be checked in an active loop and woke up when an
 * socket needed to be processed. This class handle the logic behind binding,
 * accepting and disposing the server sockets. An {@link Executor} will be used
 * for running client accepting and an {@link AbstractPollingIoProcessor} will
 * be used for processing client I/O operations like reading, writing and
 * closing.
 * 
 * All the low level methods for binding, accepting, closing need to be provided
 * by the subclassing implementation.
 * 
 * @see NioSocketAcceptor for a example of implementation
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date: 2008-06-26 17:58:30 +0200 (jeu, 26 jun 2008)
 *          $
 */
public abstract class AbstractPollingIoAcceptor<T extends AbstractIoSession, H>
        extends AbstractIoAcceptor {

    private final IoProcessor<T> processor;

    private final boolean createdProcessor;

    private final Object lock = new Object();

    private final Queue<AcceptorOperationFuture> registerQueue = new ConcurrentLinkedQueue<AcceptorOperationFuture>();

    private final Queue<AcceptorOperationFuture> cancelQueue = new ConcurrentLinkedQueue<AcceptorOperationFuture>();

    private final Map<SocketAddress, H> boundHandles = Collections
            .synchronizedMap(new HashMap<SocketAddress, H>());

    private final ServiceOperationFuture disposalFuture = new ServiceOperationFuture();

    /** A flag set when the acceptor has been created and initialized */
    private volatile boolean selectable;

    /** The thread responsible of accepting incoming requests */ 
    private Acceptor acceptor;

    /**
     * Constructor for {@link AbstractPollingIoAcceptor}. You need to provide a default
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
    protected AbstractPollingIoAcceptor(IoSessionConfig sessionConfig,
            Class<? extends IoProcessor<T>> processorClass) {
        this(sessionConfig, null, new SimpleIoProcessorPool<T>(processorClass),
                true);
    }

    /**
     * Constructor for {@link AbstractPollingIoAcceptor}. You need to provide a default
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
    protected AbstractPollingIoAcceptor(IoSessionConfig sessionConfig,
            Class<? extends IoProcessor<T>> processorClass, int processorCount) {
        this(sessionConfig, null, new SimpleIoProcessorPool<T>(processorClass,
                processorCount), true);
    }

    /**
     * Constructor for {@link AbstractPollingIoAcceptor}. You need to provide a default
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
    protected AbstractPollingIoAcceptor(IoSessionConfig sessionConfig,
            IoProcessor<T> processor) {
        this(sessionConfig, null, processor, false);
    }

    /**
     * Constructor for {@link AbstractPollingIoAcceptor}. You need to provide a default
     * session configuration and an {@link Executor} for handling I/O events. If a
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
    protected AbstractPollingIoAcceptor(IoSessionConfig sessionConfig,
            Executor executor, IoProcessor<T> processor) {
        this(sessionConfig, executor, processor, false);
    }

    /**
     * Constructor for {@link AbstractPollingIoAcceptor}. You need to provide a default
     * session configuration and an {@link Executor} for handling I/O events. If a
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
     * @param processor the {@link IoProcessor} for processing the {@link IoSession} of 
     * this transport, triggering events to the bound {@link IoHandler} and processing 
     * the chains of {@link IoFilter}
     * @param createdProcessor tagging the processor as automatically created, so it 
     * will be automatically disposed 
     */
    private AbstractPollingIoAcceptor(IoSessionConfig sessionConfig,
            Executor executor, IoProcessor<T> processor,
            boolean createdProcessor) {
        super(sessionConfig, executor);

        if (processor == null) {
            throw new NullPointerException("processor");
        }

        this.processor = processor;
        this.createdProcessor = createdProcessor;

        try {
            // Initialize the selector
            init();
            
            // The selector is now ready, we can switch the
            // flag to true so that incoming connection can be accepted
            selectable = true;
        } catch (RuntimeException e) {
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
     * Destroy the polling system, will be called when this {@link IoAcceptor}
     * implementation will be disposed.  
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract void destroy() throws Exception;

    /**
     * Check for acceptable connections, interrupt when at least a server is ready for accepting.
     * All the ready server socket descriptors need to be returned by {@link #selectedHandles()}
     * @return The number of sockets having got incoming client
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract int select() throws Exception;

    /**
     * Interrupt the {@link #select()} method. Used when the poll set need to be modified.
     */
    protected abstract void wakeup();

    /**
     * {@link Iterator} for the set of server sockets found with acceptable incoming connections
     *  during the last {@link #select()} call.
     * @return the list of server handles ready
     */
    protected abstract Iterator<H> selectedHandles();

    /**
     * Open a server socket for a given local address.
     * @param localAddress the associated local address
     * @return the opened server socket
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract H open(SocketAddress localAddress) throws Exception;

    /**
     * Get the local address associated with a given server socket
     * @param handle the server socket
     * @return the local {@link SocketAddress} associated with this handle
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract SocketAddress localAddress(H handle) throws Exception;

    /**
     * Accept a client connection for a server socket and return a new {@link IoSession}
     * associated with the given {@link IoProcessor}
     * @param processor the {@link IoProcessor} to associate with the {@link IoSession}  
     * @param handle the server handle
     * @return the created {@link IoSession}
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract T accept(IoProcessor<T> processor, H handle)
            throws Exception;

    /**
     * Close a server socket.
     * @param handle the server socket
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract void close(H handle) throws Exception;

    /**
     * {@inheritDoc}
     */
    @Override
    protected IoFuture dispose0() throws Exception {
        unbind();
        if (!disposalFuture.isDone()) {
            startupAcceptor();
            wakeup();
        }
        return disposalFuture;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final Set<SocketAddress> bind0(
            List<? extends SocketAddress> localAddresses) throws Exception {
        AcceptorOperationFuture request = new AcceptorOperationFuture(
                localAddresses);

        // adds the Registration request to the queue for the Workers
        // to handle
        registerQueue.add(request);

        // creates the Acceptor instance and has the local
        // executor kick it off.
        startupAcceptor();
        wakeup();
        request.awaitUninterruptibly();

        if (request.getException() != null) {
            throw request.getException();
        }

        // Update the local addresses.
        // setLocalAddresses() shouldn't be called from the worker thread
        // because of deadlock.
        Set<SocketAddress> newLocalAddresses = new HashSet<SocketAddress>();
        for (H handle : boundHandles.values()) {
            newLocalAddresses.add(localAddress(handle));
        }

        return newLocalAddresses;
    }

    /**
     * This method is called by the doBind() and doUnbind()
     * methods.  If the acceptor is null, the acceptor object will
     * be created and kicked off by the executor.  If the acceptor
     * object is null, probably already created and this class
     * is now working, then nothing will happen and the method
     * will just return.
     */
    private void startupAcceptor() {
        // If the acceptor is not ready, clear the queues
        // TODO : they should already be clean : do we have to do that ?
        if (!selectable) {
            registerQueue.clear();
            cancelQueue.clear();
        }

        // start the acceptor if not already started
        synchronized (lock) {
            if (acceptor == null) {
                acceptor = new Acceptor();
                executeWorker(acceptor);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void unbind0(List<? extends SocketAddress> localAddresses)
            throws Exception {
        AcceptorOperationFuture future = new AcceptorOperationFuture(
                localAddresses);

        cancelQueue.add(future);
        startupAcceptor();
        wakeup();

        future.awaitUninterruptibly();
        if (future.getException() != null) {
            throw future.getException();
        }
    }

    /**
     * This class is called by the startupAcceptor() method and is
     * placed into a NamePreservingRunnable class.
     * It's a thread accepting incoming connections from clients.
     * The loop is stopped when all the bound handlers are unbound.
     */
    private class Acceptor implements Runnable {
        public void run() {
            int nHandles = 0;

            while (selectable) {
                try {
                    // Detect if we have some keys ready to be processed
                    // The select() will be woke up if some new connection
                    // have occurred, or if the selector has been explicitely
                    // woke up
                    int selected = select();

                    // this actually sets the selector to OP_ACCEPT,
                    // and binds to the port in which this class will
                    // listen on
                    nHandles += registerHandles();

                    if (selected > 0) {
                        processHandles(selectedHandles());
                    }

                    // check to see if any cancellation request has been made.
                    nHandles -= unregisterHandles();

                    if (nHandles == 0) {
                        synchronized (lock) {
                            if (registerQueue.isEmpty()
                                    && cancelQueue.isEmpty()) {
                                acceptor = null;
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

                finishSessionInitialization(session, null, null);

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
            AcceptorOperationFuture future = registerQueue.poll();
            if (future == null) {
                return 0;
            }

            Map<SocketAddress, H> newHandles = new HashMap<SocketAddress, H>();
            List<SocketAddress> localAddresses = future.getLocalAddresses();

            try {
                for (SocketAddress a : localAddresses) {
                    H handle = open(a);
                    newHandles.put(localAddress(handle), handle);
                }

                boundHandles.putAll(newHandles);

                // and notify.
                future.setDone();
                return newHandles.size();
            } catch (Exception e) {
                future.setException(e);
            } finally {
                // Roll back if failed to bind all addresses.
                if (future.getException() != null) {
                    for (H handle : newHandles.values()) {
                        try {
                            close(handle);
                        } catch (Exception e) {
                            ExceptionMonitor.getInstance().exceptionCaught(e);
                        }
                    }
                    wakeup();
                }
            }
        }
    }

    /**
     * This method just checks to see if anything has been placed into the
     * cancellation queue.  The only thing that should be in the cancelQueue
     * is CancellationRequest objects and the only place this happens is in
     * the doUnbind() method.
     */
    private int unregisterHandles() {
        int cancelledHandles = 0;
        for (;;) {
            AcceptorOperationFuture future = cancelQueue.poll();
            if (future == null) {
                break;
            }

            // close the channels
            for (SocketAddress a : future.getLocalAddresses()) {
                H handle = boundHandles.remove(a);
                if (handle == null) {
                    continue;
                }

                try {
                    close(handle);
                    wakeup(); // wake up again to trigger thread death
                } catch (Throwable e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                } finally {
                    cancelledHandles++;
                }
            }

            future.setDone();
        }

        return cancelledHandles;
    }

    /**
     * {@inheritDoc}
     */
    public final IoSession newSession(SocketAddress remoteAddress,
            SocketAddress localAddress) {
        throw new UnsupportedOperationException();
    }
}
