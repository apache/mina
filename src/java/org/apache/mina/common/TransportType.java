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
package org.apache.mina.common;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Represents network transport types.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class TransportType implements Serializable
{
    private static final long serialVersionUID = 3258132470497883447L;

    /**
     * Transport type: TCP/IP (<code>SocketChannel</code>)
     */
    public static final TransportType SOCKET = new TransportType( "SOCKET",
            false );

    /**
     * Transport type: UDP/IP (<code>DatagramChannel</code>)
     */
    public static final TransportType DATAGRAM = new TransportType(
            "DATAGRAM", true );

    /**
     * Transport type: VM pipe (direct message exchange)
     */
    public static final TransportType VM_PIPE = new TransportType( "VM_PIPE",
            false );

    private final String strVal;

    private final boolean stateless;

    /**
     * Creates a new instance.
     */
    private TransportType( String strVal, boolean stateless )
    {
        this.strVal = strVal;
        this.stateless = stateless;
    }

    /**
     * Returns <code>true</code> if the session of this transport type is
     * stateless.
     */
    public boolean isStateless()
    {
        return stateless;
    }

    public String toString()
    {
        return strVal;
    }
    
    private Object readResolve() throws ObjectStreamException
    {
        if( strVal.equals( SOCKET.toString() ) )
            return SOCKET;
        if( strVal.equals( DATAGRAM.toString() ) )
            return DATAGRAM;
        if( strVal.equals( VM_PIPE.toString() ) )
            return VM_PIPE;
        else
            throw new InvalidObjectException( "Unknown transport type: "
                    + this );
    }
}
