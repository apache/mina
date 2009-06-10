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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.mina.statemachine.annotation.Transition;
import org.apache.mina.statemachine.annotation.TransitionAnnotation;
import org.apache.mina.statemachine.annotation.Transitions;
import org.apache.mina.statemachine.event.Event;
import org.apache.mina.statemachine.transition.MethodTransition;


/**
 * Creates {@link StateMachine}s by reading {@link org.apache.mina.statemachine.annotation.State},
 * {@link Transition} and {@link Transitions} (or equivalent) annotations from one or more arbitrary 
 * objects.
 * 
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class StateMachineFactory {
    private final Class<? extends Annotation> transitionAnnotation;
    private final Class<? extends Annotation> transitionsAnnotation;

    protected StateMachineFactory(Class<? extends Annotation> transitionAnnotation, 
                Class<? extends Annotation> transitionsAnnotation) {
        this.transitionAnnotation = transitionAnnotation;
        this.transitionsAnnotation = transitionsAnnotation;
    }
    
    /**
     * Returns a new {@link StateMachineFactory} instance which creates 
     * {@link StateMachine}s by reading the specified {@link Transition}
     * equivalent annotation.
     * 
     * @param transitionAnnotation the {@link Transition} equivalent annotation.
     * @return the {@link StateMachineFactory}.
     */
    public static StateMachineFactory getInstance(Class<? extends Annotation> transitionAnnotation) {
        TransitionAnnotation a = transitionAnnotation.getAnnotation(TransitionAnnotation.class);
        if (a == null) {
            throw new IllegalArgumentException("The annotation class " 
                    + transitionAnnotation + " has not been annotated with the " 
                    + TransitionAnnotation.class.getName() + " annotation");
        }
        return new StateMachineFactory(transitionAnnotation, a.value());
    }
    
    /**
     * Creates a new {@link StateMachine} from the specified handler object and
     * using a start state with id <code>start</code>.
     * 
     * @param handler the object containing the annotations describing the 
     *        state machine.
     * @return the {@link StateMachine} object.
     */
    public StateMachine create(Object handler) {
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
    public StateMachine create(String start, Object handler) {
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
    public StateMachine create(Object handler, Object... handlers) {
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
    public StateMachine create(String start, Object handler, Object... handlers) {
        
        Map<String, State> states = new HashMap<String, State>();
        List<Object> handlersList = new ArrayList<Object>(1 + handlers.length);
        handlersList.add(handler);
        handlersList.addAll(Arrays.asList(handlers));
        
        LinkedList<Field> fields = new LinkedList<Field>();
        for (Object h : handlersList) {
            fields.addAll(getFields(h instanceof Class ? (Class<?>) h : h.getClass()));
        }
        for (State state : createStates(fields)) {
            states.put(state.getId(), state);
        }

        if (!states.containsKey(start)) {
            throw new StateMachineCreationException("Start state '" + start + "' not found.");
        }

        setupTransitions(transitionAnnotation, transitionsAnnotation, states, handlersList);

        return new StateMachine(states.values(), start);
    }

    private static void setupTransitions(Class<? extends Annotation> transitionAnnotation, 
            Class<? extends Annotation> transitionsAnnotation, Map<String, State> states, List<Object> handlers) {
        for (Object handler : handlers) {
            setupTransitions(transitionAnnotation, transitionsAnnotation, states, handler);
        }
    }
    
    private static void setupTransitions(Class<? extends Annotation> transitionAnnotation, 
            Class<? extends Annotation> transitionsAnnotation, Map<String, State> states, Object handler) {
        
        Method[] methods = handler.getClass().getDeclaredMethods();
        Arrays.sort(methods, new Comparator<Method>() {
            public int compare(Method m1, Method m2) {
                return m1.toString().compareTo(m2.toString());
            }
        });
        
        for (Method m : methods) {
            List<TransitionWrapper> transitionAnnotations = new ArrayList<TransitionWrapper>();
            if (m.isAnnotationPresent(transitionAnnotation)) {
                transitionAnnotations.add(new TransitionWrapper(transitionAnnotation, m.getAnnotation(transitionAnnotation)));
            }
            if (m.isAnnotationPresent(transitionsAnnotation)) {
                transitionAnnotations.addAll(Arrays.asList(new TransitionsWrapper(transitionAnnotation, 
                        transitionsAnnotation, m.getAnnotation(transitionsAnnotation)).value()));
            }
            
            if (transitionAnnotations.isEmpty()) {
                continue;
            }
            
            for (TransitionWrapper annotation : transitionAnnotations) {
                Object[] eventIds = annotation.on();
                if (eventIds.length == 0) {
                    throw new StateMachineCreationException("Error encountered " 
                            + "when processing method " + m
                            + ". No event ids specified.");
                }
                if (annotation.in().length == 0) {
                    throw new StateMachineCreationException("Error encountered " 
                            + "when processing method " + m
                            + ". No states specified.");
                }
                
                State next = null;
                if (!annotation.next().equals(Transition.SELF)) {
                    next = states.get(annotation.next());
                    if (next == null) {
                        throw new StateMachineCreationException("Error encountered " 
                                + "when processing method " + m
                                + ". Unknown next state: " + annotation.next() + ".");
                    }
                }
                
                for (Object event : eventIds) {
                    if (event == null) {
                        event = Event.WILDCARD_EVENT_ID;
                    }
                    if (!(event instanceof String)) {
                        event = event.toString();
                    }
                    for (String in : annotation.in()) {
                        State state = states.get(in);
                        if (state == null) {
                            throw new StateMachineCreationException("Error encountered " 
                                    + "when processing method "
                                    + m + ". Unknown state: " + in + ".");
                        }

                        state.addTransition(new MethodTransition(event, next, m, handler), annotation.weight());
                    }
                }
            }
        }
    }

    static List<Field> getFields(Class<?> clazz) {
        LinkedList<Field> fields = new LinkedList<Field>();

        for (Field f : clazz.getDeclaredFields()) {
            if (!f.isAnnotationPresent(org.apache.mina.statemachine.annotation.State.class)) {
                continue;
            }

            if ((f.getModifiers() & Modifier.STATIC) == 0 
                    || (f.getModifiers() & Modifier.FINAL) == 0
                    || !f.getType().equals(String.class)) {
                throw new StateMachineCreationException("Error encountered when " 
                        + "processing field " + f
                        + ". Only static final " 
                        + "String fields can be used with the @State " 
                        + "annotation.");
            }

            if (!f.isAccessible()) {
                f.setAccessible(true);
            }

            fields.add(f);
        }

        return fields;
    }
        
    static State[] createStates(List<Field> fields) {
        LinkedHashMap<String, State> states = new LinkedHashMap<String, State>();

        while (!fields.isEmpty()) {
            int size = fields.size();
            int numStates = states.size();
            for (int i = 0; i < size; i++) {
                Field f = fields.remove(0);

                String value = null;
                try {
                    value = (String) f.get(null);
                } catch (IllegalAccessException iae) {
                    throw new StateMachineCreationException("Error encountered when " 
                            + "processing field " + f + ".", iae);
                }

                org.apache.mina.statemachine.annotation.State stateAnnotation = f
                        .getAnnotation(org.apache.mina.statemachine.annotation.State.class);
                if (stateAnnotation.value().equals(org.apache.mina.statemachine.annotation.State.ROOT)) {
                    states.put(value, new State(value));
                } else if (states.containsKey(stateAnnotation.value())) {
                    states.put(value, new State(value, states.get(stateAnnotation.value())));
                } else {
                    // Move to the back of the list of fields for later
                    // processing
                    fields.add(f);
                }
            }

            /*
             * If no new states were added to states during this iteration it 
             * means that all fields in fields specify non-existent parents.
             */
            if (states.size() == numStates) {
                throw new StateMachineCreationException("Error encountered while creating "
                        + "FSM. The following fields specify non-existing " 
                        + "parent states: " + fields);
            }
        }

        return states.values().toArray(new State[0]);
    }
    
    private static class TransitionWrapper {
        private final Class<? extends Annotation> transitionClazz;
        private final Annotation annotation;
        public TransitionWrapper(Class<? extends Annotation> transitionClazz, Annotation annotation) {
            this.transitionClazz = transitionClazz;
            this.annotation = annotation;
        }
        Object[] on() {
            return getParameter("on", Object[].class);
        }
        String[] in() {
            return getParameter("in", String[].class);
        }
        String next() {
            return getParameter("next", String.class);
        }
        int weight() {
            return getParameter("weight", Integer.TYPE);
        }
        @SuppressWarnings("unchecked")
        private <T> T getParameter(String name, Class<T> returnType) {
            try {
                Method m = transitionClazz.getMethod(name);
                if (!returnType.isAssignableFrom(m.getReturnType())) {
                    throw new NoSuchMethodException();
                }
                return (T) m.invoke(annotation);
            } catch (Throwable t) {
                throw new StateMachineCreationException("Could not get parameter '" 
                        + name + "' from Transition annotation " + transitionClazz);
            }
        }
    }
    
    private static class TransitionsWrapper {
        private final Class<? extends Annotation> transitionsclazz;
        private final Class<? extends Annotation> transitionClazz;
        private final Annotation annotation;
        public TransitionsWrapper(Class<? extends Annotation> transitionClazz, 
                Class<? extends Annotation> transitionsclazz, Annotation annotation) {
            this.transitionClazz = transitionClazz;
            this.transitionsclazz = transitionsclazz;
            this.annotation = annotation;
        }
        TransitionWrapper[] value() {
            Annotation[] annos = getParameter("value", Annotation[].class);
            TransitionWrapper[] wrappers = new TransitionWrapper[annos.length];
            for (int i = 0; i < annos.length; i++) {
                wrappers[i] = new TransitionWrapper(transitionClazz, annos[i]);
            }
            return wrappers;
        }
        @SuppressWarnings("unchecked")
        private <T> T getParameter(String name, Class<T> returnType) {
            try {
                Method m = transitionsclazz.getMethod(name);
                if (!returnType.isAssignableFrom(m.getReturnType())) {
                    throw new NoSuchMethodException();
                }
                return (T) m.invoke(annotation);
            } catch (Throwable t) {
                throw new StateMachineCreationException("Could not get parameter '" 
                        + name + "' from Transitions annotation " + transitionsclazz);
            }
        }
    }
}
