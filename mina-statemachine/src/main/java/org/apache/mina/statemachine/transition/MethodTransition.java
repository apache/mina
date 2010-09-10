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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.mina.statemachine.State;
import org.apache.mina.statemachine.StateMachine;
import org.apache.mina.statemachine.StateMachineFactory;
import org.apache.mina.statemachine.annotation.Transition;
import org.apache.mina.statemachine.context.StateContext;
import org.apache.mina.statemachine.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Transition} which invokes a {@link Method}. The {@link Method} will
 * only be invoked if its argument types actually matches a subset of the 
 * {@link Event}'s argument types. The argument types are matched in order so
 * you must make sure the order of the method's arguments corresponds to the
 * order of the event's arguments. 
 *<p>
 * If the first method argument type matches
 * {@link Event} the current {@link Event} will be bound to that argument. In
 * the same manner the second argument (or first if the method isn't interested 
 * in the current {@link Event}) can have the {@link StateContext} type and will
 * in that case be bound to the current {@link StateContext}.
 * </p>
 * <p>
 * Normally you wouldn't create instances of this class directly but rather use the 
 * {@link Transition} annotation to define the methods which should be used as
 * transitions in your state machine and then let {@link StateMachineFactory} create a 
 * {@link StateMachine} for you.
 * </p>
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class MethodTransition extends AbstractTransition {
    private static final Logger LOGGER = LoggerFactory.getLogger( MethodTransition.class );
    private static final Object[] EMPTY_ARGUMENTS = new Object[0];
    
    private final Method method;
    private final Object target;

    /**
     * Creates a new instance with the specified {@link State} as next state 
     * and for the specified {@link Event} id.
     * 
     * @param eventId the {@link Event} id.
     * @param nextState the next {@link State}.
     * @param method the target method.
     * @param target the target object.
     */
    public MethodTransition(Object eventId, State nextState, Method method, Object target) {
        super(eventId, nextState);
        this.method = method;
        this.target = target;
    }

    /**
     * Creates a new instance which will loopback to the same {@link State} 
     * for the specified {@link Event} id.
     * 
     * @param eventId the {@link Event} id.
     * @param method the target method.
     * @param target the target object.
     */
    public MethodTransition(Object eventId, Method method, Object target) {
        this(eventId, null, method, target);
    }
    
    /**
     * Creates a new instance with the specified {@link State} as next state 
     * and for the specified {@link Event} id. The target {@link Method} will
     * be the method in the specified target object with the same name as the
     * specified {@link Event} id.
     * 
     * @param eventId the {@link Event} id.
     * @param nextState the next {@link State}.
     * @param target the target object.
     * @throws NoSuchMethodException if no method could be found with a name 
     *         equal to the {@link Event} id.
     * @throws AmbiguousMethodException if more than one method was found with 
     *         a name equal to the {@link Event} id.
     */
    public MethodTransition(Object eventId, State nextState, Object target) {
        this(eventId, nextState, eventId.toString(), target);
    }
    
    /**
     * Creates a new instance which will loopback to the same {@link State} 
     * for the specified {@link Event} id. The target {@link Method} will
     * be the method in the specified target object with the same name as the
     * specified {@link Event} id.
     * 
     * @param eventId the {@link Event} id.
     * @param target the target object.
     * @throws NoSuchMethodException if no method could be found with a name 
     *         equal to the {@link Event} id.
     * @throws AmbiguousMethodException if more than one method was found with 
     *         a name equal to the {@link Event} id.
     */
    public MethodTransition(Object eventId, Object target) {
        this(eventId, eventId.toString(), target);
    }

    /**
     * Creates a new instance which will loopback to the same {@link State} 
     * for the specified {@link Event} id.
     * 
     * @param eventId the {@link Event} id.
     * @param methodName the name of the target {@link Method}.
     * @param target the target object.
     * @throws NoSuchMethodException if the method could not be found.
     * @throws AmbiguousMethodException if there are more than one method with 
     *         the specified name.
     */
    public MethodTransition(Object eventId, String methodName, Object target) {
        this(eventId, null, methodName, target);
    }
    
    /**
     * Creates a new instance with the specified {@link State} as next state 
     * and for the specified {@link Event} id.
     * 
     * @param eventId the {@link Event} id.
     * @param nextState the next {@link State}.
     * @param methodName the name of the target {@link Method}.
     * @param target the target object.
     * @throws NoSuchMethodException if the method could not be found.
     * @throws AmbiguousMethodException if there are more than one method with 
     *         the specified name.
     */
    public MethodTransition(Object eventId, State nextState, String methodName, Object target) {
        super(eventId, nextState);

        this.target = target;
        
        Method[] candidates = target.getClass().getMethods();
        Method result = null;
        for (int i = 0; i < candidates.length; i++) {
            if (candidates[i].getName().equals(methodName)) {
                if (result != null) {
                    throw new AmbiguousMethodException(methodName);
                }
                result = candidates[i];
            }
        }
        
        if (result == null) {
            throw new NoSuchMethodException(methodName);
        }
        
        this.method = result;
    }
    
    /**
     * Returns the target {@link Method}.
     * 
     * @return the method.
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Returns the target object.
     * 
     * @return the target object.
     */
    public Object getTarget() {
        return target;
    }

    public boolean doExecute(Event event) {
        Class<?>[] types = method.getParameterTypes();
        
        if (types.length == 0) {
            invokeMethod(EMPTY_ARGUMENTS);
            return true;
        }
        
        if (types.length > 2 + event.getArguments().length) {
            return false;
        }
        
        Object[] args = new Object[types.length];
        
        int i = 0;
        if (match(types[i], event, Event.class)) {
            args[i++] = event;
        }
        if (i < args.length && match(types[i], event.getContext(), StateContext.class)) {
            args[i++] = event.getContext();
        }
        Object[] eventArgs = event.getArguments();
        for (int j = 0; i < args.length && j < eventArgs.length; j++) {
            if (match(types[i], eventArgs[j], Object.class)) {
                args[i++] = eventArgs[j];
            }
        }
        
        if (args.length > i) {
            return false;
        }
        
        invokeMethod(args);
        
        return true;
    }
    
    @SuppressWarnings("unchecked")
    private boolean match(Class<?> paramType, Object arg, Class argType) {
        if (paramType.isPrimitive()) {
            if (paramType.equals(Boolean.TYPE)) {
                return arg instanceof Boolean;
            }
            if (paramType.equals(Integer.TYPE)) {
                return arg instanceof Integer;
            }
            if (paramType.equals(Long.TYPE)) {
                return arg instanceof Long;
            }
            if (paramType.equals(Short.TYPE)) {
                return arg instanceof Short;
            }
            if (paramType.equals(Byte.TYPE)) {
                return arg instanceof Byte;
            }
            if (paramType.equals(Double.TYPE)) {
                return arg instanceof Double;
            }
            if (paramType.equals(Float.TYPE)) {
                return arg instanceof Float;
            }
            if (paramType.equals(Character.TYPE)) {
                return arg instanceof Character;
            }
        }
        return argType.isAssignableFrom(paramType) 
                && paramType.isAssignableFrom(arg.getClass());
    }

    private void invokeMethod(Object[] arguments) {
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Executing method " + method 
                        + " with arguments " + Arrays.asList(arguments));
            }
            method.invoke(target, arguments);
        } catch (InvocationTargetException ite) {
            if (ite.getCause() instanceof RuntimeException) {
                throw (RuntimeException) ite.getCause();
            }
            throw new MethodInvocationException(method, ite);
        } catch (IllegalAccessException iae) {
            throw new MethodInvocationException(method, iae);
        }
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof MethodTransition)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        MethodTransition that = (MethodTransition) o;
        return new EqualsBuilder()
            .appendSuper(super.equals(that))
            .append(method, that.method)
            .append(target, that.target)
            .isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder(13, 33).appendSuper(super.hashCode()).append(method).append(target).toHashCode();
    }

    public String toString() {
        return new ToStringBuilder(this)
            .appendSuper(super.toString())
            .append("method", method)
            .append("target", target)
            .toString();
    }
}
