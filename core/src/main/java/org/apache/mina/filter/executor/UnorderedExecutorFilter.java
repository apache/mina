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

import java.util.concurrent.Executor;

import org.apache.mina.common.IoEventType;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoFilterEvent;

/**
 * A filter that forwards I/O events to {@link Executor} to enforce a certain
 * thread model while allowing the events per session to be processed
 * simultaneously.
 * You can apply various thread model by inserting this filter to the {@link IoFilterChain}.
 * <p>
 * Please note that this filter doesn't manage the life cycle of the underlying
 * {@link Executor}.  You have to destroy or stop it by yourself.
 * <p>
 * This filter does not maintain the order of events per session and thus
 * more than one event handler methods can be invoked at the same time with
 * mixed order.  For example, let's assume that messageReceived, messageSent,
 * and sessionClosed events are fired.
 * <ul>
 * <li>All event handler methods can be called simultaneously.
 *     (e.g. messageReceived and messageSent can be invoked at the same time.)</li>
 * <li>The event order can be mixed up.
 *     (e.g. sessionClosed or messageSent can be invoked before messageReceived
 *           is invoked.)</li>
 * </ul>
 * If you need to maintain the order of events per session, please use
 * {@link ExecutorFilter}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class UnorderedExecutorFilter extends AbstractExecutorFilter {
    /**
     * Creates a new instance with the default thread pool implementation
     * (<tt>new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new LinkedBlockingQueue() )</tt>).
     */
    public UnorderedExecutorFilter(IoEventType... eventTypes) {
        super(eventTypes);
    }

    /**
     * Creates a new instance with the specified <tt>executor</tt>.
     */
    public UnorderedExecutorFilter(Executor executor, IoEventType... eventTypes) {
        super(executor, eventTypes);
    }

    @Override
    protected void fireEvent(IoFilterEvent event) {
        getExecutor().execute(new ProcessEventRunnable(event));
    }

    private class ProcessEventRunnable implements Runnable {
        private final IoFilterEvent event;

        ProcessEventRunnable(IoFilterEvent event) {
            this.event = event;
        }

        public void run() {
            processEvent(event);
        }
    }
}
