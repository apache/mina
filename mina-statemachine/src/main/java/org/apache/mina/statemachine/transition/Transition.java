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
import org.apache.mina.statemachine.StateMachine;
import org.apache.mina.statemachine.event.Event;

/**
 * The interface implemented by classes which need to react on transitions
 * between states.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface Transition {
    /**
     * Executes this {@link Transition}. It is the responsibility of this
     * {@link Transition} to determine whether it actually applies for the
     * specified {@link Event}. If this {@link Transition} doesn't apply
     * nothing should be executed and <code>false</code> must be returned.
     * 
     * @param event the current {@link Event}.
     * @return <code>true</code> if the {@link Transition} was executed, 
     *         <code>false</code> otherwise.
     */
    boolean execute(Event event);
    
    /**
     * Returns the {@link State} which the {@link StateMachine} should move to 
     * if this {@link Transition} is taken and {@link #execute(Event)} returns
     * <code>true</code>.
     * 
     * @return the next {@link State} or <code>null</code> if this 
     *         {@link Transition} is a loopback {@link Transition}.
     */
    State getNextState();
}
