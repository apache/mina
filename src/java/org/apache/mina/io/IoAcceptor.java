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

import org.apache.mina.common.FilterChainType;

/**
 * Accepts incoming connection, communicates with clients, and fires events to
 * {@link IoHandler}s.
 * <p>
 * Please refer to
 * <a href="../../../../../xref-examples/org/apache/mina/examples/echoserver/Main.html">EchoServer</a>
 * example. 
 * <p>
 * You should bind to the desired socket address to accept incoming
 * connections, and then events for incoming connections will be sent to
 * the specified default {@link IoHandler}.
 * <p>
 * Threads accept incoming connections start automatically when
 * {@link #bind(SocketAddress, IoHandler)} is invoked, and stop when all
 * addresses are unbound.
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
public interface IoAcceptor
{
    /**
     * Binds to the specified <code>address</code> and handles incoming
     * connections with the specified <code>handler</code>.
     * 
     * @throws IOException if failed to bind
     */
    void bind( SocketAddress address, IoHandler handler ) throws IOException;

    /**
     * Unbinds from the specified <code>address</code>.
     */
    void unbind( SocketAddress address );

    IoHandlerFilterChain newFilterChain( FilterChainType type );
    
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