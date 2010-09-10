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
package org.apache.mina.filter.buffer;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.util.LazyInitializer;

/**
 * An {@link LazyInitializer} implementation that initializes an 
 * {@link IoBuffer} only when needed.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M2
 */
public class IoBufferLazyInitializer extends LazyInitializer<IoBuffer> {

    /**
     * The buffer size allocated for each new session's buffer.
     */
    private int bufferSize;

    /**
     * Constructor which sets allocated buffer size to <code>bufferSize</code>.
     * 
     * @param bufferSize the new buffer size
     */
    public IoBufferLazyInitializer(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * {@inheritDoc}
     */
    public IoBuffer init() {
        return IoBuffer.allocate(bufferSize);
    }
}