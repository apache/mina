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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * A {@link ProtocolEncoderOutput} based on queue.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractProtocolEncoderOutput implements
        ProtocolEncoderOutput {
    private final Queue<Object> messageQueue = new ConcurrentLinkedQueue<Object>();

    private boolean buffersOnly = true;

    public AbstractProtocolEncoderOutput() {
        // Do nothing
    }

    public Queue<Object> getMessageQueue() {
        return messageQueue;
    }

    public void write(Object encodedMessage) {
        if (encodedMessage instanceof IoBuffer) {
            IoBuffer buf = (IoBuffer) encodedMessage;
            if (buf.hasRemaining()) {
                messageQueue.offer(buf);
            } else {
                throw new IllegalArgumentException(
                        "buf is empty. Forgot to call flip()?");
            }
        } else {
            messageQueue.offer(encodedMessage);
            buffersOnly = false;
        }
    }

    public void mergeAll() {
        if (!buffersOnly) {
            throw new IllegalStateException(
                    "the encoded message list contains a non-buffer.");
        }
        
        final int size = messageQueue.size();

        if (size < 2) {
            // no need to merge!
            return;
        }

        // Get the size of merged BB
        int sum = 0;
        for (Object b : messageQueue) {
            sum += ((IoBuffer) b).remaining();
        }

        // Allocate a new BB that will contain all fragments
        IoBuffer newBuf = IoBuffer.allocate(sum);

        // and merge all.
        for (; ;) {
            IoBuffer buf = (IoBuffer) messageQueue.poll();
            if (buf == null) {
                break;
            }

            newBuf.put(buf);
        }

        // Push the new buffer finally.
        newBuf.flip();
        messageQueue.add(newBuf);
    }
}