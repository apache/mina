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
package org.apache.mina.queue;

import java.util.EventListener;
import java.util.Queue;

/**
 * An {@link EventListener} which is notified when the state of the
 * {@link IoQueue} it is listening to changes.
 *
 * @param <E> the type of the element of the queue that this listener
 *            listens to
 *
 * @author The Apache MINA project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoQueueListener<E> extends EventListener {
    /**
     * Invoked before the specified element is added to the specified queue.
     * You can veto the insertion request by returning <tt>false</tt> or
     * throwing an exception.
     *
     * @return <tt>true</tt> if and only if the specified element is OK to be
     *         added to the specified queue.
     */
    boolean accept(IoQueue<? extends E> queue, E element) throws Exception;

    /**
     * Invoked right after the specified element is added to the specified
     * queue.  This method runs in the same thread which called
     * {@link Queue#offer(Object)}.
     */
    void offered(IoQueue<? extends E> queue, E element) throws Exception;

    /**
     * Invoked right after the specified element is removed from the specified
     * queue.  This method runs in the same thread which called
     * {@link Queue#poll()}.
     */
    void polled(IoQueue<? extends E> queue, E element) throws Exception;
}
