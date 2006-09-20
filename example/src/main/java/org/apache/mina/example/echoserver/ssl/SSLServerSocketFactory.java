/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.mina.examples.echoserver.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.GeneralSecurityException;

import javax.net.ServerSocketFactory;

/**
 * Simple Server Socket factory to create sockets with or without SSL enabled.
 * If SSL enabled a "bougus" SSL Context is used (suitable for test purposes)
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SSLServerSocketFactory extends javax.net.ServerSocketFactory
{
    private static boolean sslEnabled = false;

    private static javax.net.ServerSocketFactory sslFactory = null;

    private static ServerSocketFactory factory = null;

    public SSLServerSocketFactory()
    {
        super();
    }

    public ServerSocket createServerSocket( int port ) throws IOException
    {
        return new ServerSocket( port );
    }

    public ServerSocket createServerSocket( int port, int backlog )
            throws IOException
    {
        return new ServerSocket( port, backlog );
    }

    public ServerSocket createServerSocket( int port, int backlog,
                                           InetAddress ifAddress )
            throws IOException
    {
        return new ServerSocket( port, backlog, ifAddress );
    }

    public static javax.net.ServerSocketFactory getServerSocketFactory()
            throws IOException
    {
        if( isSslEnabled() )
        {
            if( sslFactory == null )
            {
                try
                {
                    sslFactory = BogusSSLContextFactory.getInstance( true )
                            .getServerSocketFactory();
                }
                catch( GeneralSecurityException e )
                {
                    IOException ioe = new IOException(
                            "could not create SSL socket" );
                    ioe.initCause( e );
                    throw ioe;
                }
            }
            return sslFactory;
        }
        else
        {
            if( factory == null )
            {
                factory = new SSLServerSocketFactory();
            }
            return factory;
        }

    }

    public static boolean isSslEnabled()
    {
        return sslEnabled;
    }

    public static void setSslEnabled( boolean newSslEnabled )
    {
        sslEnabled = newSslEnabled;
    }
}
