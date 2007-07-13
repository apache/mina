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
 * Handles all protocol events fired by MINA.
 * There are 6 event handler methods, and they are all invoked by MINA
 * automatically.
 * <p>
 * Please refer to
 * <a href="../../../../../xref-examples/org/apache/mina/examples/reverser/ReverseIoHandler.html"><code>ReverseIoHandler</code></a>
 * example.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 * 
 * @see IoHandlerAdapter
 */
public interface IoHandler {
    /**
     * Invoked when the session is created.  Initialize default socket
     * parameters and user-defined attributes here.
     */
    void sessionCreated(IoSession session) throws Exception;

    /**
     * Invoked when the connection is opened.  This method is not invoked if the
     * transport type is UDP.
     */
    void sessionOpened(IoSession session) throws Exception;

    /**
     * Invoked when the connection is closed.  This method is not invoked if the
     * transport type is UDP.
     */
    void sessionClosed(IoSession session) throws Exception;

    /**
     * Invoked when the connection is idle.  Refer to {@link IdleStatus}.  This
     * method is not invoked if the transport type is UDP.
     */
    void sessionIdle(IoSession session, IdleStatus status) throws Exception;

    /**
     * Invoked when any exception is thrown by user {@link IoHandler}
     * implementation or by MINA.  If <code>cause</code> is instanceof
     * {@link IOException}, MINA will close the connection automatically.
     */
    void exceptionCaught(IoSession session, Throwable cause) throws Exception;

    /**
     * Invoked when protocol message is received.  Implement your protocol flow
     * here.
     */
    void messageReceived(IoSession session, Object message) throws Exception;

    /**
     * Invoked when protocol message that user requested by
     * {@link IoSession#write(Object)} is sent out actually.
     */
    void messageSent(IoSession session, Object message) throws Exception;
}