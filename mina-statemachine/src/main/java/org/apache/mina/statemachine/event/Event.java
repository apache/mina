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
package org.apache.mina.statemachine.event;

import org.apache.mina.statemachine.context.StateContext;

/**
 * Represents an event which typically corresponds to a method call on a proxy.
 * An event has an id and zero or more arguments typically corresponding to
 * the method arguments. 
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Event {
    /** The wildcard event */
    public static final String WILDCARD_EVENT_ID = "*";

    private final Object id;

    private final StateContext context;

    private final Object[] arguments;

    /**
     * Creates a new {@link Event} with the specified id and no arguments.
     * 
     * @param id the event id.
     * @param context the {@link StateContext} the event was triggered for.
     */
    public Event(Object id, StateContext context) {
        this(id, context, new Object[0]);
    }

    /**
     * Creates a new {@link Event} with the specified id and arguments.
     * 
     * @param id the event id.
     * @param context the {@link StateContext} the event was triggered for.
     * @param arguments the event arguments.
     */
    public Event(Object id, StateContext context, Object[] arguments) {
        if (id == null) {
            throw new IllegalArgumentException("id");
        }
        
        if (context == null) {
            throw new IllegalArgumentException("context");
        }
        
        if (arguments == null) {
            throw new IllegalArgumentException("arguments");
        }
        
        this.id = id;
        this.context = context;
        this.arguments = arguments;
    }

    /**
     * @return the {@link StateContext} this {@link Event} was triggered for.
     */
    public StateContext getContext() {
        return context;
    }

    /**
     * @return the id of this {@link Event}.
     */
    public Object getId() {
        return id;
    }

    /**
     * @return the arguments of this {@link Event}. @return an empty array if this {@link Event} has 
     *         no arguments.
     */
    public Object[] getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Event[");
        sb.append("id=").append(id);
        sb.append(",context=").append(context);
        sb.append(",arguments=");
        
        if (arguments != null) {
            sb.append('{');
            boolean isFirst = true;
            
            for (Object argument:arguments) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    sb.append(',');
                }
                
                sb.append(argument);
            }
            
            sb.append('}');
        } else {
            sb.append("null");
        }

        sb.append("]");
        
        return sb.toString();
    }
}
