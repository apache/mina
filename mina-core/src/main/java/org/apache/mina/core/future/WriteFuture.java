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
package org.apache.mina.core.future;

/**
 * An {@link IoFuture} for asynchronous write requests.
 *
 * <h3>Example</h3>
 * <pre>
 * IoSession session = ...;
 * WriteFuture future = session.write(...);
 * 
 * // Wait until the message is completely written out to the O/S buffer.
 * future.awaitUninterruptibly();
 * 
 * if( future.isWritten() )
 * {
 *     // The message has been written successfully.
 * }
 * else
 * {
 *     // The message couldn't be written out completely for some reason.
 *     // (e.g. Connection is closed)
 * }
 * </pre>
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface WriteFuture extends IoFuture {
    /**
     * @return <tt>true</tt> if the write operation is finished successfully.
     */
    boolean isWritten();

    /**
     * @return the cause of the write failure if and only if the write
     * operation has failed due to an {@link Exception}.  Otherwise,
     * <tt>null</tt> is returned.
     */
    Throwable getException();

    /**
     * Sets the message is written, and notifies all threads waiting for
     * this future.  This method is invoked by MINA internally.  Please do
     * not call this method directly.
     */
    void setWritten();

    /**
     * Sets the cause of the write failure, and notifies all threads waiting
     * for this future.  This method is invoked by MINA internally.  Please
     * do not call this method directly.
     * 
     * @param cause The exception to store in the Future instance
     */
    void setException(Throwable cause);

    /**
     * Wait for the asynchronous operation to complete.
     * The attached listeners will be notified when the operation is 
     * completed.
     * 
     * @return the created {@link WriteFuture}
     * @throws InterruptedException If the wait is interrupted
     */
    @Override
    WriteFuture await() throws InterruptedException;

    /**
     * {@inheritDoc}
     */
    @Override
    WriteFuture awaitUninterruptibly();

    /**
     * {@inheritDoc}
     */
    @Override
    WriteFuture addListener(IoFutureListener<?> listener);

    /**
     * {@inheritDoc}
     */
    @Override
    WriteFuture removeListener(IoFutureListener<?> listener);
}
