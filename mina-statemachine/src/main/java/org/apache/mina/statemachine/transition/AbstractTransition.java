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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.mina.statemachine.State;
import org.apache.mina.statemachine.StateMachine;
import org.apache.mina.statemachine.event.Event;

/**
 * Abstract {@link Transition} implementation. Takes care of matching the
 * current {@link Event}'s id against the id of the {@link Event} this 
 * {@link Transition} handles. To handle any {@link Event} the id should be set
 * to {@link Event#WILDCARD_EVENT_ID}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractTransition implements Transition {
    private final Object eventId;

    private final State nextState;

    /**
     * Creates a new instance which will loopback to the same {@link State} 
     * for the specified {@link Event} id.
     * 
     * @param eventId the {@link Event} id.
     */
    public AbstractTransition(Object eventId) {
        this(eventId, null);
    }

    /**
     * Creates a new instance with the specified {@link State} as next state 
     * and for the specified {@link Event} id.
     * 
     * @param eventId the {@link Event} id.
     * @param nextState the next {@link State}.
     */
    public AbstractTransition(Object eventId, State nextState) {
        this.eventId = eventId;
        this.nextState = nextState;
    }

    public State getNextState() {
        return nextState;
    }

    public boolean execute(Event event) {
        if (!eventId.equals(Event.WILDCARD_EVENT_ID) && !eventId.equals(event.getId())) {
            return false;
        }

        return doExecute(event);
    }

    /**
     * Executes this {@link Transition}. This method doesn't have to check
     * if the {@link Event}'s id matches because {@link #execute(Event)} has
     * already made sure that that is the case.
     * 
     * @param event the current {@link Event}.
     * @return <code>true</code> if the {@link Transition} has been executed 
     *         successfully and the {@link StateMachine} should move to the 
     *         next {@link State}. <code>false</code> otherwise.
     */
    protected abstract boolean doExecute(Event event);

    public boolean equals(Object o) {
        if (!(o instanceof AbstractTransition)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        AbstractTransition that = (AbstractTransition) o;
        return new EqualsBuilder()
            .append(eventId, that.eventId)
            .append(nextState, that.nextState)
            .isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder(11, 31).append(eventId).append(nextState).toHashCode();
    }

    public String toString() {
        return new ToStringBuilder(this)
            .append("eventId", eventId)
            .append("nextState", nextState)
            .toString();
    }
}
