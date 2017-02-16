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
 * A Transition must implement two methods 
 * <ul>
 *   <li>execute : a method called when we process the transition</li>
 *   <li>getNextState : a method that gives the next state for this transition</li>
 * </ul>
 * 
 * Each Transition accepts two parameters :
 * <ul>
 *   <li>An event ID : this defines the event this transition will accept</li>
 *   <li>A next state</li>
 * </ul>
 * 
 * The event ID might be '*', which means the transition will accept any event.
 * The next state can be null, which means teh next state is the current state.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface Transition {
    /**
     * Executes this {@link Transition}. It is the responsibility of this
     * {@link Transition} to determine whether it actually applies for the
     * specified {@link Event}. If this {@link Transition} doesn't apply
     * nothing should be executed and <tt>false</tt> must be returned.
     * The method will accept any {@link Event} if it is registered with the 
     * wild card event ID ('*'), and the event ID it is declared for (ie,
     * the event ID that has been passed as a parameter to this transition 
     * constructor.)
     * 
     * @param event the current {@link Event}.
     * @return <tt>true</tt> if the {@link Transition} was executed, 
     *         <tt>false</tt> otherwise.
     */
    boolean execute(Event event);

    /**
     * @return the {@link State} which the {@link StateMachine} should move to 
     * if this {@link Transition} is taken and {@link #execute(Event)} returns
     * <tt>true</tt>. <code>null</code> if this {@link Transition} is a loopback 
     * {@link Transition}.
     */
    State getNextState();
}
