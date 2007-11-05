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

import org.apache.mina.common.IoFilter;
import org.apache.mina.statemachine.annotation.IoFilterTransition;

/**
 * Defines all possible MINA {@link IoFilter} events for use in {@link IoFilterTransition}
 * annotations.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public enum IoFilterEvents {
    ANY(Event.WILDCARD_EVENT_ID),
    SESSION_CREATED("sessionCreated"),
    SESSION_OPENED("sessionOpened"),
    SESSION_CLOSED("sessionClosed"),
    SESSION_IDLE("sessionIdle"),
    MESSAGE_RECEIVED("messageReceived"),
    MESSAGE_SENT("messageSent"),
    FILTER_CLOSE("filterClose"),
    FILTER_WRITE("filterWrite"),
    FILTER_SET_TRAFFIC_MASK("filterSetTrafficMask"),
    EXCEPTION_CAUGHT("exceptionCaught");

    private final String value;
    
    private IoFilterEvents(String value) {
        this.value = value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
