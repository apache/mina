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

/**
 * Represents the result of an ashynchronous I/O operation.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoFuture {
    /**
     * Returns the {@link IoSession} which is associated with this future.
     */
    IoSession getSession();

    /**
     * Returns the lock object this future acquires.
     */
    Object getLock();

    /**
     * Wait for the asynchronous operation to end.
     */
    void join();

    /**
     * Wait for the asynchronous operation to end with the specified timeout.
     *
     * @return <tt>true</tt> if the operation is finished.
     */
    boolean join(long timeoutInMillis);

    /**
     * Returns if the asynchronous operation is finished.
     */
    boolean isReady();

    /**
     * Adds an event <tt>listener</tt> which is notified when
     * the state of this future changes.
     */
    void addListener(IoFutureListener listener);

    /**
     * Removes an existing event <tt>listener</tt> which is notified when
     * the state of this future changes.
     */
    void removeListener(IoFutureListener listener);
}
