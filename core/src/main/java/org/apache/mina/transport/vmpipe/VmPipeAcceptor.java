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

import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.support.BaseIoAcceptor;
import org.apache.mina.common.support.BaseIoSessionConfig;
import org.apache.mina.transport.vmpipe.support.VmPipe;

/**
 * Binds the specified {@link IoHandler} to the specified
 * {@link VmPipeAddress}.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class VmPipeAcceptor extends BaseIoAcceptor
{
    static final Map<VmPipeAddress, VmPipe> boundHandlers = new HashMap<VmPipeAddress, VmPipe>();
    
    private static final IoSessionConfig CONFIG = new BaseIoSessionConfig() {};

    /**
     * Creates a new instance.
     */
    public VmPipeAcceptor()
    {
    }

    protected Class<? extends SocketAddress> getAddressType()
    {
        return VmPipeAddress.class;
    }

    protected void doBind() throws IOException
    {
        VmPipeAddress localAddress = ( VmPipeAddress ) getLocalAddress();
        
        synchronized( boundHandlers )
        {
            if( localAddress == null || localAddress.getPort() == 0 )
            {
                for( int i = 1; i < Integer.MAX_VALUE; i++ )
                {
                    localAddress = new VmPipeAddress( i );
                    if( !boundHandlers.containsKey( localAddress ) )
                    {
                        break;
                    }
                }
            }
            else if( boundHandlers.containsKey( localAddress ) )
            {
                throw new IOException( "Address already bound: " + localAddress );
            }

            boundHandlers.put(
                    localAddress, 
                    new VmPipe( this, localAddress, getHandler(), getListeners() ) );
        }
        
        setLocalAddress( localAddress );
        getListeners().fireServiceActivated();
    }
    
    protected void doUnbind()
    {
        synchronized( boundHandlers )
        {
            boundHandlers.remove( getLocalAddress() );
        }
        
        getListeners().fireServiceDeactivated();
    }

    public IoSessionConfig getSessionConfig()
    {
        return CONFIG;
    }

    public IoSession newSession( SocketAddress remoteAddress )
    {
        throw new UnsupportedOperationException();
    }
}
