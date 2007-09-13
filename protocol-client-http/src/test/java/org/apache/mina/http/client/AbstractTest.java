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
package org.apache.mina.http.client;


import java.io.File;

import junit.framework.TestCase;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Embedded;


public abstract class AbstractTest extends TestCase
{

    protected final File BASEDIR = getBaseDir();
    protected final File CATALINAHOME = new File( BASEDIR, "src/test/catalina" );
    protected final File WORK = new File( BASEDIR, "target/work" );
    protected final File KEYSTORE = new File( CATALINAHOME, "conf/keystore" );
    protected final File WEBAPPS = new File( CATALINAHOME, "webapps" );
    protected final File ROOT = new File( WEBAPPS, "ROOT" );
    protected Embedded server;


    protected void setUp() throws Exception
    {
        System.out.println( "BASEDIR = " + BASEDIR.getAbsolutePath() );
        server = new Embedded();
        server.setCatalinaHome( CATALINAHOME.getAbsolutePath() );

        Engine engine = server.createEngine();
        engine.setDefaultHost( "localhost" );

        Host host = server.createHost( "localhost", WEBAPPS.getAbsolutePath() );
        ( ( StandardHost ) host ).setWorkDir( WORK.getAbsolutePath() );
        engine.addChild( host );

        Context context = server.createContext( "", ROOT.getAbsolutePath() );
        context.setParentClassLoader( Thread.currentThread().getContextClassLoader() );
        host.addChild( context );

        server.addEngine( engine );

        //Http
        Connector http = server.createConnector( "localhost", 8282, false );
        server.addConnector( http );

        //Https
        Connector https = server.createConnector( "localhost", 8383, true );
        https.setAttribute( "keystoreFile", KEYSTORE.getAbsolutePath() );
        server.addConnector( https );
        server.start();
    }


    protected void tearDown() throws Exception
    {
        if ( server != null )
            server.stop();
    }


    protected final File getBaseDir()
    {
        File dir;

        // If ${basedir} is set, then honor it
        String tmp = System.getProperty( "basedir" );
        if ( tmp != null )
        {
            dir = new File( tmp );
        }
        else
        {
            // Find the directory which this class (or really the sub-class of TestSupport) is defined in.
            String path = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();

            // We expect the file to be in target/test-classes, so go up 2 dirs
            dir = new File( path ).getParentFile().getParentFile();

            // Set ${basedir} which is needed by logging to initialize
            System.setProperty( "basedir", dir.getPath() );
        }

        return dir;
    }
}
