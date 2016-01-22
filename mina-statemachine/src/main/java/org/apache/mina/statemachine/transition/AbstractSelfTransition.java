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

import org.apache.mina.statemachine.context.StateContext;
import org.apache.mina.statemachine.State;

/**
 * Abstract {@link SelfTransition} implementation.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */

public abstract class AbstractSelfTransition implements SelfTransition {
    /**
     * Creates a new instance
     */
    public AbstractSelfTransition() {

    }

    /**
     * Executes this {@link SelfTransition}.
     * 
     * @param stateContext the context in which the execution should occur
     * @param state the current state
     * @return <tt>true</tt> if the {@link SelfTransition} has been executed
     *         successfully
     */
    protected abstract boolean doExecute(StateContext stateContext, State state);

    /**
     * {@inheritDoc}
     */
    public boolean execute(StateContext stateContext, State state) {

        return doExecute(stateContext, state);
    }
}
