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

import org.apache.mina.common.IoHandler;
import org.apache.mina.statemachine.annotation.IoFilterTransition;
import org.apache.mina.statemachine.annotation.IoFilterTransitions;

/**
 * Creates {@link StateMachine}s by reading {@link org.apache.mina.statemachine.annotation.State},
 * {@link IoFilterTransition} and {@link IoFilterTransitions} annotations 
 * from one or more arbitrary objects. This should be used instead of 
 * {@link StateMachineFactory} when creating {@link StateMachine}s for MINA's
 * {@link IoHandler} interface.
 * 
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class IoFilterStateMachineFactory {

    private IoFilterStateMachineFactory() {
    }
    
    /**
     * Creates a new {@link StateMachine} from the specified handler object and
     * using a start state with id <code>start</code>.
     * 
     * @param handler the object containing the annotations describing the 
     *        state machine.
     * @return the {@link StateMachine} object.
     */
    public static StateMachine create(Object handler) {
        return create(handler, new Object[0]);
    }

    /**
     * Creates a new {@link StateMachine} from the specified handler object and
     * using the {@link State} with the specified id as start state.
     * 
     * @param start the id of the start {@link State} to use.
     * @param handler the object containing the annotations describing the 
     *        state machine.
     * @return the {@link StateMachine} object.
     */
    public static StateMachine create(String start, Object handler) {
        return create(start, handler, new Object[0]);
    }

    /**
     * Creates a new {@link StateMachine} from the specified handler objects and
     * using a start state with id <code>start</code>.
     * 
     * @param handler the first object containing the annotations describing the 
     *        state machine.
     * @param handlers zero or more additional objects containing the 
     *        annotations describing the state machine.
     * @return the {@link StateMachine} object.
     */
    public static StateMachine create(Object handler, Object... handlers) {
        return create("start", handler, handlers);
    }
    
    /**
     * Creates a new {@link StateMachine} from the specified handler objects and
     * using the {@link State} with the specified id as start state.
     * 
     * @param start the id of the start {@link State} to use.
     * @param handler the first object containing the annotations describing the 
     *        state machine.
     * @param handlers zero or more additional objects containing the 
     *        annotations describing the state machine.
     * @return the {@link StateMachine} object.
     */
    public static StateMachine create(String start, Object handler, Object... handlers) {
        return StateMachineFactory.create(IoFilterTransition.class, IoFilterTransitions.class, start, handler, handlers);
    }
    
}
