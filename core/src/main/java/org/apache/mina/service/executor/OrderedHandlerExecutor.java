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
package org.apache.mina.service.executor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.mina.api.IoHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this executor if you want the {@link IoHandler} events of a session to be executed in order and on the same
 * thread. In your {@link IoHandler} code you don't need to care about session level concurrency.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public final class OrderedHandlerExecutor implements IoHandlerExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(OrderedHandlerExecutor.class);

    private Worker[] workers;

    /**
     * Create an {@link OrderedHandlerExecutor} with a given number of thread and a given queue size.
     * 
     * @param workerThreadCount the worker thread count
     * @param queueSize the size of the queue for each worker thread
     */
    public OrderedHandlerExecutor(int workerThreadCount, int queueSize) {
        LOG.debug("creating OrderedHandlerExecutor workerThreadCount = {} queueSize = {}", workerThreadCount, queueSize);
        workers = new Worker[workerThreadCount];

        for (int i = 0; i < workerThreadCount; i++) {
            workers[i] = new Worker(i, queueSize);
        }
        for (int i = 0; i < workerThreadCount; i++) {
            workers[i].start();
        }
        LOG.debug("workers started");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Event event) {
        try {
            int workerIndex = (int) (event.getSession().getId() % workers.length);
            LOG.debug("executing event {} in worker {}", event, workerIndex);
            workers[workerIndex].enqueue(event);
        } catch (InterruptedException e) {
            // interrupt the world
            return;
        }
    }

    /** thread in charge of gathering events from a queue and running them */
    private static class Worker extends Thread {

        private static HandlerCaller caller = new HandlerCaller();

        private final BlockingQueue<Event> queue;

        public Worker(int index, int queueSize) {
            super("IoHandlerWorker " + index);
            queue = new LinkedBlockingQueue<Event>(queueSize);
        }

        public void enqueue(Event event) throws InterruptedException {
            LOG.debug("enqueing event : {}", event);
            queue.put(event);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            for (;;) {
                try {

                    Event e = queue.take();
                    LOG.debug("dequeing event {}", e);
                    e.visit(caller);

                } catch (InterruptedException e) {
                    // end this thread
                    return;
                }
            }
        }
    }
}
