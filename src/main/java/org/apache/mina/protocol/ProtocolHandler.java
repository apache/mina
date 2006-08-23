/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.mina.protocol;

import java.io.IOException;

import org.apache.mina.common.IdleStatus;

/**
 * Handles all protocol events fired by MINA.
 * There are 6 event handler methods, and they are all invoked by MINA
 * automatically.
 * <p>
 * Please refer to
 * <a href="../../../../../xref-examples/org/apache/mina/examples/reverser/ReverseProtocolHandler.html"><code>ReverseProtocolHandler</code></a>
 * example.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 * 
 * @see ProtocolHandlerAdapter
 */
public interface ProtocolHandler
{
    /**
     * Invoked when the session is created.  Initialize default socket
     * parameters and user-defined attributes here.
     */
    void sessionCreated( ProtocolSession session ) throws Exception;
    
    /**
     * Invoked when the connection is opened.  This method is not invoked if the
     * transport type is UDP.
     */
    void sessionOpened( ProtocolSession session ) throws Exception;

    /**
     * Invoked when the connection is closed.  This method is not invoked if the
     * transport type is UDP.
     */
    void sessionClosed( ProtocolSession session ) throws Exception;

    /**
     * Invoked when the connection is idle.  Refer to {@link IdleStatus}.  This
     * method is not invoked if the transport type is UDP.
     */
    void sessionIdle( ProtocolSession session, IdleStatus status ) throws Exception;

    /**
     * Invoked when any exception is thrown by user {@link ProtocolHandler}
     * implementation or by MINA.  If <code>cause</code> is instanceof
     * {@link IOException}, MINA will close the connection automatically.
     */
    void exceptionCaught( ProtocolSession session, Throwable cause ) throws Exception;

    /**
     * Invoked when protocol message is received.  Implement your protocol flow
     * here.
     */
    void messageReceived( ProtocolSession session, Object message ) throws Exception;

    /**
     * Invoked when protocol message that user requested by
     * {@link ProtocolSession#write(Object)} is sent out actually.
     */
    void messageSent( ProtocolSession session, Object message ) throws Exception;
}