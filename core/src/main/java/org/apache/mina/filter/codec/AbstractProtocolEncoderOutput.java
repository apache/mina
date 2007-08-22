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
package org.apache.mina.filter.codec;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.mina.common.ByteBuffer;

/**
 * A {@link ProtocolEncoderOutput} based on queue.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractProtocolEncoderOutput implements
        ProtocolEncoderOutput {
    private final Queue<ByteBuffer> bufferQueue = new LinkedList<ByteBuffer>();

    public AbstractProtocolEncoderOutput() {
    }

    public Queue<ByteBuffer> getBufferQueue() {
        return bufferQueue;
    }

    public void write(ByteBuffer buf) {
        if (buf.hasRemaining()) {
            bufferQueue.offer(buf);
        } else {
            throw new IllegalArgumentException(
                    "buf is empty. Forgot to call flip()?");
        }
    }

    public void mergeAll() {
        int sum = 0;
        final int size = bufferQueue.size();

        if (size < 2) {
            // no need to merge!
            return;
        }

        // Get the size of merged BB
        for (ByteBuffer b : bufferQueue) {
            sum += b.remaining();
        }

        // Allocate a new BB that will contain all fragments
        ByteBuffer newBuf = ByteBuffer.allocate(sum);

        // and merge all.
        for (;;) {
            ByteBuffer buf = bufferQueue.poll();
            if (buf == null) {
                break;
            }

            newBuf.put(buf);
        }

        // Push the new buffer finally.
        newBuf.flip();
        bufferQueue.offer(newBuf);
    }
}