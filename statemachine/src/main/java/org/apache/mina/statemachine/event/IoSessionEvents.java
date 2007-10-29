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

import org.apache.mina.common.IoHandler;
import org.apache.mina.statemachine.annotation.Handler;

/**
 * Defines all possible MINA {@link IoHandler} events for use in {@link Handler}
 * annotations.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public final class IoSessionEvents {
    public static final String SESSION_CREATED = "sessionCreated";

    public static final String SESSION_OPENED = "sessionOpened";

    public static final String SESSION_CLOSED = "sessionClosed";

    public static final String SESSION_IDLE = "sessionIdle";

    public static final String MESSAGE_RECEIVED = "messageReceived";

    public static final String MESSAGE_SENT = "messageSent";

    public static final String EXCEPTION_CAUGHT = "exceptionCaught";

    private IoSessionEvents() {
    }
}
