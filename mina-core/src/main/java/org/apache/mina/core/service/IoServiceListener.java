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
package org.apache.mina.core.service;

import java.util.EventListener;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

/**
 * Listens to events related to an {@link IoService}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoServiceListener extends EventListener {
    /**
     * Invoked when a new service is activated by an {@link IoService}.
     *
     * @param service the {@link IoService}
     * @throws Exception if an error occurred while the service is being activated
     */
    void serviceActivated(IoService service) throws Exception;

    /**
     * Invoked when a service is idle.
     * 
     * @param service the {@link IoService}
     * @param idleStatus The idle status
     * @throws Exception if an error occurred while the service is being idled
     */
    void serviceIdle(IoService service, IdleStatus idleStatus) throws Exception;

    /**
     * Invoked when a service is deactivated by an {@link IoService}.
     *
     * @param service the {@link IoService}
     * @throws Exception if an error occurred while the service is being deactivated
     */
    void serviceDeactivated(IoService service) throws Exception;

    /**
     * Invoked when a new session is created by an {@link IoService}.
     *
     * @param session the new session
     * @throws Exception if an error occurred while the session is being created
     */
    void sessionCreated(IoSession session) throws Exception;

    /**
     * Invoked when a new session is closed by an {@link IoService}.
     * 
     * @param session the new session
     * @throws Exception if an error occurred while the session is being closed
     */
    void sessionClosed(IoSession session) throws Exception;

    /**
     * Invoked when a session is being destroyed by an {@link IoService}.
     * 
     * @param session the session to be destroyed
     * @throws Exception if an error occurred while the session is being destroyed
     */
    void sessionDestroyed(IoSession session) throws Exception;
}
