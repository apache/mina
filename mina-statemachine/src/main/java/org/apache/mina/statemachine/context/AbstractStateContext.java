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

import java.util.HashMap;
import java.util.Map;

import org.apache.mina.statemachine.State;

/**
 * Abstract {@link StateContext} which uses a {@link Map} to store the
 * attributes.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractStateContext implements StateContext {
    private State currentState = null;

    private Map<Object, Object> attributes = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getAttribute(Object key) {
        return getAttributes().get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public State getCurrentState() {
        return currentState;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttribute(Object key, Object value) {
        getAttributes().put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentState(State state) {
        currentState = state;
    }

    protected Map<Object, Object> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        return attributes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("StateContext[");
        sb.append("currentState=").append(currentState);
        sb.append(",attributes=").append(attributes);
        sb.append("]");
        
        return sb.toString();
    }
}
