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
package org.apache.mina.statemachine;

import org.apache.mina.statemachine.event.Event;
import org.apache.mina.statemachine.transition.Transition;

/**
 * Allows for programmatic control of a state machines execution.
 * <p>
 * The <code>*Now()</code> family of methods move to a new {@link State}
 * immediately and let the new {@link State} handle the current {@link Event}.
 * The <code>*Next()</code> family on the other hand let the new {@link State}
 * handle the next {@link Event} which is generated which make these method the 
 * programmatic equivalent of using the {@link org.apache.mina.statemachine.annotation.Transition} annotation.
 * </p>
 * <p>
 * Using the <code>breakAndCall*()</code> and <code>breakAndReturn*</code> methods one
 * can create sub state machines which behave very much like sub routines.
 * When calling a state the current state (or the specified <code>returnTo</code>
 * state) will be pushed on a stack. When returning from a state the last pushed
 * state will be popped from the stack and used as the new state.
 * </p>
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class StateControl {

    /**
     * Breaks the execution of the current {@link Transition} and tries to
     * find another {@link Transition} with higher weight or a {@link Transition}
     * of a parent {@link State} which can handle the current {@link Event}.
     */
    public static void breakAndContinue() {
        throw new BreakAndContinueException();
    }

    /**
     * Breaks the execution of the current {@link Transition} and lets the
     * {@link State} with the specified id handle the <strong>current</strong> {@link Event}.
     * 
     * @param state the id of the {@link State} to go to.
     */
    public static void breakAndGotoNow(String state) {
        throw new BreakAndGotoException(state, true);
    }

    /**
     * Breaks the execution of the current {@link Transition} and lets the
     * {@link State} with the specified id handle the <strong>next</strong> {@link Event}.
     * Using this method is the programmatic equivalent of using the
     * {@link org.apache.mina.statemachine.annotation.Transition} annotation.
     * 
     * @param state the id of the {@link State} to go to.
     */
    public static void breakAndGotoNext(String state) {
        throw new BreakAndGotoException(state, false);
    }

    /**
     * Breaks the execution of the current {@link Transition} and lets the
     * {@link State} with the specified id handle the <strong>current</strong> {@link Event}.
     * Before moving to the new state the current state will be recorded. The
     * next call to {@link #breakAndReturnNow()} or {@link #breakAndReturnNext()}
     * will return to the current state.
     * 
     * @param state the id of the {@link State} to call.
     */
    public static void breakAndCallNow(String state) {
        throw new BreakAndCallException(state, true);
    }

    /**
     * Breaks the execution of the current {@link Transition} and lets the
     * {@link State} with the specified id handle the <strong>next</strong> {@link Event}.
     * Before moving to the new state the current state will be recorded. The
     * next call to {@link #breakAndReturnNow()} or {@link #breakAndReturnNext()}
     * will return to the current state.
     * 
     * @param state the id of the {@link State} to call.
     */
    public static void breakAndCallNext(String state) {
        throw new BreakAndCallException(state, false);
    }

    /**
     * Breaks the execution of the current {@link Transition} and lets the
     * {@link State} with the specified id handle the <strong>current</strong> {@link Event}.
     * Before moving to the new state the current state will be recorded. The
     * next call to {@link #breakAndReturnNow()} or {@link #breakAndReturnNext()}
     * will return to the specified <code>returnTo</code> state.
     * 
     * @param state the id of the {@link State} to call.
     * @param returnTo the id of the {@link State} to return to.
     */
    public static void breakAndCallNow(String state, String returnTo) {
        throw new BreakAndCallException(state, returnTo, true);
    }

    /**
     * Breaks the execution of the current {@link Transition} and lets the
     * {@link State} with the specified id handle the <strong>next</strong> {@link Event}.
     * Before moving to the new state the current state will be recorded. The
     * next call to {@link #breakAndReturnNow()} or {@link #breakAndReturnNext()}
     * will return to the specified <code>returnTo</code> state.
     * 
     * @param state the id of the {@link State} to call.
     * @param returnTo the id of the {@link State} to return to.
     */
    public static void breakAndCallNext(String state, String returnTo) {
        throw new BreakAndCallException(state, returnTo, false);
    }

    /**
     * Breaks the execution of the current {@link Transition} and lets the
     * last recorded {@link State} handle the <strong>current</strong> {@link Event}.
     */
    public static void breakAndReturnNow() {
        throw new BreakAndReturnException(true);
    }

    /**
     * Breaks the execution of the current {@link Transition} and lets the
     * last recorded {@link State} handle the <strong>next</strong> {@link Event}.
     */
    public static void breakAndReturnNext() {
        throw new BreakAndReturnException(false);
    }
}
