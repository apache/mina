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
import java.util.Set;

import org.apache.mina.common.TransportType;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoHandlerFilter;
import org.apache.mina.protocol.ProtocolHandlerFilter;
import org.apache.mina.protocol.ProtocolProvider;

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
    /**
     * Binds the specified I/O handler to the specified service.
     */
    void bind( Service service, IoHandler ioHandler ) throws IOException;

    /**
     * Binds the specified protocol provider to the specified service.
     */
    void bind( Service service, ProtocolProvider protocolProvider )
            throws IOException;

    /**
     * Unbinds the specified service (and its aggregated I/O handler or
     * protocol provider). 
     */
    void unbind( Service service );

    /**
     * Adds the specified filter to the acceptors of all transport types
     * in this registry.
     */
    void addFilter( int priority, IoHandlerFilter filter );
    
    /**
     * Adds the specified filter to the acceptors of all transport types
     * in this registry.
     */
    void addFilter( int priority, ProtocolHandlerFilter filter );
    
    /**
     * Adds the specified filter to the acceptor of the specified transport
     * type with the specified priority.
     */
    void addFilter( TransportType transportType, int priority,
                   IoHandlerFilter filter );

    /**
     * Adds the specified filter to the acceptor of the specified transport
     * type with the specified priority.
     */
    void addFilter( TransportType transportType, int priority,
                   ProtocolHandlerFilter filter );

    /**
     * Removes the specified filter from the acceptors of all transport types
     * in this registry.
     */
    void removeFilter( IoHandlerFilter filter );

    /**
     * Removes the specified filter from the acceptors of all transport types
     * in this registry.
     */
    void removeFilter( ProtocolHandlerFilter filter );

    /**
     * Removes the specified filter from the acceptor of the specified
     * transport type.
     */
    void removeFilter( TransportType transportType, IoHandlerFilter filter );

    /**
     * Removes the specified filter from the acceptor of the specified
     * transport type.
     */
    void removeFilter( TransportType transportType,
                      ProtocolHandlerFilter filter );

    /**
     * Returns {@link Set} of all services bound in this registry.
     */
    Set getAllServices();
    
    /**
     * Returns {@link Set} of services bound in this registry with the
     * specified service(or protocol) name.
     */
    Set getServices(String name);

    /**
     * Returns {@link Set} of services bound in this registry with the
     * specified transport type.
     */
    Set getServices(TransportType transportType);
    
    /**
     * Returns {@link Set} of services bound in this registry with the
     * specified port number.
     */
    Set getServices(int port);
}
