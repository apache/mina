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

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.mina.statemachine.context.StateContext;
import org.apache.mina.statemachine.event.Event;
import org.apache.mina.statemachine.event.UnhandledEventException;
import org.apache.mina.statemachine.transition.SelfTransition;
import org.apache.mina.statemachine.transition.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a complete state machine. Contains a collection of {@link State}
 * objects connected by {@link Transition}s. Normally you wouldn't create 
 * instances of this class directly but rather use the 
 * {@link org.apache.mina.statemachine.annotation.State} annotation to define
 * your states and then let {@link StateMachineFactory} create a 
 * {@link StateMachine} for you.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class StateMachine {
    private static final Logger LOGGER = LoggerFactory.getLogger(StateMachine.class);

    private static final String CALL_STACK = StateMachine.class.getName() + ".callStack";

    private final State startState;

    private final Map<String, State> states;

    private final ThreadLocal<Boolean> processingThreadLocal = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    private final ThreadLocal<LinkedList<Event>> eventQueueThreadLocal = new ThreadLocal<LinkedList<Event>>() {
        @Override
        protected LinkedList<Event> initialValue() {
            return new LinkedList<>();
        }
    };

    /**
     * Creates a new instance using the specified {@link State}s and start
     * state.
     * 
     * @param states the {@link State}s.
     * @param startStateId the id of the start {@link State}.
     */
    public StateMachine(State[] states, String startStateId) {
        this.states = new HashMap<>();
        
        for (State s : states) {
            this.states.put(s.getId(), s);
        }
        
        this.startState = getState(startStateId);
    }

    /**
     * Creates a new instance using the specified {@link State}s and start
     * state.
     * 
     * @param states the {@link State}s.
     * @param startStateId the id of the start {@link State}.
     */
    public StateMachine(Collection<State> states, String startStateId) {
        this(states.toArray(new State[0]), startStateId);
    }

    /**
     * Returns the {@link State} with the specified id.
     * 
     * @param id the id of the {@link State} to return.
     * @return the {@link State}
     * @throws NoSuchStateException if no matching {@link State} could be found.
     */
    public State getState(String id) {
        State state = states.get(id);
        
        if (state == null) {
            throw new NoSuchStateException(id);
        }
        
        return state;
    }

    /**
     * @return an unmodifiable {@link Collection} of all {@link State}s used by
     * this {@link StateMachine}.
     */
    public Collection<State> getStates() {
        return Collections.unmodifiableCollection(states.values());
    }

    /**
     * Processes the specified {@link Event} through this {@link StateMachine}.
     * Normally you wouldn't call this directly but rather use
     * {@link StateMachineProxyBuilder} to create a proxy for an interface of
     * your choice. Any method calls on the proxy will be translated into
     * {@link Event} objects and then fed to the {@link StateMachine} by the
     * proxy using this method.
     * 
     * @param event the {@link Event} to be handled.
     */
    public void handle(Event event) {
        StateContext context = event.getContext();

        synchronized (context) {
            LinkedList<Event> eventQueue = eventQueueThreadLocal.get();
            eventQueue.addLast(event);

            if (processingThreadLocal.get()) {
                /*
                 * This thread is already processing an event. Queue this 
                 * event.
                 */
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("State machine called recursively. Queuing event {} for later processing.", event);
                }
            } else {
                processingThreadLocal.set(true);
                
                try {
                    if (context.getCurrentState() == null) {
                        context.setCurrentState(startState);
                    }
                    
                    processEvents(eventQueue);
                } finally {
                    processingThreadLocal.set(false);
                }
            }
        }
    }

    private void processEvents(LinkedList<Event> eventQueue) {
        while (!eventQueue.isEmpty()) {
            Event event = eventQueue.removeFirst();
            StateContext context = event.getContext();
            handle(context.getCurrentState(), event);
        }
    }

    private void handle(State state, Event event) {
        StateContext context = event.getContext();

        for (Transition t : state.getTransitions()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Trying transition {}", t);
            }

            try {
                if (t.execute(event)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Transition {} executed successfully.", t);
                    }
                    
                    setCurrentState(context, t.getNextState());

                    return;
                }
            } catch (BreakAndContinueException bace) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("BreakAndContinueException thrown in transition {}. Continuing with next transition.", t);
                }
            } catch (BreakAndGotoException bage) {
                State newState = getState(bage.getStateId());

                if (bage.isNow()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("BreakAndGotoException thrown in transition {}. Moving to state {} now", t,
                            newState.getId());
                    }
                    
                    setCurrentState(context, newState);
                    handle(newState, event);
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("BreakAndGotoException thrown in transition {}. Moving to state {} next.",
                                t, newState.getId());
                    }
                    
                    setCurrentState(context, newState);
                }
                
                return;
            } catch (BreakAndCallException bace) {
                State newState = getState(bace.getStateId());

                Deque<State> callStack = getCallStack(context);
                State returnTo = bace.getReturnToStateId() != null ? getState(bace.getReturnToStateId()) : context
                        .getCurrentState();
                callStack.push(returnTo);

                if (bace.isNow()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("BreakAndCallException thrown in transition {}. Moving to state {} now.",
                                t, newState.getId());
                    }
                    
                    setCurrentState(context, newState);
                    handle(newState, event);
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("BreakAndCallException thrown in transition {}. Moving to state {} next.",
                                t, newState.getId());
                    }
                    
                    setCurrentState(context, newState);
                }
                
                return;
            } catch (BreakAndReturnException bare) {
                Deque<State> callStack = getCallStack(context);
                State newState = callStack.pop();

                if (bare.isNow()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("BreakAndReturnException thrown in transition {}. Moving to state {} now.",
                                t, newState.getId());
                    }
                    
                    setCurrentState(context, newState);
                    handle(newState, event);
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("BreakAndReturnException thrown in transition {}. Moving to state {} next.",
                                t, newState.getId());
                    }
                    
                    setCurrentState(context, newState);
                }
                
                return;
            }
        }

        /*
         * No transition could handle the event. Try with the parent state if
         * there is one.
         */
        if (state.getParent() != null) {
            handle(state.getParent(), event);
        } else {
            throw new UnhandledEventException(event);
        }
    }

    private Deque<State> getCallStack(StateContext context) {
        @SuppressWarnings("unchecked")
        Deque<State> callStack = (Deque<State>) context.getAttribute(CALL_STACK);
        
        if (callStack == null) {
            callStack = new ConcurrentLinkedDeque<>();
            context.setAttribute(CALL_STACK, callStack);
        }
        
        return callStack;
    }

    private void setCurrentState(StateContext context, State newState) {
        if (newState != null) {
            if (LOGGER.isDebugEnabled()) {
                if (newState != context.getCurrentState()) {
                    LOGGER.debug("Leaving state {}", context.getCurrentState().getId());
                    LOGGER.debug("Entering state {}", newState.getId());
                }
            }
            
            executeOnExits(context, context.getCurrentState());
            executeOnEntries(context, newState);
            context.setCurrentState(newState);
        }
    }

    void executeOnExits(StateContext context, State state) {
        List<SelfTransition> onExits = state.getOnExitSelfTransitions();
        boolean isExecuted = false;

        if (onExits != null) {
            for (SelfTransition selfTransition : onExits) {
                selfTransition.execute(context, state);
                
                if (LOGGER.isDebugEnabled()) {
                    isExecuted = true;
                    LOGGER.debug("Executing onEntry action for {}", state.getId());
                }
            }
        }
        
        if (LOGGER.isDebugEnabled() && !isExecuted) {
            LOGGER.debug("No onEntry action for {}", state.getId());

        }
    }

    void executeOnEntries(StateContext context, State state) {
        List<SelfTransition> onEntries = state.getOnEntrySelfTransitions();
        boolean isExecuted = false;

        if (onEntries != null) {
            for (SelfTransition selfTransition : onEntries) {
                selfTransition.execute(context, state);
                
                if (LOGGER.isDebugEnabled()) {
                    isExecuted = true;
                    LOGGER.debug("Executing onExit action for {}", state.getId());
                }
            }
        }
        
        if (LOGGER.isDebugEnabled() && !isExecuted) {
            LOGGER.debug("No onEntry action for {}", state.getId());
        }
    }
}
