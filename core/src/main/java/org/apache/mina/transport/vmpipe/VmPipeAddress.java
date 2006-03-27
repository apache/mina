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
package org.apache.mina.transport.vmpipe;

import java.net.SocketAddress;

/**
 * A {@link SocketAddress} which represents in-VM pipe port number.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class VmPipeAddress extends SocketAddress implements Comparable
{
    private static final long serialVersionUID = 3257844376976830515L;

    private final int port;

    /**
     * Creates a new instance with the specifid port number.
     */
    public VmPipeAddress( int port )
    {
        this.port = port;
    }

    /**
     * Returns the port number.
     */
    public int getPort()
    {
        return port;
    }

    public int hashCode()
    {
        return port;
    }

    public boolean equals( Object o )
    {
        if( o == null )
            return false;
        if( this == o )
            return true;
        if( o instanceof VmPipeAddress )
        {
            VmPipeAddress that = ( VmPipeAddress ) o;
            return this.port == that.port;
        }

        return false;
    }

    public int compareTo( Object o )
    {
        return this.port - ( ( VmPipeAddress ) o ).port;
    }

    public String toString()
    {
        return "vm:" + port;
    }
}