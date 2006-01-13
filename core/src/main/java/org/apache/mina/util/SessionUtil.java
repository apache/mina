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
package org.apache.mina.util;

import java.net.SocketException;

import org.apache.mina.common.IoSession;
import org.apache.mina.transport.socket.nio.DatagramSession;
import org.apache.mina.transport.socket.nio.SocketSession;

/**
 * Exception utility.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SessionUtil
{
    public static void initialize( IoSession session ) throws SocketException
    {
        if( session instanceof SocketSession )
        {
            SocketSession ss = ( SocketSession ) session;
            ss.setReuseAddress( true );
            ss.setKeepAlive( true );
        }
        else if( session instanceof DatagramSession )
        {
            DatagramSession ds = ( DatagramSession ) session;
            ds.setReuseAddress( true );
        }
    }

    private SessionUtil()
    {
    }
}
