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

import java.nio.channels.SelectableChannel;

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface SelectorLoop {
    /**
     * Register a channel on a Selector, for some events. We can register for OP_ACCEPT, OP_READ or OP_WRITE.
     * 
     * @param accept Registers for OP_ACCEPT events
     * @param connect Registers for OP_CONNECT events
     * @param read Registers for OP_READ events
     * @param write Registers for OP_WRITE events
     * @param listener The listener
     * @param channel
     * @param wakeup Tells if we should do a wakeup() on the selector
     */
    void register(boolean accept, boolean connect, boolean read, boolean write, SelectorListener listener,
            SelectableChannel channel, RegistrationCallback callback);

    void modifyRegistration(boolean accept, boolean read, boolean write, SelectorListener listener,
            SelectableChannel channel, boolean wakeup);

    void unregister(SelectorListener listener, SelectableChannel channel);

    /**
     * Wake up the selector
     */
    void wakeup();

    /**
     * Run a given runnable in the loop.
     * 
     * @param task the task to be run in the main working loop.
     */
    void runInLoop(Runnable task);
}