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
 * Thrown when an {@link Event} passed to a state machine couldn't be handled.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class UnhandledEventException extends RuntimeException {
    private static final long serialVersionUID = -717373229954175430L;
    
    private final Event event;

    public UnhandledEventException(Event event) {
        super("Unhandled event: " + event);
        this.event = event;
    }

    /**
     * Returns the {@link Event} which couldn't be handled.
     * 
     * @return the {@link Event}.
     */
    public Event getEvent() {
        return event;
    }
}
