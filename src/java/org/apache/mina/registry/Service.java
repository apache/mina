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

import org.apache.mina.common.TransportType;

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

    private final int port;

    /**
     * Creates a new instance with the specified protocol name, transport type,
     * and port number.
     */
    public Service( String name, TransportType transportType, int port )
    {
        if( name == null )
            throw new NullPointerException( "name" );
        if( transportType == null )
            throw new NullPointerException( "transportType" );
        if( port < 0 || port > 65535 )
            throw new IllegalArgumentException( "port: " + port );

        this.name = name;
        this.transportType = transportType;
        this.port = port;
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
     * Returns the port number this service is bound on.
     */
    public int getPort()
    {
        return port;
    }

    public int hashCode()
    {
        return ( ( name.hashCode() * 37 ) ^ transportType.hashCode() * 37 )
                ^ port;
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
                && this.port == that.port;
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
        return "(" + transportType + ", " + name + ", " + port + ')';
    }
}
