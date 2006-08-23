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
import java.net.SocketAddress;

/**
 * Accepts incoming connection, communicates with clients, and fires events to
 * {@link ProtocolHandler}s.
 * <p>
 * Please refer to
 * <a href="../../../../../xref-examples/org/apache/mina/examples/reverser/Main.html">Reverser</a>
 * example. 
 * <p>
 * You should bind to the desired socket address to accept incoming
 * connections, and then events for incoming connections will be sent to
 * the specified default {@link ProtocolHandler}.
 * <p>
 * Threads accept incoming connections start automatically when
 * {@link #bind(SocketAddress, ProtocolProvider)} is invoked, and stop when all
 * addresses are unbound.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface ProtocolAcceptor extends ProtocolSessionManager
{
    /**
     * Binds to the specified <code>address</code> and handles incoming
     * connections with the specified <code>protocolProvider</code>.
     * 
     * @throws IOException if failed to bind
     */
    void bind( SocketAddress address, ProtocolProvider protocolProvider )
            throws IOException;

    /**
     * Unbinds from the specified <code>address</code>.
     */
    void unbind( SocketAddress address );
    
    /**
     * (Optional) Returns a {@link ProtocolSession} that is bound to the specified
     * <tt>localAddress</tt> and <tt>remoteAddress</tt> which reuses
     * the <tt>localAddress</tt> that is already bound by {@link ProtocolAcceptor}
     * via {@link #bind(SocketAddress, ProtocolProvider)}.
     * <p>
     * This operation is optional.  Please throw {@link UnsupportedOperationException}
     * if the transport type doesn't support this operation.  This operation is
     * usually implemented for connectionless transport types.
     * 
     * @throws UnsupportedOperationException if this operation is not supported
     * @throws IllegalArgumentException if the specified <tt>localAddress</tt> is
     *                                  not bound yet. (see {@link #bind(SocketAddress, ProtocolProvider)})
     */
    ProtocolSession newSession( SocketAddress remoteAddress, SocketAddress localAddress );
}