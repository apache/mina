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
package org.apache.mina.examples.httpserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.mina.common.IoSession;
import org.apache.mina.handler.StreamIoHandler;

/**
 * A simplistic HTTP protocol handler that replies back the URL and headers
 * which a client requested.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class HttpProtocolHandler extends StreamIoHandler
{
    protected void processStreamIo( IoSession session, InputStream in,
                                    OutputStream out )
    {
        // You *MUST* execute stream I/O logic in a separate thread.
        new Worker( in, out ).start();
    }
    
    private static class Worker extends Thread
    {
        private final InputStream in;
        private final OutputStream out;
        
        public Worker( InputStream in, OutputStream out )
        {
            setDaemon( true );
            this.in = in;
            this.out = out;
        }
        
        public void run()
        {
            String url;
            Map headers = new TreeMap();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader( this.in ) );
            PrintWriter out = new PrintWriter(
                    new BufferedWriter( new OutputStreamWriter( this.out ) ) );
            
            try
            {
                // Get request URL.
                url = in.readLine().split( " " )[1];
                
                // Read header
                String line;
                while ( ( line = in.readLine() ) != null && !line.equals( "" ) )
                {
                    String[] tokens = line.split(": ");
                    headers.put( tokens[0], tokens[1] );
                }
                
                // Write header
                out.println( "HTTP/1.0 200 OK" );
                out.println( "Content-Type: text/html" );
                out.println( "Server: MINA Example" );
                out.println();
                
                // Write content
                out.println( "<html><head></head><body>" );
                out.println( "<h3>Request Summary for: " + url + "</h3>" );
                out.println( "<table border=\"1\"><tr><th>Key</th><th>Value</th></tr>" );
                
                Iterator it = headers.entrySet().iterator();
                while( it.hasNext() )
                {
                    Entry e = ( Entry ) it.next();
                    out.println( "<tr><td>" + e.getKey() + "</td><td>" + e.getValue() + "</td></tr>" );
                }
                
                out.println( "</table>" );
                out.println( "</body></html>" );
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
            finally
            {
                out.flush();
                out.close();
                try
                {
                    in.close();
                }
                catch( IOException e )
                {
                }
            }
        }
    }
}
