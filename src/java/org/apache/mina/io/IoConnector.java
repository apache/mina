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
import java.net.SocketAddress;

/**
 * Connects to endpoint, communicates with the server, and fires events to
 * {@link IoHandler}s.
 * <p>
 * Please refer to
 * <a href="../../../../../xref-examples/org/apache/mina/examples/netcat/Main.html">NetCat</a>
 * example. 
 * <p>
 * You should connect to the desired socket address to start communication,
 * and then events for incoming connections will be sent to the specified
 * default {@link IoHandler}.
 * <p>
 * Threads connect to endpoint start automatically when
 * {@link #connect(SocketAddress, IoHandler)} is invoked, and stop when all
 * connection attempts are finished.
 * <p>
 * {@link IoHandlerFilter}s can be added and removed at any time to filter
 * events just like Servlet filters and they are effective immediately.
 * <p>
 * You can monitor any uncaught exceptions by setting {@link ExceptionMonitor}
 * by calling {@link #setExceptionMonitor(ExceptionMonitor)}.  The default
 * monitor is {@link DefaultExceptionMonitor}.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public interface IoConnector
{
    /**
     * Connects to the specified <code>address</code>.  If communication starts
     * successfully, events are fired to the specified
     * <code>handler</code>.  This method blocks.
     * 
     * @throws IOException if failed to connect
     */
    IoSession connect( SocketAddress address, IoHandler handler )
            throws IOException;

    /**
     * Connects to the specified <code>address</code>.  If communication starts
     * successfully, events are fired to the specified
     * <code>handler</code>.  This method blocks.
     * 
     * @param localAddress the local address the channel is bound to
     * @throws IOException if failed to connect
     */
    IoSession connect( SocketAddress address, SocketAddress localAddress,
                       IoHandler handler ) throws IOException;

    /**
     * Connects to the specified <code>address</code> with timeout.  If
     * communication starts successfully, events are fired to the specified
     * <code>handler</code>.  This method blocks.
     * 
     * @throws IOException if failed to connect
     */
    IoSession connect( SocketAddress address, int timeout, IoHandler handler )
            throws IOException;
    
    /**
     * Connects to the specified <code>address</code> with timeout.  If
     * communication starts successfully, events are fired to the specified
     * <code>handler</code>.  This method blocks.
     * 
     * @param localAddress the local address the channel is bound to
     * @throws IOException if failed to connect
     */
    IoSession connect( SocketAddress address, SocketAddress localAddress,
                       int timeout, IoHandler handler ) throws IOException;

    IoHandlerFilterChain getFilterChain();

    /**
     * Returns the current exception monitor.
     */
    ExceptionMonitor getExceptionMonitor();

    /**
     * Sets the uncaught exception monitor.  If <code>null</code> is specified,
     * a new instance of {@link DefaultExceptionMonitor} will be set.
     */
    void setExceptionMonitor( ExceptionMonitor monitor );
}