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
package org.apache.mina.proxy.event;

/**
 * IoSessionEventType.java - Enumerates session event types.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public enum IoSessionEventType {
    CREATED(1), OPENED(2), IDLE(3), CLOSED(4);

    /**
     * The event type id.
     */
    private final int id;
    
    private IoSessionEventType(int id) {
        this.id = id;
    }
    
    /**
     * Returns the event id.
     * 
     * @return the event id
     */
    public int getId() {
        return id;
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    public String toString() {
        switch (this) {
        case CREATED:
            return "- CREATED event -";
        case OPENED:
            return "- OPENED event -";
        case IDLE:
            return "- IDLE event -";
        case CLOSED:
            return "- CLOSED event -";
        default:
            return "- Event Id="+id+" -";
        }
    }
}