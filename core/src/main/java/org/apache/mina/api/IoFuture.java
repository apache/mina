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

import java.util.concurrent.Future;

/**
 * A simple extension that allows listeners to register and receive results
 * asynchronously via registered listeners.  The listeners will be called from
 * an event pool and so any amount of significant work should not be done in
 * this listener.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoFuture<V> extends Future<V> {
    /**
     * Register a listener to asynchronously receive the results of the
     * future computation.
     *
     * @param listener the listener to asynchronously receive the results of the future computation
     * @return the instance of the future to allow "chaining" of registrations
     */
    IoFuture<V> register(IoFutureListener<V> listener);
}
