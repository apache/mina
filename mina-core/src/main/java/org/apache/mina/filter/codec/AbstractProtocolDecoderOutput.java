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

/**
 * A {@link ProtocolDecoderOutput} based on queue.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractProtocolDecoderOutput implements ProtocolDecoderOutput {
    private final Queue<Object> messageQueue = new ConcurrentLinkedQueue<Object>();

    public AbstractProtocolDecoderOutput() {
        // Do nothing
    }

    public Queue<Object> getMessageQueue() {
        return messageQueue;
    }

    public void write(Object message) {
        if (message == null) {
            throw new IllegalArgumentException("message");
        }

        messageQueue.add(message);
    }
}
