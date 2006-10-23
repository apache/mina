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
package org.apache.mina.example.proxy;

import java.net.SocketAddress;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.TrafficMask;

/**
 * Handles the client to proxy part of the proxied connection.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 *
 */
public class ClientToProxyIoHandler extends AbstractProxyIoHandler
{
    private final ServerToProxyIoHandler connectorHandler = new ServerToProxyIoHandler(); 
    private final IoConnector connector; 
    private final SocketAddress remoteAddress;

    public ClientToProxyIoHandler( IoConnector connector, SocketAddress remoteAddress )
    {
        this.connector = connector;
        this.remoteAddress = remoteAddress;
        connector.setHandler( connectorHandler );
    }

    public void sessionOpened( final IoSession session ) throws Exception 
    {
        
        connector.connect( remoteAddress ).addListener( 
                new IoFutureListener()
        {
            public void operationComplete( IoFuture f )
            {
                ConnectFuture future = ( ConnectFuture ) f;
                try
                {
                    future.getSession().setAttachment( session );
                    session.setAttachment( future.getSession() );
                    future.getSession().setTrafficMask( TrafficMask.ALL );
                }
                catch( RuntimeIOException e )
                {
                    // Connect failed
                    session.close();
                }
                finally
                {
                    session.setTrafficMask( TrafficMask.ALL );
                }
            }
        } );
    }
}
