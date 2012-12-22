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
package org.apache.mina.service.executor;

import org.apache.mina.api.IoHandler;
import org.apache.mina.api.IoSession;

/**
 * A {@link IoHandler} event to be submitted to an {@link IoHandlerExecutor}.
 * 
 * Use the visitor pattern for implementing different behaviour for different events : <a
 * href="http://en.wikipedia.org/wiki/Visitor_pattern">visitor pattern"</a>
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface Event {

    /**
     * The session of this event.
     * 
     * @return the session (cannot be null)
     */
    IoSession getSession();

    /**
     * Call the visitor method for this kind of event.
     * 
     * @param visitor the vistor to call
     */
    void visit(EventVisitor visitor);
}
