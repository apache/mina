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
import static org.junit.Assert.assertSame;

import org.apache.mina.statemachine.context.DefaultStateContext;
import org.apache.mina.statemachine.context.StateContext;
import org.apache.mina.statemachine.event.Event;
import org.apache.mina.statemachine.transition.AbstractTransition;
import org.junit.Test;

/**
 * Tests {@link StateMachine}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class StateMachineTest {

    @Test
    public void testBreakAndContinue() throws Exception {
        State s1 = new State("s1");
        s1.addTransition(new BreakAndContinueTransition("foo"));
        s1.addTransition(new SuccessTransition("foo"));

        StateContext context = new DefaultStateContext();
        StateMachine sm = new StateMachine(new State[] { s1 }, "s1");
        sm.handle(new Event("foo", context));
        assertEquals(true, context.getAttribute("success"));
    }
    
    @Test
    public void testBreakAndGotoNow() throws Exception {
        State s1 = new State("s1");
        State s2 = new State("s2");
        s1.addTransition(new BreakAndGotoNowTransition("foo", "s2"));
        s2.addTransition(new SuccessTransition("foo"));

        StateContext context = new DefaultStateContext();
        StateMachine sm = new StateMachine(new State[] { s1, s2 }, "s1");
        sm.handle(new Event("foo", context));
        assertEquals(true, context.getAttribute("success"));
    }
    
    @Test
    public void testBreakAndGotoNext() throws Exception {
        State s1 = new State("s1");
        State s2 = new State("s2");
        s1.addTransition(new BreakAndGotoNextTransition("foo", "s2"));
        s2.addTransition(new SuccessTransition("foo"));

        StateContext context = new DefaultStateContext();
        StateMachine sm = new StateMachine(new State[] { s1, s2 }, "s1");
        sm.handle(new Event("foo", context));
        assertSame(s2, context.getCurrentState());
        sm.handle(new Event("foo", context));
        assertEquals(true, context.getAttribute("success"));
    }

    private static class SuccessTransition extends AbstractTransition {
        public SuccessTransition(Object eventId) {
            super(eventId);
        }

        public SuccessTransition(Object eventId, State nextState) {
            super(eventId, nextState);
        }

        @Override
        protected boolean doExecute(Event event) {
            event.getContext().setAttribute("success", true);
            return true;
        }
    }
    
    private static class BreakAndContinueTransition extends AbstractTransition {
        public BreakAndContinueTransition(Object eventId) {
            super(eventId);
        }

        public BreakAndContinueTransition(Object eventId, State nextState) {
            super(eventId, nextState);
        }

        @Override
        protected boolean doExecute(Event event) {
            StateControl.breakAndContinue();
            return true;
        }
    }
    
    private static class BreakAndGotoNowTransition extends AbstractTransition {
        private final String stateId;

        public BreakAndGotoNowTransition(Object eventId, String stateId) {
            super(eventId);
            this.stateId = stateId;
        }

        public BreakAndGotoNowTransition(Object eventId, State nextState, String stateId) {
            super(eventId, nextState);
            this.stateId = stateId;
        }

        @Override
        protected boolean doExecute(Event event) {
            StateControl.breakAndGotoNow(stateId);
            return true;
        }
    }

    private static class BreakAndGotoNextTransition extends AbstractTransition {
        private final String stateId;

        public BreakAndGotoNextTransition(Object eventId, String stateId) {
            super(eventId);
            this.stateId = stateId;
        }

        public BreakAndGotoNextTransition(Object eventId, State nextState, String stateId) {
            super(eventId, nextState);
            this.stateId = stateId;
        }

        @Override
        protected boolean doExecute(Event event) {
            StateControl.breakAndGotoNext(stateId);
            return true;
        }
    }
}
