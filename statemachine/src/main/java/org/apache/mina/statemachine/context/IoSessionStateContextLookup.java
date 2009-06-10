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

import org.apache.mina.core.session.IoSession;

/**
 * MINA specific {@link StateContextLookup} which uses an {@link IoSession}
 * attribute to store the {@link StateContextLookup}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class IoSessionStateContextLookup extends AbstractStateContextLookup {
    /**
     * The default name of the {@link IoSession} attribute used to store the
     * {@link StateContext} object.
     */
    public static final String DEFAULT_SESSION_ATTRIBUTE_NAME = 
        IoSessionStateContextLookup.class.getName() + ".stateContext";
    
    private final String sessionAttributeName;
    
    /**
     * Creates a new instance using a {@link DefaultStateContextFactory} to
     * create {@link StateContext} objects for new {@link IoSession}s.
     */
    public IoSessionStateContextLookup() {
        this(new DefaultStateContextFactory(), DEFAULT_SESSION_ATTRIBUTE_NAME);
    }

    /**
     * Creates a new instance using a {@link DefaultStateContextFactory} to
     * create {@link StateContext} objects for new {@link IoSession}s.
     * 
     * @param sessionAttributeName the name of the {@link IoSession} attribute
     *        used to store the {@link StateContext} object.
     */
    public IoSessionStateContextLookup(String sessionAttributeName) {
        this(new DefaultStateContextFactory(), sessionAttributeName);
    }
    
    /**
     * Creates a new instance using the specified {@link StateContextFactory} to
     * create {@link StateContext} objects for new {@link IoSession}s.
     * 
     * @param contextFactory the {@link StateContextFactory}.
     */
    public IoSessionStateContextLookup(StateContextFactory contextFactory) {
        this(contextFactory, DEFAULT_SESSION_ATTRIBUTE_NAME);
    }

    /**
     * Creates a new instance using the specified {@link StateContextFactory} to
     * create {@link StateContext} objects for new {@link IoSession}s.
     * 
     * @param contextFactory the {@link StateContextFactory}.
     * @param sessionAttributeName the name of the {@link IoSession} attribute
     *        used to store the {@link StateContext} object.
     */
    public IoSessionStateContextLookup(StateContextFactory contextFactory, String sessionAttributeName) {
        super(contextFactory);
        this.sessionAttributeName = sessionAttributeName;
    }
    
    protected StateContext lookup(Object eventArg) {
        IoSession session = (IoSession) eventArg;
        return (StateContext) session.getAttribute(sessionAttributeName);
    }

    protected void store(Object eventArg, StateContext context) {
        IoSession session = (IoSession) eventArg;
        session.setAttribute(sessionAttributeName, context);
    }

    protected boolean supports(Class<?> c) {
        return IoSession.class.isAssignableFrom(c);
    }
}
