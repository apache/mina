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

import java.util.EventListener;

/**
 * Something interested in being notified when the completion
 * of an asynchronous I/O operation : {@link IoFuture}. 
 * 
 * @param <F> The Future type
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoFutureListener<F extends IoFuture> extends EventListener {
    /**
     * An {@link IoFutureListener} that closes the {@link IoSession} which is
     * associated with the specified {@link IoFuture}.
     */
    IoFutureListener<IoFuture> CLOSE = new IoFutureListener<IoFuture>() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void operationComplete(IoFuture future) {
            future.getSession().closeNow();
        }
    };

    /**
     * Invoked when the operation associated with the {@link IoFuture}
     * has been completed even if you add the listener after the completion.
     *
     * @param future  The source {@link IoFuture} which called this
     *                callback.
     */
    void operationComplete(F future);
}