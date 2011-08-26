/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mina.api;

/**
 * A listener that asynchronously receives the result of a future computation.
 * The listeners will be called from an event pool and so any amount of
 * significant work should not be done in this listener.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoFutureListener<V> {

    /**
     * Called if there was an exception by the task as it was executing.
     * Expect {@link java.util.concurrent.CancellationException} of the
     * future was canceled or {@link java.util.concurrent.ExecutionException}
     * if there was an error executing the task the future was waiting on.
     *
     * @param t an instance of {@link Throwable}
     */
    void exception(Throwable t);

    /**
     * Called when the task has completed.  This method provides the result
     * returned by the task.
     *
     * @param result the result returned by the task.
     */
    void completed(V result);
}
