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
package org.apache.mina.statemachine.context;

import org.apache.mina.statemachine.State;
import org.apache.mina.statemachine.StateMachine;

/**
 * {@link StateContext} objects are used to store the current {@link State} and
 * any application specific attributes for a specific client of a 
 * {@link StateMachine}. Since {@link StateMachine}s are singletons and shared
 * by all clients using the {@link StateMachine} this is where client specific
 * data needs to be stored.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface StateContext {
    /**
     * Returns the current {@link State}. This is only meant for internal use.
     * 
     * @return the current {@link State}.
     */
    State getCurrentState();

    /**
     * Sets the current {@link State}. This is only meant for internal use.
     * Don't call it directly!
     * 
     * @param state the new current {@link State}.
     */
    void setCurrentState(State state);
    
    /**
     * Returns the value of the attribute with the specified key or 
     * <code>null</code>if not found.
     * 
     * @param key the key.
     * @return the value or <code>null</code>.
     */
    Object getAttribute(Object key);

    /**
     * Sets the value of the attribute with the specified key.
     * 
     * @param key the key.
     * @param value the value.
     */
    void setAttribute(Object key, Object value);
}
