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
package org.apache.mina.examples.echoserver.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/**
 * Factory to create a bougus SSLContext.
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class BogusSSLContextFactory
{

    /**
     * Protocol to use.
     */
    private static final String PROTOCOL = "TLS";

    /**
     * Bougus Server certificate keystore file name.
     */
    private static final String BOGUS_KEYSTORE = "bogus.cert";

    // NOTE: The keystore was generated using keytool:
    //   keytool -genkey -alias bogus -keysize 512 -validity 3650
    //           -keyalg RSA -dname "CN=bogus.com, OU=XXX CA,
    //               O=Bogus Inc, L=Stockholm, S=Stockholm, C=SE"
    //           -keypass boguspw -storepass boguspw -keystore bogus.cert

    /**
     * Bougus keystore password.
     */
    private static final char[] BOGUS_PW = { 'b', 'o', 'g', 'u', 's', 'p',
                                            'w' };

    private static SSLContext serverInstance = null;

    private static SSLContext clientInstance = null;

    /**
     * Get SSLContext singleton.
     *
     * @return SSLContext
     * @throws java.security.GeneralSecurityException
     *
     */
    public static SSLContext getInstance( boolean server )
            throws GeneralSecurityException
    {
        SSLContext retInstance = null;
        if( server )
        {
            if( serverInstance == null )
            {
                synchronized( BogusSSLContextFactory.class )
                {
                    if( serverInstance == null )
                    {
                        try
                        {
                            serverInstance = createBougusServerSSLContext();
                        }
                        catch( Exception ioe )
                        {
                            throw new GeneralSecurityException(
                                    "Can't create Server SSLContext:" + ioe );
                        }
                    }
                }
            }
            retInstance = serverInstance;
        }
        else
        {
            if( clientInstance == null )
            {
                synchronized( BogusSSLContextFactory.class )
                {
                    if( clientInstance == null )
                    {
                        clientInstance = createBougusClientSSLContext();
                    }
                }
            }
            retInstance = clientInstance;
        }
        return retInstance;
    }

    private static SSLContext createBougusServerSSLContext()
            throws GeneralSecurityException, IOException
    {
        // Create keystore
        KeyStore ks = KeyStore.getInstance( "JKS" );
        InputStream in = null;
        try
        {
            in = BogusSSLContextFactory.class
                    .getResourceAsStream( BOGUS_KEYSTORE );
            ks.load( in, BOGUS_PW );
        }
        finally
        {
            if( in != null )
            {
                try
                {
                    in.close();
                }
                catch( IOException ignored )
                {
                }
            }
        }

        // Set up key manager factory to use our key store
        KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
        kmf.init( ks, BOGUS_PW );

        // Initialize the SSLContext to work with our key managers.
        SSLContext sslContext = SSLContext.getInstance( PROTOCOL );
        sslContext.init( kmf.getKeyManagers(),
                BogusTrustManagerFactory.X509_MANAGERS, null );

        return sslContext;
    }

    private static SSLContext createBougusClientSSLContext()
            throws GeneralSecurityException
    {
        SSLContext context = SSLContext.getInstance( PROTOCOL );
        context.init( null, BogusTrustManagerFactory.X509_MANAGERS, null );
        return context;
    }

}
