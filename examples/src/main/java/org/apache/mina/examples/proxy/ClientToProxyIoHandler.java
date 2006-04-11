/*
 *   @(#) $Id$
 *
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.mina.examples.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoSession;
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
    private final ServerToProxyIoHandler connectorHandler; 
    private final IoConnector connector; 
    private final InetSocketAddress address;

    public ClientToProxyIoHandler( ServerToProxyIoHandler connectorHandler, 
                        IoConnector connector, InetSocketAddress address )
    {
        this.connectorHandler = connectorHandler;
        this.connector = connector;
        this.address = address;
    }

    public void sessionOpened( final IoSession session ) throws Exception 
    {
        connector.connect( address, connectorHandler ).setCallback( 
                new IoFuture.Callback()
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
                catch( IOException e )
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
