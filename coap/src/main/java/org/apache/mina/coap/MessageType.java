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
package org.apache.mina.coap;

/**
 * Type of CoAP message :
 * <ul>
 * <li>CONFIMABLE : need to be confirmed by a ACK
 * <li>NON_CONFIRMABLE : fire and forget message
 * <li>ACK : confirmation of a CONFIRMABLE message
 * <li>RESET : to reset the session (see the RFC for details)
 * </ul>
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public enum MessageType {
    CONFIRMABLE(0), NON_CONFIRMABLE(1), ACK(2), RESET(3);

    private final int code;

    private MessageType(int code) {
        this.code = code;
    }

    /**
     * @return the numeric code for this message type
     */
    public int getCode() {
        return code;
    }

    /**
     * Find the {@link MessageType} for the given value code (<code>null</code> if not found)
     */
    public static MessageType fromCode(int code) {
        for (MessageType t : MessageType.values()) {
            if (t.getCode() == code) {
                return t;
            }
        }
        return null;
    }
}
