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
package org.apache.mina.filter.executor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.session.IoEvent;

/**
 * A {@link ThreadPoolExecutor} that does not maintain the order of {@link IoEvent}s.
 * This means more than one event handler methods can be invoked at the same
 * time with mixed order.  For example, let's assume that messageReceived, messageSent,
 * and sessionClosed events are fired.
 * <ul>
 * <li>All event handler methods can be called simultaneously.
 *     (e.g. messageReceived and messageSent can be invoked at the same time.)</li>
 * <li>The event order can be mixed up.
 *     (e.g. sessionClosed or messageSent can be invoked before messageReceived
 *           is invoked.)</li>
 * </ul>
 * If you need to maintain the order of events per session, please use
 * {@link OrderedThreadPoolExecutor}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @org.apache.xbean.XBean
 */
public class UnorderedThreadPoolExecutor extends ThreadPoolExecutor {

    private static final Runnable EXIT_SIGNAL = new Runnable() {
        public void run() {
            throw new Error(
                    "This method shouldn't be called. " +
                    "Please file a bug report.");
        }
    };

    private final Set<Worker> workers = new HashSet<Worker>();

    private volatile int corePoolSize;
    private volatile int maximumPoolSize;
    private volatile int largestPoolSize;
    private final AtomicInteger idleWorkers = new AtomicInteger();

    private long completedTaskCount;
    private volatile boolean shutdown;

    private final IoEventQueueHandler queueHandler;

    public UnorderedThreadPoolExecutor() {
        this(16);
    }

    public UnorderedThreadPoolExecutor(int maximumPoolSize) {
        this(0, maximumPoolSize);
    }

    public UnorderedThreadPoolExecutor(int corePoolSize, int maximumPoolSize) {
        this(corePoolSize, maximumPoolSize, 30, TimeUnit.SECONDS);
    }

    public UnorderedThreadPoolExecutor(
            int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, Executors.defaultThreadFactory());
    }

    public UnorderedThreadPoolExecutor(
            int corePoolSize, int maximumPoolSize,
            long keepAliveTime, TimeUnit unit,
            IoEventQueueHandler queueHandler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, Executors.defaultThreadFactory(), queueHandler);
    }

    public UnorderedThreadPoolExecutor(
            int corePoolSize, int maximumPoolSize,
            long keepAliveTime, TimeUnit unit,
            ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, threadFactory, null);
    }

    public UnorderedThreadPoolExecutor(
            int corePoolSize, int maximumPoolSize,
            long keepAliveTime, TimeUnit unit,
            ThreadFactory threadFactory, IoEventQueueHandler queueHandler) {
        super(0, 1, keepAliveTime, unit, new LinkedBlockingQueue<Runnable>(), threadFactory, new AbortPolicy());
        if (corePoolSize < 0) {
            throw new IllegalArgumentException("corePoolSize: " + corePoolSize);
        }

        if (maximumPoolSize == 0 || maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException("maximumPoolSize: " + maximumPoolSize);
        }

        if (queueHandler == null) {
            queueHandler = IoEventQueueHandler.NOOP;
        }

        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.queueHandler = queueHandler;
    }

    public IoEventQueueHandler getQueueHandler() {
        return queueHandler;
    }

    @Override
    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
        // Ignore the request.  It must always be AbortPolicy.
    }

    private void addWorker() {
        synchronized (workers) {
            if (workers.size() >= maximumPoolSize) {
                return;
            }

            Worker worker = new Worker();
            Thread thread = getThreadFactory().newThread(worker);
            idleWorkers.incrementAndGet();
            thread.start();
            workers.add(worker);

            if (workers.size() > largestPoolSize) {
                largestPoolSize = workers.size();
            }
        }
    }

    private void addWorkerIfNecessary() {
        if (idleWorkers.get() == 0) {
            synchronized (workers) {
                if (workers.isEmpty() || idleWorkers.get() == 0) {
                    addWorker();
                }
            }
        }
    }

    private void removeWorker() {
        synchronized (workers) {
            if (workers.size() <= corePoolSize) {
                return;
            }
            getQueue().offer(EXIT_SIGNAL);
        }
    }

    @Override
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    @Override
    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0 || maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException("maximumPoolSize: "
                    + maximumPoolSize);
        }

        synchronized (workers) {
            this.maximumPoolSize = maximumPoolSize;
            int difference = workers.size() - maximumPoolSize;
            while (difference > 0) {
                removeWorker();
                --difference;
            }
        }
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {

        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);

        synchronized (workers) {
            while (!isTerminated()) {
                long waitTime = deadline - System.currentTimeMillis();
                if (waitTime <= 0) {
                    break;
                }

                workers.wait(waitTime);
            }
        }
        return isTerminated();
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public boolean isTerminated() {
        if (!shutdown) {
            return false;
        }

        synchronized (workers) {
            return workers.isEmpty();
        }
    }

    @Override
    public void shutdown() {
        if (shutdown) {
            return;
        }

        shutdown = true;

        synchronized (workers) {
            for (int i = workers.size(); i > 0; i --) {
                getQueue().offer(EXIT_SIGNAL);
            }
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown();

        List<Runnable> answer = new ArrayList<Runnable>();
        Runnable task;
        while ((task = getQueue().poll()) != null) {
            if (task == EXIT_SIGNAL) {
                getQueue().offer(EXIT_SIGNAL);
                Thread.yield(); // Let others take the signal.
                continue;
            }

            getQueueHandler().polled(this, (IoEvent) task);
            answer.add(task);
        }

        return answer;
    }

    @Override
    public void execute(Runnable task) {
        if (shutdown) {
            rejectTask(task);
        }

        checkTaskType(task);

        IoEvent e = (IoEvent) task;
        boolean offeredEvent = queueHandler.accept(this, e);
        if (offeredEvent) {
            getQueue().offer(e);
        }

        addWorkerIfNecessary();

        if (offeredEvent) {
            queueHandler.offered(this, e);
        }
    }

    private void rejectTask(Runnable task) {
        getRejectedExecutionHandler().rejectedExecution(task, this);
    }

    private void checkTaskType(Runnable task) {
        if (!(task instanceof IoEvent)) {
            throw new IllegalArgumentException("task must be an IoEvent or its subclass.");
        }
    }

    @Override
    public int getActiveCount() {
        synchronized (workers) {
            return workers.size() - idleWorkers.get();
        }
    }

    @Override
    public long getCompletedTaskCount() {
        synchronized (workers) {
            long answer = completedTaskCount;
            for (Worker w: workers) {
                answer += w.completedTaskCount;
            }

            return answer;
        }
    }

    @Override
    public int getLargestPoolSize() {
        return largestPoolSize;
    }

    @Override
    public int getPoolSize() {
        synchronized (workers) {
            return workers.size();
        }
    }

    @Override
    public long getTaskCount() {
        return getCompletedTaskCount();
    }

    @Override
    public boolean isTerminating() {
        synchronized (workers) {
            return isShutdown() && !isTerminated();
        }
    }

    @Override
    public int prestartAllCoreThreads() {
        int answer = 0;
        synchronized (workers) {
            for (int i = corePoolSize - workers.size() ; i > 0; i --) {
                addWorker();
                answer ++;
            }
        }
        return answer;
    }

    @Override
    public boolean prestartCoreThread() {
        synchronized (workers) {
            if (workers.size() < corePoolSize) {
                addWorker();
                return true;
            }
            
            return false;
        }
    }

    @Override
    public void purge() {
        // Nothing to purge in this implementation.
    }

    @Override
    public boolean remove(Runnable task) {
        boolean removed = super.remove(task);
        if (removed) {
            getQueueHandler().polled(this, (IoEvent) task);
        }
        return removed;
    }

    @Override
    public int getCorePoolSize() {
        return corePoolSize;
    }

    @Override
    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 0) {
            throw new IllegalArgumentException("corePoolSize: " + corePoolSize);
        }
        if (corePoolSize > maximumPoolSize) {
            throw new IllegalArgumentException("corePoolSize exceeds maximumPoolSize");
        }

        synchronized (workers) {
            if (this.corePoolSize > corePoolSize) {
                for (int i = this.corePoolSize - corePoolSize; i > 0; i --) {
                    removeWorker();
                }
            }
            this.corePoolSize = corePoolSize;
        }
    }

    private class Worker implements Runnable {

        private volatile long completedTaskCount;
        private Thread thread;

        public void run() {
            thread = Thread.currentThread();

            try {
                for (;;) {
                    Runnable task = fetchTask();

                    idleWorkers.decrementAndGet();

                    if (task == null) {
                        synchronized (workers) {
                            if (workers.size() > corePoolSize) {
                                // Remove now to prevent duplicate exit.
                                workers.remove(this);
                                break;
                            }
                        }
                    }

                    if (task == EXIT_SIGNAL) {
                        break;
                    }

                    try {
                        if (task != null) {
                            queueHandler.polled(UnorderedThreadPoolExecutor.this, (IoEvent) task);
                            runTask(task);
                        }
                    } finally {
                        idleWorkers.incrementAndGet();
                    }
                }
            } finally {
                synchronized (workers) {
                    workers.remove(this);
                    UnorderedThreadPoolExecutor.this.completedTaskCount += completedTaskCount;
                    workers.notifyAll();
                }
            }
        }

        private Runnable fetchTask() {
            Runnable task = null;
            long currentTime = System.currentTimeMillis();
            long deadline = currentTime + getKeepAliveTime(TimeUnit.MILLISECONDS);
            for (;;) {
                try {
                    long waitTime = deadline - currentTime;
                    if (waitTime <= 0) {
                        break;
                    }

                    try {
                        task = getQueue().poll(waitTime, TimeUnit.MILLISECONDS);
                        break;
                    } finally {
                        if (task == null) {
                            currentTime = System.currentTimeMillis();
                        }
                    }
                } catch (InterruptedException e) {
                    // Ignore.
                    continue;
                }
            }
            return task;
        }

        private void runTask(Runnable task) {
            beforeExecute(thread, task);
            boolean ran = false;
            try {
                task.run();
                ran = true;
                afterExecute(task, null);
                completedTaskCount ++;
            } catch (RuntimeException e) {
                if (!ran) {
                    afterExecute(task, e);
                }
                throw e;
            }
        }
    }
}
