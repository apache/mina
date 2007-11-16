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
package org.apache.mina.common;

import java.util.EventListener;

/**
 * Something interested in being notified when the result
 * of an {@link IoFuture} becomes available.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoServiceListener extends EventListener {
    /**
     * Invoked when a new service is activated by an {@link IoService}.
     *
     * @param service the {@link IoService}
     */
    void serviceActivated(IoService service);
    
    /**
     * Invoked when a service is idle.
     */
    void serviceIdle(IoService service, IdleStatus idleStatus);

    /**
     * Invoked when a service is deactivated by an {@link IoService}.
     *
     * @param service the {@link IoService}
     */
    void serviceDeactivated(IoService service);

    /**
     * Invoked when a new session is created by an {@link IoService}.
     *
     * @param session the new session
     */
    void sessionCreated(IoSession session);

    /**
     * Invoked when a session is being destroyed by an {@link IoService}.
     *
     * @param session the session to be destroyed
     */
    void sessionDestroyed(IoSession session);
}
