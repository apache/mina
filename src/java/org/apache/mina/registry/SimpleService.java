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

import org.apache.mina.common.TransportType;

/**
 * Default implementation of {@link Service}.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class SimpleService implements Service
{

    private final String name;

    private final TransportType transportType;

    private final int port;

    /**
     * Creates a new instance with the specified protocol name, transport type,
     * and port number.
     */
    public SimpleService( String name, TransportType transportType, int port )
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

    public String getName()
    {
        return name;
    }

    public TransportType getTransportType()
    {
        return transportType;
    }

    public int getPort()
    {
        return port;
    }
}