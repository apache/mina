/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mina.service.idlechecker;

import org.apache.mina.session.AbstractIoSession;

/**
 * Utility for checking detecting idle sessions.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IdleChecker {

    /**
     * Inform the IdleCheker a session have a write event
     * 
     * @param session the session with the write event
     * @param time the data in ms (unix time) of the event
     */
    void sessionWritten(AbstractIoSession session, long time);

    /**
     * Inform the IdleCheker a session have a read event
     * 
     * @param session the session with the read event
     * @param time the data in ms (unix time) of the event
     */
    void sessionRead(AbstractIoSession session, long time);

    /**
     * Find idle session, to be called for each select() call.
     * 
     * @param time current unix time in ms
     * @return the number of idle event detected
     */
    int processIdleSession(long time);

    /**
     * Start the idle checker inner threads
     */
    void start();

    /**
     * Stop the idle checker.
     */
    void destroy();
}
