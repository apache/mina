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
import org.apache.mina.statemachine.event.UnhandledEventException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to create proxies which will forward all method calls on them to a
 * {@link StateMachine}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class StateMachineProxyBuilder {
    private static final Logger log = LoggerFactory
            .getLogger(StateMachineProxyBuilder.class);

    private static final Object[] EMPTY_ARGUMENTS = new Object[0];

    private StateContextLookup contextLookup = new SingletonStateContextLookup();

    private EventFactory eventFactory = new DefaultEventFactory();

    private EventArgumentsInterceptor interceptor = null;

    private boolean ignoreUnhandledEvents = false;

    private boolean ignoreStateContextLookupFailure = false;

    // the classloader to use, if null, then will use the class local one
    private ClassLoader defaultCl = null; 

    public StateMachineProxyBuilder() {
    }

    public StateMachineProxyBuilder setStateContextLookup(
            StateContextLookup contextLookup) {
        this.contextLookup = contextLookup;
        return this;
    }

    public StateMachineProxyBuilder setEventFactory(EventFactory eventFactory) {
        this.eventFactory = eventFactory;
        return this;
    }

    public StateMachineProxyBuilder setEventArgumentsInterceptor(
            EventArgumentsInterceptor interceptor) {
        this.interceptor = interceptor;
        return this;
    }

    public StateMachineProxyBuilder setIgnoreUnhandledEvents(boolean b) {
        this.ignoreUnhandledEvents = b;
        return this;
    }

    public StateMachineProxyBuilder setIgnoreStateContextLookupFailure(boolean b) {
        this.ignoreStateContextLookupFailure = b;
        return this;
    }

    /**
     * Set the class loader to use for instanciate proxies.
     * @params cl the class loader
     * @return StateMachineProxyBuilder this for chaining 
     */
    public StateMachineProxyBuilder setClassLoader(ClassLoader cl) {
        this.defaultCl = cl;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> iface, StateMachine sm) {
        return (T) create(new Class[] { iface }, sm);
    }

    public Object create(Class<?>[] ifaces, StateMachine sm) {
	ClassLoader cl = defaultCl;
	if (cl == null) {
	   cl = Thread.currentThread().getContextClassLoader();
	}

        InvocationHandler handler = new MethodInvocationHandler(sm,
                contextLookup, interceptor, eventFactory,
                ignoreUnhandledEvents, ignoreStateContextLookupFailure);

        return Proxy.newProxyInstance(cl, ifaces, handler);
    }
    
    private static class MethodInvocationHandler implements InvocationHandler {
        private final StateMachine sm;
        private final StateContextLookup contextLookup;
        private final EventArgumentsInterceptor interceptor;
        private final EventFactory eventFactory;
        private final boolean ignoreUnhandledEvents;
        private final boolean ignoreStateContextLookupFailure;
        
        public MethodInvocationHandler(StateMachine sm, StateContextLookup contextLookup,
                EventArgumentsInterceptor interceptor, EventFactory eventFactory,
                boolean ignoreUnhandledEvents, boolean ignoreStateContextLookupFailure) {
            
            this.contextLookup = contextLookup;
            this.sm = sm;
            this.interceptor = interceptor;
            this.eventFactory = eventFactory;
            this.ignoreUnhandledEvents = ignoreUnhandledEvents;
            this.ignoreStateContextLookupFailure = ignoreStateContextLookupFailure;
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
                if (ignoreStateContextLookupFailure) {
                    return null;
                }
                throw new IllegalStateException("Cannot determine state "
                        + "context for method invocation: " + method);
            }

            Event event = eventFactory.create(context, method, args);

            try {
                sm.handle(event);
            } catch (UnhandledEventException uee) {
                if (!ignoreUnhandledEvents) {
                    throw uee;
                }
            }

            return null;
        }
    }
}
