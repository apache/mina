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

import org.apache.mina.statemachine.State;
import org.apache.mina.statemachine.context.StateContext;
import org.apache.mina.statemachine.event.Event;
import org.apache.mina.statemachine.transition.MethodTransition;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * Tests {@link MethodTransition}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class MethodTransitionTest extends RMockTestCase {
    State currentState;
    State nextState;
    TestStateContext context;
    Target target;
    Method subsetAllArgsMethod1;
    Method subsetAllArgsMethod2;
    Event noArgsEvent;
    Event argsEvent;
    Object[] args;
    
    protected void setUp() throws Exception {
        super.setUp();
        
        currentState = new State( "current" );
        nextState = new State( "next" );
        target = (Target) mock(Target.class);
        subsetAllArgsMethod1 = Target.class.getMethod("subsetAllArgs", new Class[] {
                TestStateContext.class, B.class, A.class, Integer.TYPE
        });
        subsetAllArgsMethod2 = Target.class.getMethod("subsetAllArgs", new Class[] {
                Event.class, B.class, B.class, Boolean.TYPE
        });
        
        args = new Object[] { new A(), new B(), new C(), new Integer(627438), Boolean.TRUE };
        context = (TestStateContext) mock(TestStateContext.class);
        noArgsEvent = new Event("event", context, new Object[0]);
        argsEvent = new Event("event", context, args);
    }

    public void testExecuteWrongEventId() throws Exception {
        startVerification();
        MethodTransition t = new MethodTransition("otherEvent", nextState, "noArgs", target);
        assertFalse(t.execute(noArgsEvent));
    }
    
    public void testExecuteNoArgsMethodOnNoArgsEvent() throws Exception {
        target.noArgs();
        startVerification();
        MethodTransition t = new MethodTransition("event", nextState, "noArgs", target);
        assertTrue(t.execute(noArgsEvent));
    }
    
    public void testExecuteNoArgsMethodOnArgsEvent() throws Exception {
        target.noArgs();
        startVerification();
        MethodTransition t = new MethodTransition("event", nextState, "noArgs", target);
        assertTrue(t.execute(argsEvent));
    }
    
    public void testExecuteExactArgsMethodOnNoArgsEvent() throws Exception {
        startVerification();
        MethodTransition t = new MethodTransition("event", nextState, "exactArgs", target);
        assertFalse(t.execute(noArgsEvent));
    }
    
    public void testExecuteExactArgsMethodOnArgsEvent() throws Exception {
        target.exactArgs((A) args[0], (B) args[1], (C) args[2], 
                         ((Integer) args[3]).intValue(), ((Boolean) args[4]).booleanValue());
        startVerification();
        MethodTransition t = new MethodTransition("event", nextState, "exactArgs", target);
        assertTrue(t.execute(argsEvent));
    }
    
    public void testExecuteSubsetExactArgsMethodOnNoArgsEvent() throws Exception {
        startVerification();
        MethodTransition t = new MethodTransition("event", nextState, "subsetExactArgs", target);
        assertFalse(t.execute(noArgsEvent));
    }
    
    public void testExecuteSubsetExactArgsMethodOnArgsEvent() throws Exception {
        target.subsetExactArgs((A) args[0], (A) args[1], ((Integer) args[3]).intValue());
        startVerification();
        MethodTransition t = new MethodTransition("event", nextState, "subsetExactArgs", target);
        assertTrue(t.execute(argsEvent));
    }
    
    public void testExecuteAllArgsMethodOnArgsEvent() throws Exception {
        target.allArgs(argsEvent, context, (A) args[0], (B) args[1], (C) args[2], 
                ((Integer) args[3]).intValue(), ((Boolean) args[4]).booleanValue());
        startVerification();
        MethodTransition t = new MethodTransition("event", nextState, "allArgs", target);
        assertTrue(t.execute(argsEvent));
    }
    
    public void testExecuteSubsetAllArgsMethod1OnArgsEvent() throws Exception {
        target.subsetAllArgs(context, (B) args[1], (A) args[2], ((Integer) args[3]).intValue());
        startVerification();
        MethodTransition t = new MethodTransition("event", nextState, subsetAllArgsMethod1, target);
        assertTrue(t.execute(argsEvent));
    }
    
    public void testExecuteSubsetAllArgsMethod2OnArgsEvent() throws Exception {
        target.subsetAllArgs(argsEvent, (B) args[1], (B) args[2], ((Boolean) args[4]).booleanValue());
        startVerification();
        MethodTransition t = new MethodTransition("event", nextState, subsetAllArgsMethod2, target);
        assertTrue(t.execute(argsEvent));
    }
    
    public interface Target {
        void noArgs();
        void exactArgs(A a, B b, C c, int integer, boolean bool);
        void allArgs(Event event, StateContext ctxt, A a, B b, C c, int integer, boolean bool);
        void subsetExactArgs(A a, A b, int integer);
        void subsetAllArgs(TestStateContext ctxt, B b, A c, int integer);
        void subsetAllArgs(Event event, B b, B c, boolean bool);
    }
    
    public interface TestStateContext extends StateContext {}
    
    public static class A {}
    public static class B extends A {}
    public static class C extends B {}
}
