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
package org.apache.mina.transport.socket.nio;

import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;

import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.IoSession;

/**
 * An {@link IoSession} which is managed by the NIO transport.
 *  
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class NioSession extends AbstractIoSession {
    /** The NioSession processor */
    protected final IoProcessor<NioSession> processor;
    
    
    /**
     * 
     * Creates a new instance of NioSession, with its associated IoProcessor.
     * <br>
     * This method is only called by the inherited class.
     *
     * @param processor The associated IoProcessor
     */
    protected  NioSession(IoProcessor<NioSession> processor) {
        this.processor = processor;
    }

    /**
     * @return The ByteChannel associated with this {@link IoSession} 
     */
    abstract ByteChannel getChannel();
    
    /**
     * @return The {@link SelectionKey} associated with this {@link IoSession}
     */
    abstract SelectionKey getSelectionKey();
    
    /**
     * Sets the {@link SelectionKey} for this {@link IoSession}
     *
     * @param key The new {@link SelectionKey}
     */
    abstract void setSelectionKey(SelectionKey key);

    /**
     * {@inheritDoc}
     */
    public IoProcessor<NioSession> getProcessor() {
        return processor;
    }
}
