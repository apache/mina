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
package org.apache.mina.handler.chain;

import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;

/**
 * An {@link IoHandler} which executes an {@link IoHandlerChain}
 * on a <tt>messageReceived</tt> event.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ChainedIoHandler extends IoHandlerAdapter {
    private final IoHandlerChain chain;

    /**
     * Creates a new instance which contains an empty {@link IoHandlerChain}.
     */
    public ChainedIoHandler() {
        chain = new IoHandlerChain();
    }

    /**
     * Creates a new instance which executes the specified
     * {@link IoHandlerChain} on a <tt>messageReceived</tt> event.
     * 
     * @param chain an {@link IoHandlerChain} to execute
     */
    public ChainedIoHandler(IoHandlerChain chain) {
        if (chain == null) {
            throw new NullPointerException("chain");
        }
        this.chain = chain;
    }

    /**
     * Returns the {@link IoHandlerCommand} this handler will use to
     * handle <tt>messageReceived</tt> events.
     */
    public IoHandlerChain getChain() {
        return chain;
    }

    /**
     * Handles the specified <tt>messageReceived</tt> event with the
     * {@link IoHandlerCommand} or {@link IoHandlerChain} you specified
     * in the constructor.  
     */
    public void messageReceived(IoSession session, Object message)
            throws Exception {
        chain.execute(null, session, message);
    }
}
