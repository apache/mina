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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.mina.statemachine.context.SingletonStateContextLookup;
import org.apache.mina.statemachine.context.StateContext;
import org.apache.mina.statemachine.context.StateContextLookup;
import org.apache.mina.statemachine.event.DefaultEventFactory;
import org.apache.mina.statemachine.event.Event;
import org.apache.mina.statemachine.event.EventArgumentsInterceptor;
import org.apache.mina.statemachine.event.EventFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to create proxies which will forward all method calls on them to a
 * {@link StateMachine}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class StateMachineProxyFactory {
    private static final Logger log = LoggerFactory.getLogger(StateMachineProxyFactory.class);
    
    private static final Object[] EMPTY_ARGUMENTS = new Object[0];

    private StateMachineProxyFactory() {
    }

    public static Object create(Class iface, StateMachine sm) {
        return create(new Class[] { iface }, sm);
    }
    
    public static Object create(Class iface, StateMachine sm, StateContextLookup contextLookup) {
        return create(new Class[] { iface }, sm, contextLookup);
    }
    
    public static Object create(Class iface, StateMachine sm, StateContextLookup contextLookup,
            EventArgumentsInterceptor interceptor) {
        return create(new Class[] { iface }, sm, contextLookup, interceptor, new DefaultEventFactory());
    }

    public static Object create(Class iface, StateMachine sm, StateContextLookup contextLookup,
            EventArgumentsInterceptor interceptor, EventFactory eventFactory) {
        return create(new Class[] { iface }, sm, contextLookup, interceptor, eventFactory);
    }
    
    public static Object create(Class[] ifaces, StateMachine sm) {
        return create(ifaces, sm, new SingletonStateContextLookup());
    }
    
    public static Object create(Class[] ifaces, StateMachine sm, StateContextLookup contextLookup) {

        return create(ifaces, sm, contextLookup, null, new DefaultEventFactory());
    }
    
    public static Object create(Class[] ifaces, StateMachine sm, StateContextLookup contextLookup,
            EventArgumentsInterceptor interceptor, EventFactory eventFactory) {

        ClassLoader cl = StateMachineProxyFactory.class.getClassLoader();
        InvocationHandler handler = new MethodInvocationHandler(sm, contextLookup, interceptor, eventFactory);

        return Proxy.newProxyInstance(cl, ifaces, handler);
    }
    
    private static class MethodInvocationHandler implements InvocationHandler {
        private final StateMachine sm;
        private final StateContextLookup contextLookup;
        private final EventArgumentsInterceptor interceptor;
        private final EventFactory eventFactory;
        
        public MethodInvocationHandler(StateMachine sm, StateContextLookup contextLookup,
                EventArgumentsInterceptor interceptor, EventFactory eventFactory) {
            
            this.contextLookup = contextLookup;
            this.sm = sm;
            this.interceptor = interceptor;
            this.eventFactory = eventFactory;
        }
        
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("hashCode".equals(method.getName()) && args == null) {
                return new Integer(System.identityHashCode(proxy));
            }
            if ("equals".equals(method.getName()) && args.length == 1) {
                return Boolean.valueOf(proxy == args[0]);
            }
            if ("toString".equals(method.getName()) && args == null) {
                return proxy.getClass().getName() + "@" 
                        + Integer.toHexString(System.identityHashCode(proxy));
            }

            if (log.isDebugEnabled()) {
                log.debug("Method invoked: " + method);
            }

            args = args == null ? EMPTY_ARGUMENTS : args;
            if (interceptor != null) {
                args = interceptor.modify(args);
            }

            StateContext context = contextLookup.lookup(args);

            if (context == null) {
                throw new IllegalStateException("Cannot determine state " 
                        + "context for method invocation: " + method);
            }

            Event event = eventFactory.create(context, method, args);

            sm.handle(event);

            return null;
        }
    }
}
