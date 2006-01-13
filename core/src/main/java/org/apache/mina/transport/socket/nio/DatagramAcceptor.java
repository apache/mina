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
import org.apache.mina.transport.socket.nio.support.DatagramAcceptorDelegate;

/**
 * {@link IoAcceptor} for datagram transport (UDP/IP).
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class DatagramAcceptor extends DelegatedIoAcceptor implements DatagramSessionManager
{
    /**
     * Creates a new instance.
     */
    public DatagramAcceptor()
    {
        init( new DatagramAcceptorDelegate( this ) );
    }

    public int getReceiveBufferSize()
    {
        return ( ( DatagramAcceptorDelegate ) delegate ).getReceiveBufferSize();
    }

    public void setReceiveBufferSize( int receiveBufferSize )
    {
        ( ( DatagramAcceptorDelegate ) delegate ).setReceiveBufferSize( receiveBufferSize );
    }

    public boolean getBroadcast()
    {
        return ( ( DatagramAcceptorDelegate ) delegate ).getBroadcast();
    }

    public void setBroadcast( boolean broadcast )
    {
        ( ( DatagramAcceptorDelegate ) delegate ).setBroadcast( broadcast );
    }

    public int getSendBufferSize()
    {
        return ( ( DatagramAcceptorDelegate ) delegate ).getSendBufferSize();
    }

    public void setSendBufferSize( int sendBufferSize )
    {
        ( ( DatagramAcceptorDelegate ) delegate ).setSendBufferSize( sendBufferSize );
    }

    public int getTrafficClass()
    {
        return ( ( DatagramAcceptorDelegate ) delegate ).getTrafficClass();
    }

    public void setTrafficClass( int trafficClass )
    {
        ( ( DatagramAcceptorDelegate ) delegate ).setTrafficClass( trafficClass );
    }

    public boolean getReuseAddress()
    {
        return ( ( DatagramAcceptorDelegate ) delegate ).getReuseAddress();
    }

    public void setReuseAddress( boolean reuseAddress )
    {
        ( ( DatagramAcceptorDelegate ) delegate ).setReuseAddress( reuseAddress );
    }
}
