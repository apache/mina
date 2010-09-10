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

import java.lang.reflect.Method;

/**
 * Thrown by {@link MethodTransition} if the target method couldn't be invoked
 * or threw an exception.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class MethodInvocationException extends RuntimeException {
    private static final long serialVersionUID = 4288548621384649704L;

    /**
     * Creates a new instance for the specified {@link Method} and 
     * {@link Throwable}.
     * 
     * @param method the {@link Method}.
     * @param cause the reason.
     */
    public MethodInvocationException(Method method, Throwable cause) {
        super("Invoking method: " + method, cause);
    }

}
