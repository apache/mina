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
package org.apache.mina.statemachine.event;

/**
 * Defines all possible MINA {@link IoFilter} events for use in {@link IoFilterTransition}
 * annotations.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public enum IoFilterEvents {
    /** The wildcard event */
    ANY(Event.WILDCARD_EVENT_ID),
    
    /** The Session Created event */ 
    SESSION_CREATED("sessionCreated"), 
    
    /** The Session Opened event */ 
    SESSION_OPENED("sessionOpened"), 
    
    /** The Session Closed event */ 
    SESSION_CLOSED("sessionClosed"), 
    
    /** The Session Idle event */ 
    SESSION_IDLE("sessionIdle"), 
    
    /** The Message Received event */ 
    MESSAGE_RECEIVED("messageReceived"), 
    
    /** The Message Sent event */ 
    MESSAGE_SENT("messageSent"), 
    
    /** The Exception Caught event */ 
    EXCEPTION_CAUGHT("exceptionCaught"), 
    
    /** The Close event */ 
    CLOSE("filterClose"), 
    
    /** The Write event */ 
    WRITE("filterWrite"), 
    
    /** The InputClosed event */
    INPUT_CLOSED("inputClosed"),
    
    /** The Set Traffic Mask event */ 
    SET_TRAFFIC_MASK("filterSetTrafficMask");

    private final String value;

    private IoFilterEvents(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
