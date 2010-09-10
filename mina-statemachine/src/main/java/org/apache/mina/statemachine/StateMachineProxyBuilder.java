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
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
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
    
    private String name = null;

    /*
     * The classloader to use. Iif null we will use the current thread's 
     * context classloader.
     */
    private ClassLoader defaultCl = null; 

    public StateMachineProxyBuilder() {
    }

    /**
     * Sets the name of the proxy created by this builder. This will be used 
     * by the proxies <code>toString()</code> method. If not specified a default
     * auto generated name will be used.
     * 
     * @param name the name.
     * @return this {@link StateMachineProxyBuilder} for method chaining.
     */
    public StateMachineProxyBuilder setName(String name) {
        this.name = name;
        return this;
    }
    
    /**
     * Sets the {@link StateContextLookup} to be used. The default is to use
     * a {@link SingletonStateContextLookup}.
     * 
     * @param contextLookup the {@link StateContextLookup} to use.
     * @return this {@link StateMachineProxyBuilder} for method chaining. 
     */
    public StateMachineProxyBuilder setStateContextLookup(
            StateContextLookup contextLookup) {
        this.contextLookup = contextLookup;
        return this;
    }

    /**
     * Sets the {@link EventFactory} to be used. The default is to use a 
     * {@link DefaultEventFactory}.
     * 
     * @param eventFactory the {@link EventFactory} to use.
     * @return this {@link StateMachineProxyBuilder} for method chaining.
     */
    public StateMachineProxyBuilder setEventFactory(EventFactory eventFactory) {
        this.eventFactory = eventFactory;
        return this;
    }

    /**
     * Sets the {@link EventArgumentsInterceptor} to be used. By default no
     * {@link EventArgumentsInterceptor} will be used.
     * 
     * @param interceptor the {@link EventArgumentsInterceptor} to use.
     * @return this {@link StateMachineProxyBuilder} for method chaining.
     */
    public StateMachineProxyBuilder setEventArgumentsInterceptor(
            EventArgumentsInterceptor interceptor) {
        this.interceptor = interceptor;
        return this;
    }

    /**
     * Sets whether events which have no handler in the current state will raise 
     * an exception or be silently ignored. The default is to raise an 
     * exception. 
     * 
     * @param b <code>true</code> to ignore context lookup failures.
     * @return this {@link StateMachineProxyBuilder} for method chaining. 
     */
    public StateMachineProxyBuilder setIgnoreUnhandledEvents(boolean b) {
        this.ignoreUnhandledEvents = b;
        return this;
    }

    /**
     * Sets whether the failure to lookup a {@link StateContext} corresponding
     * to a method call on the proxy produced by this builder will raise an
     * exception or be silently ignored. The default is to raise an exception.
     * 
     * @param b <code>true</code> to ignore context lookup failures.
     * @return this {@link StateMachineProxyBuilder} for method chaining. 
     */
    public StateMachineProxyBuilder setIgnoreStateContextLookupFailure(boolean b) {
        this.ignoreStateContextLookupFailure = b;
        return this;
    }

    /**
     * Sets the class loader to use for instantiating proxies. The default is
     * to use the current threads context {@link ClassLoader} as returned by 
     * {@link Thread#getContextClassLoader()}.
     * 
     * @param cl the class loader
     * @return this {@link StateMachineProxyBuilder} for method chaining. 
     */
    public StateMachineProxyBuilder setClassLoader(ClassLoader cl) {
        this.defaultCl = cl;
        return this;
    }

    /**
     * Creates a proxy for the specified interface and which uses the specified 
     * {@link StateMachine}.
     * 
     * @param iface the interface the proxy will implement.
     * @param sm the {@link StateMachine} which will receive the events 
     *        generated by the method calls on the proxy.
     * @return the proxy object.
     */
    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> iface, StateMachine sm) {
        return (T) create(new Class[] { iface }, sm);
    }

    /**
     * Creates a proxy for the specified interfaces and which uses the specified 
     * {@link StateMachine}.
     * 
     * @param ifaces the interfaces the proxy will implement.
     * @param sm the {@link StateMachine} which will receive the events 
     *        generated by the method calls on the proxy.
     * @return the proxy object.
     */
    public Object create(Class<?>[] ifaces, StateMachine sm) {
        ClassLoader cl = defaultCl;
        if (cl == null) {
           cl = Thread.currentThread().getContextClassLoader();
        }

        InvocationHandler handler = new MethodInvocationHandler(sm,
                contextLookup, interceptor, eventFactory,
                ignoreUnhandledEvents, ignoreStateContextLookupFailure, name);

        return Proxy.newProxyInstance(cl, ifaces, handler);
    }
    
    private static class MethodInvocationHandler implements InvocationHandler {
        private final StateMachine sm;
        private final StateContextLookup contextLookup;
        private final EventArgumentsInterceptor interceptor;
        private final EventFactory eventFactory;
        private final boolean ignoreUnhandledEvents;
        private final boolean ignoreStateContextLookupFailure;
        private final String name;
        
        public MethodInvocationHandler(StateMachine sm, StateContextLookup contextLookup,
                EventArgumentsInterceptor interceptor, EventFactory eventFactory,
                boolean ignoreUnhandledEvents, boolean ignoreStateContextLookupFailure, 
                String name) {
            
            this.contextLookup = contextLookup;
            this.sm = sm;
            this.interceptor = interceptor;
            this.eventFactory = eventFactory;
            this.ignoreUnhandledEvents = ignoreUnhandledEvents;
            this.ignoreStateContextLookupFailure = ignoreStateContextLookupFailure;
            this.name = name;
        }
        
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("hashCode".equals(method.getName()) && args == null) {
                return new Integer(System.identityHashCode(proxy));
            }
            if ("equals".equals(method.getName()) && args.length == 1) {
                return Boolean.valueOf(proxy == args[0]);
            }
            if ("toString".equals(method.getName()) && args == null) {
                return (name != null ? name : proxy.getClass().getName()) + "@" 
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
