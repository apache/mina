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

import java.io.IOException;
import java.util.Iterator;

import org.apache.mina.common.TransportType;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoHandlerFilter;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolHandlerFilter;

/**
 * Interface for the internet service registry. The registry is used by MINA
 * to associate services with ports and transport protocols.
 * 
 * @author akarasulu@apache.org
 * @author trustin@apache.org
 * @version $Rev$, $Date$
 */
public interface ServiceRegistry
{
    void bind( Service service, IoHandler sessionHandler ) throws IOException;

    void bind( Service service, ProtocolHandler sessionHandler )
            throws IOException;

    void unbind( Service service );

    void addFilter( int priority, IoHandlerFilter filter );

    void addFilter( int priority, ProtocolHandlerFilter filter );

    void addFilter( Service service, int priority, IoHandlerFilter filter );

    void addFilter( Service service, int priority, ProtocolHandlerFilter filter );

    void removeFilter( IoHandlerFilter filter );

    void removeFilter( ProtocolHandlerFilter filter );

    Service getByName( String name, TransportType transportType );

    Service getByPort( int port, TransportType transportType );

    Iterator getAll();

    Iterator getByTransportType( TransportType transportType );

    /**
     * Gets an iteration over all the entries for a service by the name of the
     * service.
     * 
     * @param name
     *            the authoritative name of the service
     * @return an Iterator over InetServiceEntry objects
     */
    Iterator getByName( String name );

    /**
     * Gets an iteration over all the entries for a service by port number.
     * This method returns an Iterator over the set of InetServiceEntry objects
     * since more than one transport protocol can be used on the same port.
     * 
     * @param port
     *            the port one which the service resides
     * @return an Iterator over InetServiceEntry objects
     */
    Iterator getByPort( int port );
}