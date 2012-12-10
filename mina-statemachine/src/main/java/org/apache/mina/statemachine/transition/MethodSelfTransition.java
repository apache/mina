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

import org.apache.mina.statemachine.context.StateContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.mina.statemachine.State;

/**
 * {@link SelfTransition} which invokes a {@link Method}. The {@link Method} can
 * have zero or any number of StateContext and State regarding order
 * <p>
 * Normally you wouldn't create instances of this class directly but rather use the
 * {@link SelfTransition} annotation to define the methods which should be used as
 * transitions in your state machine and then let {@link StateMachineFactory} create a
 * {@link StateMachine} for you.
 * </p>
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class MethodSelfTransition extends AbstractSelfTransition {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodTransition.class);

    private Method method;

    private final Object target;

    private static final Object[] EMPTY_ARGUMENTS = new Object[0];

    public MethodSelfTransition(Method method, Object target) {
        super();
        this.method = method;
        this.target = target;
    }

    /**
     * Creates a new instance
     * 
     * @param method the target method.
     * @param target the target object.
     */
    public MethodSelfTransition(String methodName, Object target) {

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

    public boolean doExecute(StateContext stateContext, State state) {
        Class<?>[] types = method.getParameterTypes();

        if (types.length == 0) {
            invokeMethod(EMPTY_ARGUMENTS);
            return true;
        }

        if (types.length > 2) {
            return false;
        }

        Object[] args = new Object[types.length];

        int i = 0;
        
        if (types[i].isAssignableFrom(StateContext.class)) {
            args[i++] = stateContext;
        }
        if ((i < types.length) && types[i].isAssignableFrom(State.class)) {
            args[i++] = state;
        }

        invokeMethod(args);

        return true;
    }

    private void invokeMethod(Object[] arguments) {
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Executing method " + method + " with arguments " + Arrays.asList(arguments));
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

}
