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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.mina.statemachine.event.Event;
import org.apache.mina.statemachine.transition.Transition;

/**
 * Represents a state in a {@link StateMachine}. Normally you wouldn't create 
 * instances of this class directly but rather use the 
 * {@link org.apache.mina.statemachine.annotation.State} annotation to define
 * your states and then let {@link StateMachineFactory} create a 
 * {@link StateMachine} for you.
 * <p> 
 * {@link State}s  inherits {@link Transition}s from
 * their parent. A {@link State} can override any of the parents 
 * {@link Transition}s. When an {@link Event} is processed the {@link Transition}s
 * of the current {@link State} will be searched for a {@link Transition} which
 * can handle the event. If none is found the {@link State}'s parent will be
 * searched and so on.
 * </p>
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class State {
    private final String id;
    private final State parent;
    private List<TransitionHolder> transitionHolders = new ArrayList<TransitionHolder>();
    private List<Transition> transitions = Collections.emptyList();
    
    /**
     * Creates a new {@link State} with the specified id.
     * 
     * @param id the unique id of this {@link State}.
     */
    public State(String id) {
        this(id, null);
    }

    /**
     * Creates a new {@link State} with the specified id and parent.
     * 
     * @param id the unique id of this {@link State}.
     * @param parent the parent {@link State}.
     */
    public State(String id, State parent) {
        this.id = id;
        this.parent = parent;
    }

    /**
     * Returns the id of this {@link State}.
     * 
     * @return the id.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the parent {@link State}.
     * 
     * @return the parent or <code>null</code> if this {@link State} has no 
     *         parent.
     */
    public State getParent() {
        return parent;
    }

    /**
     * Returns an unmodifiable {@link List} of {@link Transition}s going out
     * from this {@link State}.
     * 
     * @return the {@link Transition}s.
     */
    public List<Transition> getTransitions() {
        return Collections.unmodifiableList(transitions);
    }

    private void updateTransitions() {
        transitions = new ArrayList<Transition>(transitionHolders.size());
        for (TransitionHolder holder : transitionHolders) {
            transitions.add(holder.transition);
        }
    }
    
    /**
     * Adds an outgoing {@link Transition} to this {@link State} with weight 0.
     * 
     * @param transition the {@link Transition} to add.
     * @return this {@link State}.
     * @see #addTransition(Transition, int)
     */
    public State addTransition(Transition transition) {
        return addTransition(transition, 0);
    }

    /**
     * Adds an outgoing {@link Transition} to this {@link State} with the 
     * specified weight. The higher the weight the less important a 
     * {@link Transition} is. If two {@link Transition}s match the same
     * {@link Event} the {@link Transition} with the lower weight will
     * be executed.
     * 
     * @param transition the {@link Transition} to add.
     * @return this {@link State}.
     */
    public State addTransition(Transition transition, int weight) {
        if (transition == null) {
            throw new IllegalArgumentException("transition");
        }

        transitionHolders.add(new TransitionHolder(transition, weight));
        Collections.sort(transitionHolders);
        updateTransitions();
        return this;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof State)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        State that = (State) o;
        return new EqualsBuilder().append(this.id, that.id).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 33).append(this.id).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("id", this.id).toString();
    }

    private static class TransitionHolder implements Comparable<TransitionHolder> {
        Transition transition;

        int weight;

        TransitionHolder(Transition transition, int weight) {
            this.transition = transition;
            this.weight = weight;
        }

        public int compareTo(TransitionHolder o) {
            return (weight > o.weight) ? 1 : (weight < o.weight ? -1 : 0);
        }
    }
}
