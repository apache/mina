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

import org.apache.mina.common.Session;

/**
 * A {@link Session} which represents high-level protocol connection between two
 * endpoints regardless of underlying transport types.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 * 
 * @see Session
 */
public interface ProtocolSession extends Session
{
    /**
     * Returns the {@link ProtocolHandler} which handles this session.
     */
    ProtocolHandler getHandler();

    /**
     * Returns the filter chain that only affects this session.
     */
    ProtocolFilterChain getFilterChain();


    /**
     * Returns the {@link ProtocolEncoder} for this session.
     */
    ProtocolEncoder getEncoder();

    /**
     * Returns the {@link ProtocolDecoder} for this session.
     */
    ProtocolDecoder getDecoder();

    /**
     * Writes the specified <code>message</code> to remote peer.  This operation
     * is asynchronous; {@link ProtocolHandler#messageSent(ProtocolSession, Object)}
     * will be invoked when the message is actually sent to remote peer.
     */
    void write( Object message );
}