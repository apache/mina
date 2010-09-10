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

import static org.apache.mina.statemachine.event.IoFilterEvents.*;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.example.tapedeck.TapeDeckServer.TapeDeckContext;
import org.apache.mina.statemachine.StateControl;
import org.apache.mina.statemachine.annotation.IoFilterTransition;
import org.apache.mina.statemachine.annotation.State;
import org.apache.mina.statemachine.context.AbstractStateContext;

/**
 * TODO Add documentation
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class AuthenticationHandler {
    @State public static final String ROOT = "Root";
    @State(ROOT) public static final String START = "Start";
    @State(ROOT) public static final String WAIT_USER = "WaitUser";
    @State(ROOT) public static final String WAIT_PASSWORD = "WaitPassword";
    @State(ROOT) public static final String DONE = "Done";
    @State(ROOT) public static final String FAILED = "Failed";

    static class AuthenticationContext extends AbstractStateContext {
        public String user;
        public String password;
        public int tries = 0;
    }

    @IoFilterTransition(on = SESSION_OPENED, in = START, next = WAIT_USER)
    public void sendAuthRequest(NextFilter nextFilter, IoSession session) {
        session.write("+ Greetings from your tape deck!. Please authenticate yourself.");
    }
    
    @IoFilterTransition(on = MESSAGE_RECEIVED, in = WAIT_USER, next = WAIT_PASSWORD)
    public void user(AuthenticationContext context, NextFilter nextFilter, IoSession session, UserCommand cmd) {
        context.user = cmd.getUsername();
        session.write("+ Give me your password (hint: try your username backwards)");
    }
    
    @IoFilterTransition(on = MESSAGE_RECEIVED, in = WAIT_PASSWORD, next = DONE)
    public void password(AuthenticationContext context, NextFilter nextFilter, IoSession session, PasswordCommand cmd) {
        context.password = cmd.getPassword();
        if (context.password.equals(reverse(context.user))) {
            session.write("+ Authentication succeeded! Your tape deck has been unlocked.");
        } else {
            context.tries++;
            if (context.tries < 3) {
                session.write("- Authentication failed! Please try again.");
                StateControl.breakAndGotoNext(WAIT_USER);
            } else {
                session.write("- Authentication failed too many times! Bye bye.").addListener(IoFutureListener.CLOSE);
                StateControl.breakAndGotoNext(FAILED);
            }
        }
    }
    
    
    @IoFilterTransition(on = MESSAGE_RECEIVED, in = ROOT)
    public void quit(TapeDeckContext context, IoSession session, QuitCommand cmd) {
        session.write("+ Bye! Please come back!").addListener(IoFutureListener.CLOSE);
    }
    
    private String reverse(String s) {
        char[] expectedPassword = new char[s.length()];
        for (int i = 0; i < expectedPassword.length; i++) {
            expectedPassword[i] = s.charAt(s.length() - i - 1);
        }
        return new String(expectedPassword);
    }
    
    @IoFilterTransition(on = MESSAGE_RECEIVED, in = WAIT_USER, weight = 10)
    public void errorWaitingForUser(IoSession session, Command cmd) {
        if (cmd instanceof QuitCommand) {
            StateControl.breakAndContinue();
        }
        session.write("Unexpected command '" + cmd.getName() + "'. Expected 'user <username>'.");
    }
    
    @IoFilterTransition(on = MESSAGE_RECEIVED, in = WAIT_PASSWORD, weight = 10)
    public void errorWaitingForPassword(IoSession session, Command cmd) {
        if (cmd instanceof QuitCommand) {
            StateControl.breakAndContinue();
        }
        session.write("Unexpected command '" + cmd.getName() + "'. Expected 'password <password>'.");
    }
    
    @IoFilterTransition(on = EXCEPTION_CAUGHT, in = ROOT)
    public void commandSyntaxError(IoSession session, CommandSyntaxException e) {
        session.write("- " + e.getMessage());
    }
    
    @IoFilterTransition(on = EXCEPTION_CAUGHT, in = ROOT, weight = 10)
    public void exceptionCaught(IoSession session, Exception e) {
        e.printStackTrace();
        session.close(true);
    }
    
//    
//    @IoFilterTransition(on = SESSION_CREATED, in = ROOT)
//    public void sessionCreated(NextFilter nextFilter, IoSession session) {
//        nextFilter.sessionCreated(session);
//    }
//    @IoFilterTransition(on = SESSION_OPENED, in = ROOT)
//    public void sessionOpened(NextFilter nextFilter, IoSession session) {
//        nextFilter.sessionOpened(session);
//    }
    
    @IoFilterTransition(on = SESSION_CLOSED, in = DONE)
    public void sessionClosed(NextFilter nextFilter, IoSession session) {
        nextFilter.sessionClosed(session);
    }
    @IoFilterTransition(on = EXCEPTION_CAUGHT, in = DONE)
    public void exceptionCaught(NextFilter nextFilter, IoSession session, Throwable cause) {
        nextFilter.exceptionCaught(session, cause);
    }
    @IoFilterTransition(on = MESSAGE_RECEIVED, in = DONE)
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) {
        nextFilter.messageReceived(session, message);
    }
    @IoFilterTransition(on = MESSAGE_SENT, in = DONE)
    public void messageSent(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) {
        nextFilter.messageSent(session, writeRequest);
    }
    
    @IoFilterTransition(on = CLOSE, in = ROOT)
    public void filterClose(NextFilter nextFilter, IoSession session) {
        nextFilter.filterClose(session);
    }
    @IoFilterTransition(on = WRITE, in = ROOT)
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) {
        nextFilter.filterWrite(session, writeRequest);
    }
}
