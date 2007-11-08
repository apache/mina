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
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.common.AttributeKey;
import org.apache.mina.common.DummySession;
import org.apache.mina.common.IoEvent;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.CircularQueue;

/**
 * A {@link ThreadPoolExecutor} that maintains the order of {@link IoEvent}s.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class OrderedThreadPoolExecutor extends ThreadPoolExecutor {

    private static final IoSession EXIT_SIGNAL = new DummySession();

    private final AttributeKey BUFFER = new AttributeKey(getClass(), "queue");
    private final BlockingQueue<IoSession> waitingSessionQueue = new LinkedBlockingQueue<IoSession>();
    
    private final Set<Worker> workers = new HashSet<Worker>();
    
    private volatile int corePoolSize;
    private volatile int maximumPoolSize;
    private volatile int largestPoolSize;
    private final AtomicInteger idleWorkers = new AtomicInteger();
    
    private long completedTaskCount;
    
    private volatile boolean shutdown;
    
    public OrderedThreadPoolExecutor(int maximumPoolSize) {
        this(0, maximumPoolSize);
    }
    
    public OrderedThreadPoolExecutor(int corePoolSize, int maximumPoolSize) {
        this(corePoolSize, maximumPoolSize, 30, TimeUnit.SECONDS);
    }
    
    public OrderedThreadPoolExecutor(
            int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, Executors.defaultThreadFactory());
    }
    
    public OrderedThreadPoolExecutor(
            int corePoolSize, int maximumPoolSize, 
            long keepAliveTime, TimeUnit unit,
            RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, Executors.defaultThreadFactory(), handler);
    }

    public OrderedThreadPoolExecutor(
            int corePoolSize, int maximumPoolSize, 
            long keepAliveTime, TimeUnit unit,
            ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, threadFactory, new AbortPolicy());
    }

    public OrderedThreadPoolExecutor(
            int corePoolSize, int maximumPoolSize, 
            long keepAliveTime, TimeUnit unit,
            ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(0, 1, keepAliveTime, unit, new SynchronousQueue<Runnable>(), threadFactory, handler);
        if (corePoolSize < 0) {
            throw new IllegalArgumentException("minThreads: " + corePoolSize);
        }
        
        if (maximumPoolSize == 0 || maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException("maxThreads: " + maximumPoolSize);
        }
        
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
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
            waitingSessionQueue.offer(EXIT_SIGNAL);
        }
    }
    
    @Override
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }
    
    @Override
    public void setMaximumPoolSize(int maximumPoolSize) {
        synchronized (workers) {
            if (this.maximumPoolSize > maximumPoolSize) {
                for (int i = this.maximumPoolSize - maximumPoolSize; i > 0; i --) {
                    removeWorker();
                }
            }
            this.maximumPoolSize = maximumPoolSize;
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
                waitingSessionQueue.offer(EXIT_SIGNAL);
            }
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown();
        
        List<Runnable> answer = new ArrayList<Runnable>();
        IoSession session;
        while ((session = waitingSessionQueue.poll()) != null) {
            if (session == EXIT_SIGNAL) {
                waitingSessionQueue.offer(EXIT_SIGNAL);
                Thread.yield(); // Let others take the signal.
                continue;
            }
            
            SessionBuffer buf = (SessionBuffer) session.getAttribute(BUFFER);
            synchronized (buf.queue) {
                answer.addAll(buf.queue);
                buf.queue.clear();
            }
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
        IoSession s = e.getSession();
        SessionBuffer buf = getSessionBuffer(s);
        Queue<Runnable> queue = buf.queue;
        boolean offer;
        synchronized (queue) {
            queue.offer(e);
            if (buf.processingCompleted) {
                buf.processingCompleted = false;
                offer = true;
            } else {
                offer = false;
            }
        }
        
        if (offer) {
            waitingSessionQueue.offer(s);
        }
        
        addWorkerIfNecessary();
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
            } else {
                return false;
            }
        }
    }

    @Override
    public void purge() {
    }

    @Override
    public boolean remove(Runnable task) {
        checkTaskType(task);
        IoEvent e = (IoEvent) task;
        IoSession s = e.getSession();
        SessionBuffer buffer = (SessionBuffer) s.getAttribute(BUFFER);
        if (buffer == null) {
            return false;
        }
        
        synchronized (buffer.queue) {
            return buffer.queue.remove(task);
        }
    }
    
    @Override
    public int getCorePoolSize() {
        return corePoolSize;
    }

    @Override
    public void setCorePoolSize(int corePoolSize) {
        synchronized (workers) {
            if (this.corePoolSize > corePoolSize) {
                for (int i = this.corePoolSize - corePoolSize; i > 0; i --) {
                    removeWorker();
                }
            }
            this.corePoolSize = corePoolSize;
        }
    }

    private SessionBuffer getSessionBuffer(IoSession session) {
        SessionBuffer buffer = (SessionBuffer) session.getAttribute(BUFFER);
        if (buffer == null) {
            buffer = new SessionBuffer();
            SessionBuffer oldBuffer = (SessionBuffer) session.setAttributeIfAbsent(BUFFER, buffer);
            if (oldBuffer != null) {
                buffer = oldBuffer;
            }
        }
        return buffer;
    }
    
    private static class SessionBuffer {
        private final Queue<Runnable> queue = new CircularQueue<Runnable>();
        private boolean processingCompleted = true;
    }
    
    private class Worker implements Runnable {
        
        private volatile long completedTaskCount;
        private Thread thread;
        
        public void run() {
            thread = Thread.currentThread();
            
            for (;;) {
                IoSession session = fetchSession();
                
                idleWorkers.decrementAndGet();
                
                if (session == null) {
                    synchronized (workers) {
                        if (workers.size() >= corePoolSize) {
                            // Remove now to prevent duplicate exit.
                            workers.remove(this);
                            break;
                        }
                    }
                }
                
                if (session == EXIT_SIGNAL) {
                    break;
                }
                
                try {
                    runTasks(getSessionBuffer(session));
                } finally {
                    idleWorkers.incrementAndGet();
                }
            }
            
            synchronized (workers) {
                workers.remove(this);
                OrderedThreadPoolExecutor.this.completedTaskCount += completedTaskCount;
                workers.notifyAll();
            }
        }

        private IoSession fetchSession() {
            IoSession session = null;
            long currentTime = System.currentTimeMillis();
            long deadline = currentTime + getKeepAliveTime(TimeUnit.MILLISECONDS);
            for (;;) {
                try {
                    long waitTime = deadline - currentTime;
                    if (waitTime <= 0) {
                        break;
                    }

                    try {
                        session = waitingSessionQueue.poll(waitTime, TimeUnit.MILLISECONDS);
                        break;
                    } finally {
                        if (session == null) {
                            currentTime = System.currentTimeMillis();
                        }
                    }
                } catch (InterruptedException e) {
                    // Ignore.
                    continue;
                }
            }
            return session;
        }

        private void runTasks(SessionBuffer buf) {
            for (;;) {
                Runnable task;
                synchronized (buf.queue) {
                    task = buf.queue.poll();
    
                    if (task == null) {
                        buf.processingCompleted = true;
                        break;
                    }
                }

                runTask(task);
            }
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
                if (!ran)
                    afterExecute(task, e);
                throw e;
            }
        }
    }
}
