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

import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * A {@link Queue} that stores the objects used for I/O operations.
 * <p>
 * {@link IoQueue} is different from an ordinary {@link Queue} in that it
 * provides a way to listen to the state of the queue, {@link IoQueueListener}.
 * This is often useful when you want to veto adding an element or to monitor
 * the insertion and removal of an element, which allows a user to enforce
 * various constraints to the queue dynamically in run time.  The following is
 * the list of possible use cases:
 * <ul>
 * <li>implementation of dynamic or static bound queue</li>
 * <li>throughput calculation</li>
 * <li>logging, ...</li>
 * </ul>
 *
 * @param <E> the type of the queue's elements
 *
 * @author The Apache MINA project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoQueue<E> extends Queue<E> {

    /**
     * Returns the element at the specified position.
     *
     * @throws NoSuchElementException if a wrong index is specified
     */
    E element(int index);

    /**
     * Adds the specified {@link IoQueueListener} to this queue.  Once added,
     * the listener will be notified whenever the state of this queue changes.
     */
    void addListener(IoQueueListener<? super E> listener);

    /**
     * Removes the specified {@link IoQueueListener} from this queue.  Once
     * removed, the listener will no longer be notified when the state of this
     * queue changes.
     */
    void removeListener(IoQueueListener<? super E> listener);
}
