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

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;

/**
 * An {@link IoHandlerFilterChain} that forwards all events
 * except <tt>filterWrite</tt> to the {@link IoSessionFilterChain}
 * of the recipient session.
 * <p>
 * This filter chain is used by implementations of {@link IoSessionManager}s.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public abstract class IoSessionManagerFilterChain extends AbstractIoHandlerFilterChain {

    private final IoSessionManager manager;

    protected IoSessionManagerFilterChain( IoSessionManager manager )
    {
        this.manager = manager;
    }
    
    public IoSessionManager getManager()
    {
        return manager;
    }
    
    protected IoHandlerFilter createTailFilter()
    {
        return new IoHandlerFilter()
        {
            public void sessionOpened( NextFilter nextFilter, IoSession session )
            {
                ( ( IoSessionFilterChain ) session.getFilterChain() ).sessionOpened( session );
            }

            public void sessionClosed( NextFilter nextFilter, IoSession session )
            {
                ( ( IoSessionFilterChain ) session.getFilterChain() ).sessionClosed( session );
            }

            public void sessionIdle( NextFilter nextFilter, IoSession session,
                                    IdleStatus status )
            {
                ( ( IoSessionFilterChain ) session.getFilterChain() ).sessionIdle( session, status );
            }

            public void exceptionCaught( NextFilter nextFilter,
                                        IoSession session, Throwable cause )
            {
                ( ( IoSessionFilterChain ) session.getFilterChain() ).exceptionCaught( session, cause );
            }

            public void dataRead( NextFilter nextFilter, IoSession session,
                                 ByteBuffer buf )
            {
                ( ( IoSessionFilterChain ) session.getFilterChain() ).dataRead( session, buf );
            }

            public void dataWritten( NextFilter nextFilter, IoSession session,
                                    Object marker )
            {
                ( ( IoSessionFilterChain ) session.getFilterChain() ).dataWritten( session, marker );
            }

            public void filterWrite( NextFilter nextFilter,
                                     IoSession session, ByteBuffer buf, Object marker )
            {
                nextFilter.filterWrite( session, buf, marker );
            }
        };
    }
}
