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
package org.apache.mina.integration.spring;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.mina.integration.spring.support.AbstractIoAcceptorFactoryBean;
import org.springframework.util.Assert;

/**
 * Common base class for factory beans creating IoAcceptor instances which bind
 * to InetSocketAddress addresses.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class InetSocketAddressBindingIoAcceptorFactoryBean extends
        AbstractIoAcceptorFactoryBean
{

    /**
     * Parses the specified string and returns a corresponding
     * InetSocketAddress. E.g.: <code>google.com:80</code>, <code>:22</code>,
     * <code>192.168.0.1:110</code>.
     * 
     * @param s
     *            the string to parse. An optional host or ip address followed
     *            by a colon and a port number (<em>[host|ip]:port</em>).
     * @return the SocketAddress.
     */
    protected SocketAddress parseSocketAddress( String s )
    {
        Assert.notNull( s, "null SocketAddress string" );
        s = s.trim();
        int colonIndex = s.indexOf( ":" );
        if( colonIndex > 0 )
        {
            String host = s.substring( 0, colonIndex );
            int port = parsePort( s.substring( colonIndex + 1 ) );
            return new InetSocketAddress( host, port );
        }
        else
        {
            int port = parsePort( s.substring( colonIndex + 1 ) );
            return new InetSocketAddress( port );
        }
    }

    /**
     * Parses a port number by calling
     * {@link Integer#parseInt(java.lang.String)}.
     * 
     * @param s
     *            the textual representation of the port.
     * @return the port number.
     * @throws IllegalArgumentException
     *             if {@link Integer#parseInt(java.lang.String)} throws a
     *             NumberFormatException.
     */
    protected int parsePort( String s )
    {
        try
        {
            return Integer.parseInt( s );
        }
        catch( NumberFormatException nfe )
        {
            throw new IllegalArgumentException( "Illegal port number: " + s );
        }
    }
}
