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
 * An {@link ProtocolHandlerFilterChain} that forwards all events
 * except <tt>filterWrite</tt> to the {@link ProtocolSessionFilterChain}
 * of the recipient session.
 * <p>
 * This filter chain is used by implementations of {@link ProtocolSessionManager}s.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public abstract class ProtocolSessionManagerFilterChain extends AbstractProtocolHandlerFilterChain {

    private final ProtocolSessionManager manager;

    protected ProtocolSessionManagerFilterChain( ProtocolSessionManager manager )
    {
        this.manager = manager;
    }
    
    public ProtocolSessionManager getManager()
    {
        return manager;
    }
    
    protected ProtocolHandlerFilter createTailFilter()
    {
        return new ProtocolHandlerFilter()
        {
            public void sessionOpened( NextFilter nextFilter, ProtocolSession session )
            {
                ( ( ProtocolSessionFilterChain ) session.getFilterChain() ).sessionOpened( session );
            }

            public void sessionClosed( NextFilter nextFilter, ProtocolSession session )
            {
                ( ( ProtocolSessionFilterChain ) session.getFilterChain() ).sessionClosed( session );
            }

            public void sessionIdle( NextFilter nextFilter, ProtocolSession session,
                                    IdleStatus status )
            {
                ( ( ProtocolSessionFilterChain ) session.getFilterChain() ).sessionIdle( session, status );
            }

            public void exceptionCaught( NextFilter nextFilter,
                                        ProtocolSession session, Throwable cause )
            {
                ( ( ProtocolSessionFilterChain ) session.getFilterChain() ).exceptionCaught( session, cause );
            }

            public void messageReceived( NextFilter nextFilter, ProtocolSession session,
                                         Object message )
            {
                ( ( ProtocolSessionFilterChain ) session.getFilterChain() ).messageReceived( session, message );
            }

            public void messageSent( NextFilter nextFilter, ProtocolSession session,
                                     Object message )
            {
                ( ( ProtocolSessionFilterChain ) session.getFilterChain() ).messageSent( session, message );
            }

            public void filterWrite( NextFilter nextFilter,
                                     ProtocolSession session, Object message )
            {
                nextFilter.filterWrite( session, message );
            }
        };
    }
}
