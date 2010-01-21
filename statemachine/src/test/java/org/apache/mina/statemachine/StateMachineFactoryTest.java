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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.mina.statemachine.annotation.Transition;
import org.apache.mina.statemachine.annotation.Transitions;
import org.apache.mina.statemachine.transition.MethodTransition;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link StateMachineFactory}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class StateMachineFactoryTest {
    Method barInA;
    Method error;
    Method fooInA;
    Method fooInB;
    Method barInC;
    Method fooOrBarInCOrFooInD;

    @Before
    public void setUp() throws Exception {
        barInA = States.class.getDeclaredMethod("barInA", new Class[0]);
        error = States.class.getDeclaredMethod("error", new Class[0]);
        fooInA = States.class.getDeclaredMethod("fooInA", new Class[0]);
        fooInB = States.class.getDeclaredMethod("fooInB", new Class[0]);
        barInC = States.class.getDeclaredMethod("barInC", new Class[0]);
        fooOrBarInCOrFooInD = States.class.getDeclaredMethod("fooOrBarInCOrFooInD", new Class[0]);
    }

    @Test
    public void testCreate() throws Exception {
        States states = new States();
        StateMachine sm = StateMachineFactory.getInstance(Transition.class).create(States.A, states);

        State a = sm.getState(States.A);
        State b = sm.getState(States.B);
        State c = sm.getState(States.C);
        State d = sm.getState(States.D);

        assertEquals(States.A, a.getId());
        assertNull(a.getParent());
        assertEquals(States.B, b.getId());
        assertSame(a, b.getParent());
        assertEquals(States.C, c.getId());
        assertSame(b, c.getParent());
        assertEquals(States.D, d.getId());
        assertSame(a, d.getParent());

        List<org.apache.mina.statemachine.transition.Transition> trans = null;

        trans = a.getTransitions();
        assertEquals(3, trans.size());
        assertEquals(new MethodTransition("bar", barInA, states), trans.get(0));
        assertEquals(new MethodTransition("*", error, states), trans.get(1));
        assertEquals(new MethodTransition("foo", b, fooInA, states), trans.get(2));
        
        trans = b.getTransitions();
        assertEquals(1, trans.size());
        assertEquals(new MethodTransition("foo", c, fooInB, states), trans.get(0));

        trans = c.getTransitions();
        assertEquals(3, trans.size());
        assertEquals(new MethodTransition("bar", a, barInC, states), trans.get(0));
        assertEquals(new MethodTransition("foo", d, fooOrBarInCOrFooInD, states), trans.get(1));
        assertEquals(new MethodTransition("bar", d, fooOrBarInCOrFooInD, states), trans.get(2));

        trans = d.getTransitions();
        assertEquals(1, trans.size());
        assertEquals(new MethodTransition("foo", fooOrBarInCOrFooInD, states), trans.get(0));
    }
    
    @Test
    public void testCreateStates() throws Exception {
        State[] states = StateMachineFactory.createStates(StateMachineFactory.getFields(States.class));
        assertEquals(States.A, states[0].getId());
        assertNull(states[0].getParent());
        assertEquals(States.B, states[1].getId());
        assertEquals(states[0], states[1].getParent());
        assertEquals(States.C, states[2].getId());
        assertEquals(states[1], states[2].getParent());
        assertEquals(States.D, states[3].getId());
        assertEquals(states[0], states[3].getParent());
    }
    
    @Test
    public void testCreateStatesMissingParents() throws Exception {
        try {
            StateMachineFactory.createStates(StateMachineFactory.getFields(StatesWithMissingParents.class));
            fail("Missing parents. FsmCreationException expected.");
        } catch (StateMachineCreationException fce) {
        }
    }
    
    public static class States {
        @org.apache.mina.statemachine.annotation.State
        protected static final String A = "a";
        @org.apache.mina.statemachine.annotation.State(A)
        protected static final String B = "b";
        @org.apache.mina.statemachine.annotation.State(B)
        protected static final String C = "c";
        @org.apache.mina.statemachine.annotation.State(A)
        protected static final String D = "d";
        
        @Transition(on = "bar", in = A)
        protected void barInA() {
        }

        @Transition(on = "bar", in = C, next = A)
        protected void barInC() {
        }

        @Transition(in = A)
        protected void error() {
        }

        @Transition(on = "foo", in = A, next = B)
        protected void fooInA() {
        }

        @Transition(on = "foo", in = B, next = C)
        protected void fooInB() {
        }

        @Transitions( { @Transition(on = { "foo", "bar" }, in = C, next = D), @Transition(on = "foo", in = D) })
        protected void fooOrBarInCOrFooInD() {
        }
      
    }
    
    public static class StatesWithMissingParents {
        @org.apache.mina.statemachine.annotation.State("b")
        public static final String A = "a";
        @org.apache.mina.statemachine.annotation.State("c")
        public static final String B = "b";
        @org.apache.mina.statemachine.annotation.State("d")
        public static final String C = "c";
        @org.apache.mina.statemachine.annotation.State("e")
        public static final String D = "d";
    }
}
