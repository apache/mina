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


/**
 * A skeletal decorator which wraps an existing {@link IoQueue} instance.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IoQueueWrapper<E> extends QueueWrapper<E> {

    /**
     * Creates a new instance.
     */
    public IoQueueWrapper(IoQueue<E> queue) {
        super(queue);
    }

    public void addListener(IoQueueListener<? super E> listener) {
        ((IoQueue<E>) q).addListener(listener);
    }

    public void removeListener(IoQueueListener<? super E> listener) {
        ((IoQueue<E>) q).removeListener(listener);
    }
}