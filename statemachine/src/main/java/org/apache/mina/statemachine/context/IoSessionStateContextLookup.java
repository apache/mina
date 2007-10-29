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
package org.apache.mina.statemachine.context;

import org.apache.mina.common.IoSession;

/**
 * MINA specific {@link StateContextLookup} which uses an {@link IoSession}
 * attribute to store the {@link StateContextLookup}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class IoSessionStateContextLookup extends AbstractStateContextLookup {
    /**
     * The name of the {@link IoSession} attribute used to store the
     * {@link StateContext} object.
     */
    public static final String STATE_CONTEXT = 
        IoSessionStateContextLookup.class.getName() + ".stateContext";
    
    /**
     * Creates a new instance using a {@link DefaultStateContextFactory} to
     * create {@link StateContext} objects for new {@link IoSession}s.
     */
    public IoSessionStateContextLookup() {
        this(new DefaultStateContextFactory());
    }

    /**
     * Creates a new instance using the specified {@link StateContextFactory} to
     * create {@link StateContext} objects for new {@link IoSession}s.
     * 
     * @param contextFactory the {@link StateContextFactory}.
     */
    public IoSessionStateContextLookup(StateContextFactory contextFactory) {
        super(contextFactory);
    }

    protected StateContext lookup(Object eventArg) {
        IoSession session = (IoSession) eventArg;
        return (StateContext) session.getAttribute(STATE_CONTEXT);
    }

    protected void store(Object eventArg, StateContext context) {
        IoSession session = (IoSession) eventArg;
        session.setAttribute(STATE_CONTEXT, context);
    }

    protected boolean supports(Class<?> c) {
        return IoSession.class.isAssignableFrom(c);
    }
}
