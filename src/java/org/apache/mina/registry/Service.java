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
package org.apache.mina.registry;

import java.io.Serializable;
import java.net.SocketAddress;

import org.apache.mina.common.TransportType;
import org.apache.mina.protocol.vmpipe.VmPipeAddress;

/**
 * Represents a service that is registered to {@link ServiceRegistry}.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class Service implements Serializable, Cloneable
{
    private static final long serialVersionUID = 3258407344110383155L;

    private final String name;

    private final TransportType transportType;

    private final SocketAddress address;

    /**
     * Creates a new instance with the specified protocol name, transport type,
     * and socket address to be bound.
     */
    public Service( String name, TransportType transportType, SocketAddress address )
    {
        if( name == null )
            throw new NullPointerException( "name" );
        if( transportType == null )
            throw new NullPointerException( "transportType" );
        if( address == null )
            throw new NullPointerException( "address" );

        if( transportType == TransportType.VM_PIPE &&
            !( address instanceof VmPipeAddress ) )
        {
            throw new IllegalArgumentException(
                    "VM_PIPE transport type accepts only VmPipeAddress: " + address.getClass() );
        }

        this.name = name;
        this.transportType = transportType;
        this.address = address;
    }

    /**
     * Returns the name of this service (protocol).
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the transport type this service uses.
     */
    public TransportType getTransportType()
    {
        return transportType;
    }

    /**
     * Returns the socket address this service is bound on.
     */
    public SocketAddress getAddress()
    {
        return address;
    }

    public int hashCode()
    {
        return ( ( name.hashCode() * 37 ) ^ transportType.hashCode() * 37 )
                ^ address.hashCode();
    }

    public boolean equals( Object o )
    {
        if( o == null )
            return false;
        if( this == o )
            return true;
        if( !( o instanceof Service ) )
            return false;

        Service that = ( Service ) o;
        return this.name.equals( that.name )
                && this.transportType == that.transportType
                && this.address.equals( that.address );
    }

    public Object clone()
    {
        try
        {
            return super.clone();
        }
        catch( CloneNotSupportedException e )
        {
            throw new InternalError();
        }
    }
    
    public String toString() {
        return "(" + transportType + ", " + name + ", " + address + ')';
    }
}
