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
package org.apache.mina.example.tapedeck;

import static org.apache.mina.statemachine.event.IoHandlerEvents.*;

import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.statemachine.StateControl;
import org.apache.mina.statemachine.annotation.IoHandlerTransition;
import org.apache.mina.statemachine.annotation.IoHandlerTransitions;
import org.apache.mina.statemachine.annotation.State;
import org.apache.mina.statemachine.context.AbstractStateContext;
import org.apache.mina.statemachine.context.StateContext;
import org.apache.mina.statemachine.event.Event;

/**
 * The actual state machine implementation for the tape deck server.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class TapeDeckServer {
    @State public static final String ROOT = "Root";
    @State(ROOT) public static final String EMPTY = "Empty";
    @State(ROOT) public static final String LOADED = "Loaded";
    @State(ROOT) public static final String PLAYING = "Playing";
    @State(ROOT) public static final String PAUSED = "Paused";
    
    private final String[] tapes = {
            "The Knife - Silent Shout", 
            "Kings of convenience - Riot on an empty street"
    };
    
    static class TapeDeckContext extends AbstractStateContext {
        public String tapeName;
    }
    
    @IoHandlerTransition(on = SESSION_OPENED, in = EMPTY)
    public void connect(IoSession session) {
        session.write("+ Greetings from your tape deck!");
    }
    
    @IoHandlerTransition(on = MESSAGE_RECEIVED, in = EMPTY, next = LOADED)
    public void loadTape(TapeDeckContext context, IoSession session, LoadCommand cmd) {
        if (cmd.getTapeNumber() < 1 || cmd.getTapeNumber() > tapes.length) {
            session.write("- Unknown tape number: " + cmd.getTapeNumber());
            StateControl.breakAndGotoNext(EMPTY);
        } else {
            context.tapeName = tapes[cmd.getTapeNumber() - 1];
            session.write("+ \"" + context.tapeName + "\" loaded");
        }
    }

    @IoHandlerTransitions({
        @IoHandlerTransition(on = MESSAGE_RECEIVED, in = LOADED, next = PLAYING),
        @IoHandlerTransition(on = MESSAGE_RECEIVED, in = PAUSED, next = PLAYING)
    })
    public void playTape(TapeDeckContext context, IoSession session, PlayCommand cmd) {
        session.write("+ Playing \"" + context.tapeName + "\"");
    }
    
    @IoHandlerTransition(on = MESSAGE_RECEIVED, in = PLAYING, next = PAUSED)
    public void pauseTape(TapeDeckContext context, IoSession session, PauseCommand cmd) {
        session.write("+ \"" + context.tapeName + "\" paused");
    }
    
    @IoHandlerTransition(on = MESSAGE_RECEIVED, in = PLAYING, next = LOADED)
    public void stopTape(TapeDeckContext context, IoSession session, StopCommand cmd) {
        session.write("+ \"" + context.tapeName + "\" stopped");
    }
    
    @IoHandlerTransition(on = MESSAGE_RECEIVED, in = LOADED, next = EMPTY)
    public void ejectTape(TapeDeckContext context, IoSession session, EjectCommand cmd) {
        session.write("+ \"" + context.tapeName + "\" ejected");
        context.tapeName = null;
    }
    
    @IoHandlerTransition(on = MESSAGE_RECEIVED, in = ROOT)
    public void listTapes(IoSession session, ListCommand cmd) {
        StringBuilder response = new StringBuilder("+ (");
        for (int i = 0; i < tapes.length; i++) {
            response.append(i + 1).append(": ");
            response.append('"').append(tapes[i]).append('"');
            if (i < tapes.length - 1) {
                response.append(", ");
            }
        }
        response.append(')');
        session.write(response);
    }
    
    @IoHandlerTransition(on = MESSAGE_RECEIVED, in = ROOT)
    public void info(TapeDeckContext context, IoSession session, InfoCommand cmd) {
        String state = context.getCurrentState().getId().toLowerCase();
        if (context.tapeName == null) {
            session.write("+ Tape deck is " + state + "");
        } else {
            session.write("+ Tape deck is " + state 
                    + ". Current tape: \"" + context.tapeName + "\"");
        }
    }
    
    @IoHandlerTransition(on = MESSAGE_RECEIVED, in = ROOT)
    public void quit(TapeDeckContext context, IoSession session, QuitCommand cmd) {
        session.write("+ Bye! Please come back!").addListener(IoFutureListener.CLOSE);
    }
    
    @IoHandlerTransition(on = MESSAGE_RECEIVED, in = ROOT, weight = 10)
    public void error(Event event, StateContext context, IoSession session, Command cmd) {
        session.write("- Cannot " + cmd.getName() 
                + " while " + context.getCurrentState().getId().toLowerCase());
    }
    
    @IoHandlerTransition(on = EXCEPTION_CAUGHT, in = ROOT)
    public void commandSyntaxError(IoSession session, CommandSyntaxException e) {
        session.write("- " + e.getMessage());
    }
    
    @IoHandlerTransition(on = EXCEPTION_CAUGHT, in = ROOT, weight = 10)
    public void exceptionCaught(IoSession session, Exception e) {
        e.printStackTrace();
        session.close(true);
    }
    
    @IoHandlerTransition(in = ROOT, weight = 100)
    public void unhandledEvent() {
    }
    
}
