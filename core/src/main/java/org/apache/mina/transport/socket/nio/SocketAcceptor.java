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
package org.apache.mina.transport.socket.nio;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.support.DelegatedIoAcceptor;
import org.apache.mina.transport.socket.nio.support.SocketAcceptorDelegate;

/**
 * {@link IoAcceptor} for socket transport (TCP/IP).
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SocketAcceptor extends DelegatedIoAcceptor
{
    /**
     * Creates a new instance.
     */
    public SocketAcceptor()
    {
        init( new SocketAcceptorDelegate( this ) );
    }

    public int getReceiveBufferSize()
    {
        return ( ( SocketAcceptorDelegate ) delegate ).getReceiveBufferSize();
    }

    /**
     * @param receiveBufferSize <tt>-1</tt> to use the default value.
     */
    public void setReceiveBufferSize( int receiveBufferSize )
    {
        ( ( SocketAcceptorDelegate ) delegate ).setReceiveBufferSize( receiveBufferSize );
    }

    public boolean isReuseAddress()
    {
        return ( ( SocketAcceptorDelegate ) delegate ).isReuseAddress();
    }

    public void setReuseAddress( boolean reuseAddress )
    {
        ( ( SocketAcceptorDelegate ) delegate ).setReuseAddress( reuseAddress );
    }

    public int getBacklog()
    {
        return ( ( SocketAcceptorDelegate ) delegate ).getBacklog();
    }

    public void setBacklog( int backlog )
    {
        ( ( SocketAcceptorDelegate ) delegate ).setBacklog( backlog );
    }
}
