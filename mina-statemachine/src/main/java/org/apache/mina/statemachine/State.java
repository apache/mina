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

import org.apache.mina.statemachine.transition.SelfTransition;
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
    /** The state ID */
    private final String id;

    /** The parent state */
    private final State parent;

    private List<TransitionHolder> transitionHolders = new ArrayList<>();

    /** The list of transitions for this state */
    private List<Transition> transitions = Collections.emptyList();

    /** The list of entry transitions on a state */
    private List<SelfTransition> onEntries = new ArrayList<>();

    /** The list of exit transition from a state */
    private List<SelfTransition> onExits = new ArrayList<>();

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
     * @return the id of this {@link State}.
     */
    public String getId() {
        return id;
    }

    /**
     * @return the parent or <code>null</code> if this {@link State} has no 
     *         parent.
     */
    public State getParent() {
        return parent;
    }

    /**
     * @return an unmodifiable {@link List} of {@link Transition}s going out
     * from this {@link State}.
     */
    public List<Transition> getTransitions() {
        return Collections.unmodifiableList(transitions);
    }

    /**
     * @return an unmodifiable {@link List} of entry {@link SelfTransition}s  
     */
    public List<SelfTransition> getOnEntrySelfTransitions() {
        return Collections.unmodifiableList(onEntries);
    }

    /**
     * @return an unmodifiable {@link List} of exit {@link SelfTransition}s  
     */
    public List<SelfTransition> getOnExitSelfTransitions() {
        return Collections.unmodifiableList(onExits);
    }

    /**
     * Adds an entry {@link SelfTransition} to this {@link State} 
     * 
     * @param selfTransition the {@link SelfTransition} to add.
     * @return this {@link State}.
     */
    State addOnEntrySelfTransaction(SelfTransition onEntrySelfTransaction) {
        if (onEntrySelfTransaction == null) {
            throw new IllegalArgumentException("transition");
        }
        
        onEntries.add(onEntrySelfTransaction);
        
        return this;
    }

    /**
     * Adds an exit {@link SelfTransition} to this {@link State} 
     * 
     * @param selfTransition the {@link SelfTransition} to add.
     * @return this {@link State}.
     */
    State addOnExitSelfTransaction(SelfTransition onExitSelfTransaction) {
        if (onExitSelfTransaction == null) {
            throw new IllegalArgumentException("transition");
        }
        
        onExits.add(onExitSelfTransaction);
        
        return this;
    }

    private void updateTransitions() {
        transitions = new ArrayList<>(transitionHolders.size());
        
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
     * @param weight The weight of this transition
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        
        if (!(o instanceof State)) {
            return false;
        }
        
        return id.equals(((State) o).id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int h = 37;
        
        return h * 17 + id.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("State[");
        sb.append("id=").append(id);
        sb.append("]");
        
        return sb.toString();
    }

    private static class TransitionHolder implements Comparable<TransitionHolder> {
        private Transition transition;

        private int weight;

        TransitionHolder(Transition transition, int weight) {
            this.transition = transition;
            this.weight = weight;
        }

        @Override
        public int compareTo(TransitionHolder o) {
            return (weight > o.weight) ? 1 : (weight < o.weight ? -1 : 0);
        }
    }
}
