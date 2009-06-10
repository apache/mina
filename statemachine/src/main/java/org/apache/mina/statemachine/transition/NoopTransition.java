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
package org.apache.mina.statemachine.transition;

import org.apache.mina.statemachine.State;
import org.apache.mina.statemachine.event.Event;

/**
 * {@link Transition} implementation which does nothing but change the state.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NoopTransition extends AbstractTransition {

    /**
     * Creates a new instance which will loopback to the same {@link State} 
     * for the specified {@link Event} id.
     * 
     * @param eventId the {@link Event} id.
     */
    public NoopTransition(Object eventId) {
        super(eventId);
    }

    /**
     * Creates a new instance with the specified {@link State} as next state 
     * and for the specified {@link Event} id.
     * 
     * @param eventId the {@link Event} id.
     * @param nextState the next {@link State}.
     */
    public NoopTransition(Object eventId, State nextState) {
        super(eventId, nextState);
    }
    
    protected boolean doExecute(Event event) {
        return true;
    }
    
}
