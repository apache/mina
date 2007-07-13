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
package org.apache.mina.integration.spring;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Spring {@link FactoryBean} which enables the configuration of
 * {@link ThreadPoolExecutor} instances using Spring. Most of this code
 * has been copied from the <code>ThreadPoolTaskExecutor</code> class
 * available in Spring 2.0.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ThreadPoolExecutorFactoryBean extends AbstractFactoryBean {
    private int corePoolSize = 1;

    private int maxPoolSize = Integer.MAX_VALUE;

    private int keepAliveSeconds = 60;

    private int queueCapacity = Integer.MAX_VALUE;

    private ThreadFactory threadFactory = Executors.defaultThreadFactory();

    private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

    /**
     * Set the ThreadPoolExecutor's core pool size. Default is 1.
     */
    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    /**
     * Set the ThreadPoolExecutor's maximum pool size. Default is
     * <code>Integer.MAX_VALUE</code>.
     */
    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    /**
     * Set the ThreadPoolExecutor's keep alive seconds. Default is 60.
     */
    public void setKeepAliveSeconds(int keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }

    /**
     * Set the capacity for the ThreadPoolExecutor's BlockingQueue. Default is
     * <code>Integer.MAX_VALUE</code>.
     * <p>
     * Any positive value will lead to a LinkedBlockingQueue instance; any other
     * value will lead to a SynchronousQueue instance.
     *
     * @see LinkedBlockingQueue
     * @see SynchronousQueue
     */
    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    /**
     * Set the ThreadFactory to use for the ThreadPoolExecutor's thread pool.
     * Default is the ThreadPoolExecutor's default thread factory.
     *
     * @see Executors#defaultThreadFactory()
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = (threadFactory != null ? threadFactory : Executors
                .defaultThreadFactory());
    }

    /**
     * Set the RejectedExecutionHandler to use for the ThreadPoolExecutor.
     * Default is the ThreadPoolExecutor's default abort policy.
     *
     * @see ThreadPoolExecutor.AbortPolicy
     */
    public void setRejectedExecutionHandler(
            RejectedExecutionHandler rejectedExecutionHandler) {
        this.rejectedExecutionHandler = (rejectedExecutionHandler != null ? rejectedExecutionHandler
                : new ThreadPoolExecutor.AbortPolicy());
    }

    protected Object createInstance() throws Exception {
        BlockingQueue<Runnable> queue = null;
        if (queueCapacity > 0) {
            queue = new LinkedBlockingQueue<Runnable>(queueCapacity);
        } else {
            queue = new SynchronousQueue<Runnable>();
        }
        return new ThreadPoolExecutor(corePoolSize, maxPoolSize,
                keepAliveSeconds, TimeUnit.SECONDS, queue, threadFactory,
                rejectedExecutionHandler);
    }

    protected void destroyInstance(Object o) throws Exception {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) o;
        executor.shutdown();
    }

    public Class getObjectType() {
        return ThreadPoolExecutor.class;
    }

}
