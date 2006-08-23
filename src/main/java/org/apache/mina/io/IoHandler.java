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
package org.apache.mina.io;

import java.io.IOException;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;

/**
 * Handles all I/O events fired by {@link IoAcceptor} and {@link IoConnector}.
 * There are 6 event handler methods, and they are all invoked by MINA
 * automatically.  Most users of MINA I/O package will be OK with this single
 * interface to implement their protocols.
 * <p>
 * Please refer to
 * <a href="../../../../../xref-examples/org/apache/mina/examples/echoserver/EchoProtocolHandler.html"><code>EchoProtocolHandler</code></a>
 * example. 
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 * 
 * @see IoHandlerAdapter
 */
public interface IoHandler
{
    /**
     * Invoked when the session is created.  Initialize default socket
     * parameters and user-defined attributes here.
     */
    void sessionCreated( IoSession session ) throws Exception;
    
    /**
     * Invoked when the connection is opened.  This method is not invoked if the
     * transport type is UDP.
     */
    void sessionOpened( IoSession session ) throws Exception;

    /**
     * Invoked when the connection is closed.  This method is not invoked if the
     * transport type is UDP.
     */
    void sessionClosed( IoSession session ) throws Exception;

    /**
     * Invoked when the connection is idle.  Refer to {@link IdleStatus}.  This
     * method is not invoked if the transport type is UDP.
     */
    void sessionIdle( IoSession session, IdleStatus status ) throws Exception;

    /**
     * Invoked when any exception is thrown by user {@link IoHandler}
     * implementation or by MINA.  If <code>cause</code> is instanceof
     * {@link IOException}, MINA will close the connection automatically.
     */
    void exceptionCaught( IoSession session, Throwable cause ) throws Exception;

    /**
     * Invoked when data is read from the connection.  You can access
     * <code>buf</code> to get read data.  <code>buf</code> returns to
     * the internal buffer pool of MINA after this method is invoked, so
     * please don't try to reuse it.
     */
    void dataRead( IoSession session, ByteBuffer buf ) throws Exception;

    /**
     * Invoked when MINA wrote {@link IoSession#write(ByteBuffer, Object)}
     * request successfully.  <code>marker</code> is what you specified at the
     * point of invocation of {@link IoSession#write(ByteBuffer, Object)}.
     */
    void dataWritten( IoSession session, Object marker ) throws Exception;
}