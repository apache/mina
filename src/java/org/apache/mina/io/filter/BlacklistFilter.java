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
package org.apache.mina.io.filter;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.io.IoFilter;
import org.apache.mina.io.IoFilterAdapter;
import org.apache.mina.io.IoSession;

/**
 * A {@link IoFilter} which blocks connections from blacklisted remote
 * address.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class BlacklistFilter extends IoFilterAdapter
{
    private final Set blacklist = new HashSet();

    /**
     * Blocks the specified endpoint.
     */
    public synchronized void block( InetAddress address )
    {
        blacklist.add( address );
    }

    /**
     * Unblocks the specified endpoint.
     */
    public synchronized void unblock( InetAddress address )
    {
        blacklist.remove( address );
    }

    /**
     * Forwards event if and if only the remote address of session is not
     * blacklisted.
     */
    public void dataRead( NextFilter nextFilter, IoSession session,
                         ByteBuffer buf )
    {
        if( !isBlocked( session ) )
        {
            // forward if not blocked
            super.dataRead( nextFilter, session, buf );
        }
    }

    /**
     * Close connection immediately if the remote address of session is
     * blacklisted.
     */
    public void sessionOpened( NextFilter nextFilter, IoSession session )
    {
        if( isBlocked( session ) )
        {
            // Close immediately if blocked
            session.close();
        }
        else
        {
            super.sessionOpened( nextFilter, session );
        }
    }

    private boolean isBlocked( IoSession session )
    {
        SocketAddress remoteAddress = session.getRemoteAddress();
        if( remoteAddress instanceof InetSocketAddress )
        {
            if( blacklist.contains( ( ( InetSocketAddress ) remoteAddress )
                    .getAddress() ) )
            {
                return true;
            }
        }

        return false;
    }
}