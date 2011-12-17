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
package org.apache.mina.service;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.mina.api.IoSession;

/**
 * A strategy for using only one thread, for accepting and processing all
 * the acceptor events.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 */
public class OneThreadSelectorStrategy implements SelectorStrategy {
    /** The processor in charge of the messages processing */
    private SelectorProcessor processor;

    /**
     * Creates an instance of the OneThreadSelectorStrategy class
     * @param selectorFactory The Selector factory to use to create the processor
     */
    public OneThreadSelectorStrategy(SelectorFactory selectorFactory) {
        this.processor = selectorFactory.getNewSelector("uniqueSelector", this);
    }

    @Override
    public SelectorProcessor getSelectorForBindNewAddress() {
        return processor;
    }

    @Override
    public SelectorProcessor getSelectorForNewSession(SelectorProcessor acceptingProcessor) {
        return processor;
    }

    @Override
    public SelectorProcessor getSelectorForWrite(IoSession session) {
        return processor;
    }

    @Override
    public void unbind(SocketAddress address) throws IOException {
        processor.unbind(address);
    }

}
