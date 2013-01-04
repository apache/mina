/*
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
package org.apache.mina.transport.nio;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A fixed size pool of {@link SelectorLoop}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class FixedSelectorLoopPool implements SelectorLoopPool {
    /** the pool of selector loop */
    private final SelectorLoop[] pool;

    /** the index of the next selector loop to be served */
    private final AtomicInteger nextIndex = new AtomicInteger();

    /**
     * Create a pool of "size" {@link SelectorLoop}
     * 
     * @param size
     */
    public FixedSelectorLoopPool(String prefix, final int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("We can't create a pool with no Selectorloop in it");
        }

        pool = new SelectorLoop[size];

        for (int i = 0; i < size; i++) {
            pool[i] = new NioSelectorLoop(prefix + "-I/O", i);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SelectorLoop getSelectorLoop() {
        return pool[Math.abs(nextIndex.incrementAndGet() % pool.length)];
    }
}
