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

import java.lang.reflect.Method;

import org.apache.mina.statemachine.StateMachineProxyBuilder;
import org.apache.mina.statemachine.context.StateContext;

/**
 * Used by {@link StateMachineProxyBuilder} to create {@link Event} objects when 
 * methods are invoked on the proxy.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface EventFactory
{
    /**
     * Creates a new {@link Event} from the specified method and method 
     * arguments.
     * 
     * @param context the current {@link StateContext}.
     * @param method the method being invoked.
     * @param args the method arguments.
     * @return the {@link Event} object.
     */
    Event create(StateContext context, Method method, Object[] arguments);
}
