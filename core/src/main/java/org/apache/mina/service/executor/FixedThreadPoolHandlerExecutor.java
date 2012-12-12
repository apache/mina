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
import java.util.concurrent.LinkedBlockingDeque;

import org.apache.mina.api.IoSession;

public class FixedThreadPoolHandlerExecutor implements HandlerExecutor {

    private Worker[] workers;

    public FixedThreadPoolHandlerExecutor(int workerThreadCount, int queueSize) {
        workers = new Worker[workerThreadCount];

        for (int i = 0; i < workerThreadCount; i++) {
            workers[i] = new Worker(queueSize);
        }
        for (int i = 0; i < workerThreadCount; i++) {
            workers[i].start();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Event event) {
        try {
            workers[(int) (event.getSession().getId() % workers.length)].enqueue(event);
        } catch (InterruptedException e) {
            // interrupt the world
            return;
        }
    }

    private static class Worker extends Thread {

        private static CallHandler caller = new CallHandler();

        private BlockingQueue<Event> queue;

        public Worker(int queueSize) {
            queue = new LinkedBlockingDeque<Event>(queueSize);
        }

        public void enqueue(Event event) throws InterruptedException {
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
                    e.visit(caller);

                } catch (InterruptedException e) {
                    // end this thread
                    return;
                }
            }
        }
    }

    private static class CallHandler implements EventVisitor {

        @Override
        public void visit(CloseEvent event) {
            IoSession session = event.getSession();
            try {
                session.getService().getIoHandler().sessionClosed(session);
            } catch (Exception e) {
                session.getService().getIoHandler().exceptionCaught(session, e);
            }
        }

        @Override
        public void visit(IdleEvent event) {
            IoSession session = event.getSession();
            try {
                session.getService().getIoHandler().sessionIdle(session, event.getIdleStatus());
            } catch (Exception e) {
                session.getService().getIoHandler().exceptionCaught(session, e);
            }

        }

        @Override
        public void visit(OpenEvent event) {
            IoSession session = event.getSession();
            try {
                session.getService().getIoHandler().sessionOpened(session);
            } catch (Exception e) {
                session.getService().getIoHandler().exceptionCaught(session, e);
            }

        }

        @Override
        public void visit(ReceiveEvent event) {
            IoSession session = event.getSession();
            try {
                session.getService().getIoHandler().messageReceived(session, event.getMessage());
            } catch (Exception e) {
                session.getService().getIoHandler().exceptionCaught(session, e);
            }
        }

        @Override
        public void visit(SentEvent event) {
            IoSession session = event.getSession();
            try {
                session.getService().getIoHandler().messageSent(session, event.getMessage());
            } catch (Exception e) {
                session.getService().getIoHandler().exceptionCaught(session, e);
            }
        }
    }
}
