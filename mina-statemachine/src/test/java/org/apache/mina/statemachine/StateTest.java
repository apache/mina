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

import org.apache.mina.statemachine.State;
import org.apache.mina.statemachine.transition.Transition;
import org.junit.BeforeClass;
import org.junit.Test;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * Tests {@link State}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class StateTest extends RMockTestCase {
    State state;
    Transition transition1;
    Transition transition2;
    Transition transition3;

    @BeforeClass
    protected void setUp() throws Exception {
        state = new State("test");
        transition1 = (Transition) mock(Transition.class);
        transition2 = transition1; //(Transition) mock(Transition.class);
        transition3 = transition1; //(Transition) mock(Transition.class);
    }

    @Test
    public void testAddFirstTransition() throws Exception {
        assertTrue(state.getTransitions().isEmpty());
        state.addTransition(transition1);
        assertFalse(state.getTransitions().isEmpty());
        assertEquals(1, state.getTransitions().size());
        assertSame(transition1, state.getTransitions().get(0));
    }

    @Test
    public void testUnweightedTransitions() throws Exception {
        assertTrue(state.getTransitions().isEmpty());
        state.addTransition(transition1);
        state.addTransition(transition2);
        state.addTransition(transition3);
        assertEquals(3, state.getTransitions().size());
        assertSame(transition1, state.getTransitions().get(0));
        assertSame(transition2, state.getTransitions().get(1));
        assertSame(transition3, state.getTransitions().get(2));
    }
    
    @Test
    public void testWeightedTransitions() throws Exception {
        assertTrue(state.getTransitions().isEmpty());
        state.addTransition(transition1, 10);
        state.addTransition(transition2, 5);
        state.addTransition(transition3, 7);
        assertEquals(3, state.getTransitions().size());
        assertSame(transition2, state.getTransitions().get(0));
        assertSame(transition3, state.getTransitions().get(1));
        assertSame(transition1, state.getTransitions().get(2));
    }

    @Test
    public void testAddTransitionReturnsSelf() throws Exception {
        assertSame(state, state.addTransition(transition1));
    }

    @Test
    public void testAddNullTransitionThrowsException() throws Exception {
        try {
            state.addTransition(null);
            fail("null transition added. IllegalArgumentException expected.");
        } catch (IllegalArgumentException npe) {
        }
    }
    
}
