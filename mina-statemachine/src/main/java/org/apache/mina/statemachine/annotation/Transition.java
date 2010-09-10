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
package org.apache.mina.statemachine.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.mina.statemachine.StateMachine;
import org.apache.mina.statemachine.event.Event;

/**
 * Annotation used on methods to indicate that the method handles a specific
 * kind of event when in a specific state.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@TransitionAnnotation(Transitions.class)
public @interface Transition {
    public static final String SELF = "__self__";

    /**
     * Specifies the ids of one or more events handled by the annotated method. If
     * not specified the handler method will be executed for any event.
     */
    String[] on() default Event.WILDCARD_EVENT_ID;

    /**
     * The id of the state or states that this handler applies to. Must be
     * specified.
     */
    String[] in();

    /**
     * The id of the state the {@link StateMachine} should move to next after
     * executing the annotated method. If not specified the {@link StateMachine}
     * will remain in the same state.
     */
    String next() default SELF;

    /**
     * The weight used to order handler annotations which match the same event 
     * in the same state. Transitions with lower weight will be matched first. The
     * default weight is 0.
     */
    int weight() default 0;
}
