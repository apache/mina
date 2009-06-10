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

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.IoUtil;

/**
 * An {@link IoFuture} of {@link IoFuture}s.  It is useful when you want to
 * get notified when all {@link IoFuture}s are complete.  It is not recommended
 * to use {@link CompositeIoFuture} if you just want to wait for all futures.
 * In that case, please use {@link IoUtil#await(Iterable)} instead
 * for better performance.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 * @param <E> the type of the child futures.
 */
public class CompositeIoFuture<E extends IoFuture> extends DefaultIoFuture {
    
    private final NotifyingListener listener = new NotifyingListener();
    private final AtomicInteger unnotified = new AtomicInteger();
    private volatile boolean constructionFinished;
    
    public CompositeIoFuture(Iterable<E> children) {
        super(null);
        
        for (E f: children) {
            f.addListener(listener);
            unnotified.incrementAndGet();
        }
        
        constructionFinished = true;
        if (unnotified.get() == 0) {
            setValue(true);
        }
    }
    
    private class NotifyingListener implements IoFutureListener<IoFuture> {
        public void operationComplete(IoFuture future) {
            if (unnotified.decrementAndGet() == 0 && constructionFinished) {
                setValue(true);
            }
        }
    }
}
