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
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;

import org.apache.mina.statemachine.annotation.Transition;
import org.apache.mina.statemachine.annotation.Transitions;
import org.apache.mina.statemachine.event.Event;
import org.apache.mina.statemachine.transition.MethodTransition;
import org.junit.Test;

/**
 * Tests {@link StateMachineProxyBuilder}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class StateMachineProxyBuilderTest {
    @Test
    public void testReentrantStateMachine() throws Exception {
        ReentrantStateMachineHandler handler = new ReentrantStateMachineHandler();

        State s1 = new State("s1");
        State s2 = new State("s2");
        State s3 = new State("s3");

        s1.addTransition(new MethodTransition("call1", s2, handler));
        s2.addTransition(new MethodTransition("call2", s3, handler));
        s3.addTransition(new MethodTransition("call3", handler));

        StateMachine sm = new StateMachine(new State[] { s1, s2, s3 }, "s1");
        Reentrant reentrant = new StateMachineProxyBuilder().create(Reentrant.class, sm);
        reentrant.call1(reentrant);
        assertTrue(handler.finished);
    }
    
    @Test
    public void testTapeDeckStateMachine() throws Exception {
        TapeDeckStateMachineHandler handler = new TapeDeckStateMachineHandler();

        State parent = new State("parent");
        State s1 = new State("s1", parent);
        State s2 = new State("s2", parent);
        State s3 = new State("s3", parent);
        State s4 = new State("s4", parent);
        State s5 = new State("s5", parent);

        parent.addTransition(new MethodTransition("*", "error", handler));
        s1.addTransition(new MethodTransition("insert", s2, "inserted", handler));
        s2.addTransition(new MethodTransition("start", s3, "playing", handler));
        s3.addTransition(new MethodTransition("stop", s4, "stopped", handler));
        s3.addTransition(new MethodTransition("pause", s5, "paused", handler));
        s4.addTransition(new MethodTransition("eject", s1, "ejected", handler));
        s5.addTransition(new MethodTransition("pause", s3, "playing", handler));

        StateMachine sm = new StateMachine(new State[] { s1, s2, s3, s4, s5 }, "s1");
        TapeDeck player = new StateMachineProxyBuilder().create(TapeDeck.class, sm);
        player.insert("Kings of convenience - Riot on an empty street");
        player.start();
        player.pause();
        player.pause();
        player.eject();
        player.stop();
        player.eject();

        LinkedList<String> messages = handler.messages;
        assertEquals("Tape 'Kings of convenience - Riot on an empty street' inserted", messages.removeFirst());
        assertEquals("Playing", messages.removeFirst());
        assertEquals("Paused", messages.removeFirst());
        assertEquals("Playing", messages.removeFirst());
        assertEquals("Error: Cannot eject at this time", messages.removeFirst());
        assertEquals("Stopped", messages.removeFirst());
        assertEquals("Tape ejected", messages.removeFirst());
        assertTrue(messages.isEmpty());
    }
    
    @Test
    public void testTapeDeckStateMachineAnnotations() throws Exception {
        TapeDeckStateMachineHandler handler = new TapeDeckStateMachineHandler();

        StateMachine sm = StateMachineFactory.getInstance(Transition.class).create(TapeDeckStateMachineHandler.S1, handler);

        TapeDeck player = new StateMachineProxyBuilder().create(TapeDeck.class, sm);
        player.insert("Kings of convenience - Riot on an empty street");
        player.start();
        player.pause();
        player.pause();
        player.eject();
        player.stop();
        player.eject();

        LinkedList<String> messages = handler.messages;
        assertEquals("Tape 'Kings of convenience - Riot on an empty street' inserted", messages.removeFirst());
        assertEquals("Playing", messages.removeFirst());
        assertEquals("Paused", messages.removeFirst());
        assertEquals("Playing", messages.removeFirst());
        assertEquals("Error: Cannot eject at this time", messages.removeFirst());
        assertEquals("Stopped", messages.removeFirst());
        assertEquals("Tape ejected", messages.removeFirst());
        assertTrue(messages.isEmpty());
    }
    
    public interface Reentrant {
        void call1(Reentrant proxy);
        void call2(Reentrant proxy);
        void call3(Reentrant proxy);
    }

    public static class ReentrantStateMachineHandler {
        private boolean finished = false;

        public void call1(Reentrant proxy) {
            proxy.call2(proxy);
        }

        public void call2(Reentrant proxy) {
            proxy.call3(proxy);
        }

        public void call3(Reentrant proxy) {
            finished = true;
        }
    }

    public interface TapeDeck {
        void insert(String name);
        void eject();
        void start();
        void pause();
        void stop();
    }
    
    public static class TapeDeckStateMachineHandler {
        @org.apache.mina.statemachine.annotation.State public static final String PARENT = "parent";
        @org.apache.mina.statemachine.annotation.State(PARENT) public static final String S1 = "s1";
        @org.apache.mina.statemachine.annotation.State(PARENT) public static final String S2 = "s2";
        @org.apache.mina.statemachine.annotation.State(PARENT) public static final String S3 = "s3";
        @org.apache.mina.statemachine.annotation.State(PARENT) public static final String S4 = "s4";
        @org.apache.mina.statemachine.annotation.State(PARENT) public static final String S5 = "s5";
        
        private LinkedList<String> messages = new LinkedList<String>();
        
        @Transition(on = "insert", in = "s1", next = "s2")
        public void inserted(String name) {
            messages.add("Tape '" + name + "' inserted");
        }

        @Transition(on = "eject", in = "s4", next = "s1")
        public void ejected() {
            messages.add("Tape ejected");
        }
        
        @Transitions({@Transition( on = "start", in = "s2", next = "s3" ),
                   @Transition( on = "pause", in = "s5", next = "s3" )})
        public void playing() {
            messages.add("Playing");
        }
        
        @Transition(on = "pause", in = "s3", next = "s5")
        public void paused() {
            messages.add("Paused");
        }

        @Transition(on = "stop", in = "s3", next = "s4")
        public void stopped() {
            messages.add("Stopped");
        }

        @Transition(on = "*", in = "parent")
        public void error(Event event) {
            messages.add("Error: Cannot " + event.getId() + " at this time");
        }
    }
}
