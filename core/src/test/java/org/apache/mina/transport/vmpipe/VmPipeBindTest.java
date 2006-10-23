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

import java.net.SocketAddress;
import java.util.Collection;

import junit.framework.Assert;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.transport.AbstractBindTest;

/**
 * Tests {@link VmPipeAcceptor} bind and unbind.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$ 
 */
public class VmPipeBindTest extends AbstractBindTest
{

    public VmPipeBindTest()
    {
        super( new VmPipeAcceptor() );
    }

    protected SocketAddress createSocketAddress( int port )
    {
        return new VmPipeAddress( port );
    }
    
    public void testUnbindDisconnectsClients() throws Exception
    {
        // TODO: This test is almost identical to the test with the same name in SocketBindTest
        bind( false );
        
        SocketAddress addr = createSocketAddress( port );
     
        IoConnector connector = new VmPipeConnector();
        connector.setHandler( new IoHandlerAdapter() );
        IoSession[] sessions = new IoSession[ 5 ];
        for( int i = 0; i < sessions.length; i++ )
        {
            ConnectFuture future = connector.connect( addr );
            future.join();
            sessions[ i ] = future.getSession();
            Assert.assertTrue( sessions[ i ].isConnected() );
        }
        
        // Wait for the server side sessions to be created.
        Thread.sleep( 500 );
        
        Collection managedSessions = acceptor.getManagedSessions();
        Assert.assertEquals( 5, managedSessions.size() );
        // Make sure it's the server side sessions we get when calling getManagedSessions()
        for( int i = 0; i < sessions.length; i++ )
        {
            Assert.assertFalse( managedSessions.contains( sessions[ i ] ) );
        }
        
        acceptor.unbind();
        
        // Wait for the client side sessions to close.
        Thread.sleep( 500 );
             
        for( int i = 0; i < sessions.length; i++ )
        {
            Assert.assertFalse( sessions[ i ].isConnected() );
        }
    }
}
