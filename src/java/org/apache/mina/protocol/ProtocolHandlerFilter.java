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

import org.apache.mina.common.IdleStatus;

/**
 * A filter which intercepts {@link ProtocolHandler} events like Servlet
 * filters.  Filters can be used for these purposes:
 * <ul>
 *   <li>Event logging,</li>
 *   <li>Performance measurement,</li>
 *   <li>Authorization,</li>
 *   <li>Overload control,</li>
 *   <li>Message transformation (e.g. encryption and decryption, ...),</li>
 *   <li>and many more.</li>
 * </ul>
 * <p>
 * <strong>Please NEVER implement your filters to wrap
 * {@link ProtocolSession}s.</strong> Users can cache the reference to the
 * session, which might malfunction if any filters are added or removed later.
 * Please implement {@link #filterWrite(ProtocolSession,Object)} method to
 * override {@link ProtocolSession#write(Object)} method.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 * 
 * @see ProtocolHandlerFilterAdapter
 */
public interface ProtocolHandlerFilter
{
    /**
     * Filters {@link ProtocolHandler#sessionOpened(ProtocolSession)} event.
     */
    void sessionOpened( NextFilter nextFilter, ProtocolSession session );

    /**
     * Filters {@link ProtocolHandler#sessionClosed(ProtocolSession)} event.
     */
    void sessionClosed( NextFilter nextFilter, ProtocolSession session );

    /**
     * Filters {@link ProtocolHandler#sessionIdle(ProtocolSession,IdleStatus)}
     * event.
     */
    void sessionIdle( NextFilter nextFilter, ProtocolSession session,
                     IdleStatus status );

    /**
     * Filters {@link ProtocolHandler#exceptionCaught(ProtocolSession,Throwable)}
     * event.
     */
    void exceptionCaught( NextFilter nextFilter,
                         ProtocolSession session, Throwable cause );

    /**
     * Filters {@link ProtocolHandler#messageReceived(ProtocolSession,Object)}
     * event.
     */
    void messageReceived( NextFilter nextFilter,
                         ProtocolSession session, Object message );

    /**
     * Filters {@link ProtocolHandler#messageSent(ProtocolSession,Object)}
     * event.
     */
    void messageSent( NextFilter nextFilter, ProtocolSession session,
                     Object message );

    /**
     * Filters {@link ProtocolSession#write(Object)} method invocation.
     */
    void filterWrite( NextFilter nextFilter, ProtocolSession session, Object message );
    
    public interface NextFilter extends ProtocolHandler
    {
        void filterWrite( ProtocolSession session, Object message );
    }
}
