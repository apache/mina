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
package org.apache.mina.transport.vmpipe;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;

/**
 * Makes sure if the order of event is correct.
 * 
 * @author The Apache MINA Project Team (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class VmPipeEventOrderTest extends TestCase
{
    public void testServerToClient() throws Exception
    {
        IoAcceptor acceptor = new VmPipeAcceptor();
        acceptor.getDefaultConfig().setThreadModel( ThreadModel.MANUAL );
        //acceptor.getFilterChain().addLast( "logger", new LoggingFilter() );

        IoConnector connector = new VmPipeConnector();
        connector.getDefaultConfig().setThreadModel( ThreadModel.MANUAL );
        //connector.getFilterChain().addLast( "logger", new LoggingFilter() );
        
        acceptor.bind(
                new VmPipeAddress( 1 ),
                new IoHandlerAdapter() {
                    public void sessionOpened( IoSession session ) throws Exception
                    {
                        session.write("B");
                    }
                    
                    public void messageSent( IoSession session, Object message ) throws Exception
                    {
                        session.close();
                    }
                });
        
        final StringBuffer actual = new StringBuffer();
        
        ConnectFuture future = connector.connect(
                new VmPipeAddress( 1 ),
                new IoHandlerAdapter() {

                    public void messageReceived( IoSession session, Object message ) throws Exception
                    {
                        actual.append( message );
                    }

                    public void sessionClosed( IoSession session ) throws Exception
                    {
                        actual.append( "C" );
                    }

                    public void sessionOpened( IoSession session ) throws Exception {
                        actual.append( "A" );
                    }
                    
                });

        future.join();
        future.getSession().getCloseFuture().join();
        acceptor.unbindAll();
        
        // sessionClosed() might not be invoked yet
        // even if the connection is closed.
        while( actual.indexOf("C") < 0 )
        {
            Thread.yield();
        }
        
        Assert.assertEquals( "ABC", actual.toString() );
    }

    public void testClientToServer() throws Exception
    {
        IoAcceptor acceptor = new VmPipeAcceptor();
        acceptor.getDefaultConfig().setThreadModel( ThreadModel.MANUAL );
        //acceptor.getFilterChain().addLast( "logger", new LoggingFilter() );

        IoConnector connector = new VmPipeConnector();
        connector.getDefaultConfig().setThreadModel( ThreadModel.MANUAL );
        //connector.getFilterChain().addLast( "logger", new LoggingFilter() );
        
        final StringBuffer actual = new StringBuffer();
        
        acceptor.bind(
                new VmPipeAddress( 1 ),
                new IoHandlerAdapter() {

                    public void messageReceived( IoSession session, Object message ) throws Exception
                    {
                        actual.append( message );
                    }

                    public void sessionClosed( IoSession session ) throws Exception
                    {
                        actual.append( "C" );
                    }

                    public void sessionOpened( IoSession session ) throws Exception {
                        actual.append( "A" );
                    }
                    
                });
        
        ConnectFuture future = connector.connect(
                new VmPipeAddress( 1 ),
                new IoHandlerAdapter() {
                    public void sessionOpened( IoSession session ) throws Exception
                    {
                        session.write("B");
                    }
                    
                    public void messageSent( IoSession session, Object message ) throws Exception
                    {
                        session.close();
                    }
                });

        future.join();
        future.getSession().getCloseFuture().join();
        acceptor.unbindAll();
        
        // sessionClosed() might not be invoked yet
        // even if the connection is closed.
        while( actual.indexOf("C") < 0 )
        {
            Thread.yield();
        }
        
        Assert.assertEquals( "ABC", actual.toString() );
    }
}
