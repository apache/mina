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

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link IoProcessor} pool that distributes {@link IoSession}s into one or more
 * {@link IoProcessor}s. Most current transport implementations use this pool internally
 * to perform better in a multi-core environment, and therefore, you won't need to 
 * use this pool directly unless you are running multiple {@link IoService}s in the
 * same JVM.
 * <p>
 * If you are running multiple {@link IoService}s, you could want to share the pool
 * among all services.  To do so, you can create a new {@link SimpleIoProcessorPool}
 * instance by yourself and provide the pool as a constructor parameter when you
 * create the services.
 * <p>
 * This pool uses Java reflection API to create multiple {@link IoProcessor} instances.
 * It tries to instantiate the processor in the following order:
 * <ol>
 * <li>A public constructor with one {@link ExecutorService} parameter.</li>
 * <li>A public constructor with one {@link Executor} parameter.</li>
 * <li>A public default constructor</li>
 * </ol>
 * The following is an example for the NIO socket transport:
 * <pre><code>
 * // Create a shared pool.
 * SimpleIoProcessorPool&lt;NioSession&gt; pool = 
 *         new SimpleIoProcessorPool&lt;NioSession&gt;(NioProcessor.class, 16);
 * 
 * // Create two services that share the same pool.
 * SocketAcceptor acceptor = new NioSocketAcceptor(pool);
 * SocketConnector connector = new NioSocketConnector(pool);
 * 
 * ...
 * 
 * // Release related resources.
 * connector.dispose();
 * acceptor.dispose();
 * pool.dispose();
 * </code></pre>
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * 
 * @param <T> the type of the {@link IoSession} to be managed by the specified
 *            {@link IoProcessor}.
 */
public class SimpleIoProcessorPool<T extends AbstractIoSession> implements IoProcessor<T> {
    
    private static final int DEFAULT_SIZE = Runtime.getRuntime().availableProcessors() + 1;
    private static final AttributeKey PROCESSOR = new AttributeKey(SimpleIoProcessorPool.class, "processor");
    
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final IoProcessor<T>[] pool;
    private final AtomicInteger processorDistributor = new AtomicInteger();
    private final Executor executor;
    private final boolean createdExecutor;
    
    private final Object disposalLock = new Object();
    private volatile boolean disposing;
    private volatile boolean disposed;
    
    public SimpleIoProcessorPool(Class<? extends IoProcessor<T>> processorType) {
        this(processorType, null, DEFAULT_SIZE);
    }
    
    public SimpleIoProcessorPool(Class<? extends IoProcessor<T>> processorType, int size) {
        this(processorType, null, size);
    }

    public SimpleIoProcessorPool(Class<? extends IoProcessor<T>> processorType, Executor executor) {
        this(processorType, executor, DEFAULT_SIZE);
    }
    
    @SuppressWarnings("unchecked")
    public SimpleIoProcessorPool(Class<? extends IoProcessor<T>> processorType, Executor executor, int size) {
        if (processorType == null) {
            throw new NullPointerException("processorType");
        }
        if (size <= 0) {
            throw new IllegalArgumentException(
                    "size: " + size + " (expected: positive integer)");
        }
        
        if (executor == null) {
            this.executor = executor = Executors.newCachedThreadPool();
            this.createdExecutor = true;
        } else {
            this.executor = executor;
            this.createdExecutor = false;
        }
        
        pool = new IoProcessor[size];
        
        boolean success = false;
        try {
            for (int i = 0; i < pool.length; i ++) {
                IoProcessor<T> processor = null;
                
                // Try to create a new processor with a proper constructor.
                try {
                    try {
                        processor = processorType.getConstructor(ExecutorService.class).newInstance(executor);
                    } catch (NoSuchMethodException e) {
                        // To the next step...
                    }
                    
                    if (processor == null) {
                        try {
                            processor = processorType.getConstructor(Executor.class).newInstance(executor);
                        } catch (NoSuchMethodException e) {
                            // To the next step...
                        }
                    }
                    
                    if (processor == null) {
                        try {
                            processor = processorType.getConstructor().newInstance();
                        } catch (NoSuchMethodException e) {
                            // To the next step...
                        }
                    }
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeIoException(
                            "Failed to create a new instance of " + processorType.getName(), e);
                }
                
                // Raise an exception if no proper constructor is found.
                if (processor == null) {
                    throw new IllegalArgumentException(
                            String.valueOf(processorType) + " must have a public constructor " +
                            "with one " + ExecutorService.class.getSimpleName() + " parameter, " +
                            "a public constructor with one " + Executor.class.getSimpleName() + 
                            " parameter or a public default constructor.");
                }
                
                pool[i] = processor;
            }
            
            success = true;
        } finally {
            if (!success) {
                dispose();
            }
        }
    }
    
    public final void add(T session) {
        getProcessor(session).add(session);
    }

    public final void flush(T session) {
        getProcessor(session).flush(session);
    }

    public final void remove(T session) {
        getProcessor(session).remove(session);
    }

    public final void updateTrafficMask(T session) {
        getProcessor(session).updateTrafficMask(session);
    }
    
    public boolean isDisposed() {
        return disposed;
    }

    public boolean isDisposing() {
        return disposing;
    }

    public final void dispose() {
        if (disposed) {
            return;
        }

        synchronized (disposalLock) {
            if (!disposing) {
                disposing = true;
                for (int i = pool.length - 1; i >= 0; i --) {
                    if (pool[i] == null || pool[i].isDisposing()) {
                        continue;
                    }

                    try {
                        pool[i].dispose();
                    } catch (Exception e) {
                        logger.warn(
                                "Failed to dispose a " +
                                pool[i].getClass().getSimpleName() +
                                " at index " + i + ".", e);
                    } finally {
                        pool[i] = null;
                    }
                }
                
                if (createdExecutor) {
                    ((ExecutorService) executor).shutdown();
                }
            }
        }

        disposed = true;
    }
    
    @SuppressWarnings("unchecked")
    private IoProcessor<T> getProcessor(T session) {
        IoProcessor<T> p = (IoProcessor<T>) session.getAttribute(PROCESSOR);
        if (p == null) {
            p = nextProcessor();
            IoProcessor<T> oldp =
                (IoProcessor<T>) session.setAttributeIfAbsent(PROCESSOR, p);
            if (oldp != null) {
                p = oldp;
            }
        }
        
        return p;
    }

    private IoProcessor<T> nextProcessor() {
        checkDisposal();
        return pool[Math.abs(processorDistributor.getAndIncrement()) % pool.length];
    }

    private void checkDisposal() {
        if (disposed) {
            throw new IllegalStateException("A disposed processor cannot be accessed.");
        }
    }
}
