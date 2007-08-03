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

import java.io.IOException;

/**
 * Handles all I/O events fired by MINA.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * 
 * @see IoHandlerAdapter
 */
public interface IoHandler {
    /**
     * Invoked from an I/O processor thread when a new connection has been created.
     * Because this method is supposed to be called from the same thread that
     * handles I/O of multiple sessions, please implement this method to perform
     * tasks that consumes minimal amount of time such as socket parameter
     * and user-defined session attribute initialization.
     */
    void sessionCreated(IoSession session) throws Exception;

    /**
     * Invoked when a connection has been opened.  This method is invoked after
     * {@link #sessionCreated(IoSession)}.  The biggest difference from
     * {@link #sessionCreated(IoSession)} is that it's invoked from other thread
     * than an I/O processor thread once thread modesl is configured properly.
     */
    void sessionOpened(IoSession session) throws Exception;

    /**
     * Invoked when a connection is closed.
     */
    void sessionClosed(IoSession session) throws Exception;

    /**
     * Invoked with the related {@link IdleStatus} when a connection becomes idle.
     * This method is not invoked if the transport type is UDP; it's a known bug,
     * and will be fixed in 2.0.
     */
    void sessionIdle(IoSession session, IdleStatus status) throws Exception;

    /**
     * Invoked when any exception is thrown by user {@link IoHandler}
     * implementation or by MINA.  If <code>cause</code> is instanceof
     * {@link IOException}, MINA will close the connection automatically.
     */
    void exceptionCaught(IoSession session, Throwable cause) throws Exception;

    /**
     * Invoked when a message is received.
     */
    void messageReceived(IoSession session, Object message) throws Exception;

    /**
     * Invoked when a message written by {@link IoSession#write(Object)} is
     * sent out.
     */
    void messageSent(IoSession session, Object message) throws Exception;
}