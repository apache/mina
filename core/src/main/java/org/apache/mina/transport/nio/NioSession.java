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
package org.apache.mina.transport.nio;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import org.apache.mina.api.IoSession;

/**
 * This interface extends the IoSession interface and adds specific
 * methods used by NIO session.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface NioSession extends IoSession {
    /**
     * @return The associated SelectionKey
     */
    SelectionKey getSelectionKey();

    /**
     * Associates a SelectionKey to this session.
     * @param key The session's SelectionKey
     */
    void setSelectionKey(SelectionKey key);

    /**
     * Refers to the Selector in used by this session. A session is managed by one
     * single selector during its own life.
     * 
     * @param selector The selector managing this session
     */
    void setSelector(Selector selector);

    /**
     * Wakeup the associated selector
     */
    void wakeup();
}
