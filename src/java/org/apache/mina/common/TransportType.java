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
 * Represents network transport types.  MINA provides three transport types:
 * <ul>
 *   <li>{@link #SOCKET} - TCP/IP</li>
 *   <li>{@link #DATAGRAM} - UDP/IP</li>
 *   <li>{@link #VM_PIPE} - in-VM pipe support (only available in protocol
 *       layer</li>
 * </ul> 
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
     * Transport type: VM pipe (direct message exchange).
     * Please refer to
     * <a href="../protocol/vmpipe/package-summary.htm"><tt>org.apache.mina.protocol.vmpipe</tt></a>
     * package.
     */
    public static final TransportType VM_PIPE = new TransportType( "VM_PIPE",
            false );

    /**
     * Returns the transport type of the specified name.  Here are the list
     * of available names:
     * <ul>
     *   <li><code>"socket"</code> or <code>"tcp"</code> returns {@link #SOCKET}</li>
     *   <li><code>"datagram"</code> or <code>"udp"</code> returns {@link #DATAGRAM}</li>
     *   <li><code>"vm_pipe"</code> returns {@link #VM_PIPE}</li>
     * </ul>
     * All names are case-insensitive.
     * 
     * @param name the name of the transport type
     * @return the transport type
     * @throws IllegalArgumentException if the specified name is not available.
     */
    public static TransportType getInstance(String name)
    {
        if( "socket".equalsIgnoreCase(name) || "tcp".equalsIgnoreCase(name) )
        {
            return SOCKET;
        }

        if( "datagram".equalsIgnoreCase(name) || "udp".equalsIgnoreCase(name) )
        {
            return DATAGRAM;
        }
        
        if( "vm_pipe".equalsIgnoreCase(name) )
        {
            return VM_PIPE;
        }
        
        throw new IllegalArgumentException("Unknown transport type name: " + name);
    }

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
