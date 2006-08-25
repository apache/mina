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
package org.apache.mina.common.support;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;

/**
 * A default implementation of {@link ConnectFuture}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultConnectFuture extends DefaultIoFuture implements ConnectFuture
{
    /**
     * Returns a new {@link ConnectFuture} which is already marked as 'failed to connect'.
     */
    public static ConnectFuture newFailedFuture( Throwable exception )
    {
        DefaultConnectFuture failedFuture = new DefaultConnectFuture();
        failedFuture.setException( exception );
        return failedFuture;
    }
    
    /**
     * Creates a new instance.
     */
    public DefaultConnectFuture()
    {
        super( null );
    }
    
    /**
     * Creates a new instance which uses the specified object as a lock.
     */
    public DefaultConnectFuture( Object lock )
    {
        super( null, lock );
    }

    public IoSession getSession() throws RuntimeIOException
    {
        Object v = getValue();
        if( v instanceof RuntimeIOException )
        {
            throw ( RuntimeIOException ) v;
        }
        else if( v instanceof Throwable )
        {
            throw ( RuntimeIOException ) new RuntimeIOException( "Failed to get the session." ).initCause( ( Throwable ) v );
        }
        else
        {
            return ( IoSession ) v;
        }
    }

    public boolean isConnected()
    {
        return getValue() instanceof IoSession;
    }
    
    public void setSession( IoSession session )
    {
        setValue( session );
    }
    
    public void setException( Throwable exception )
    {
        setValue( exception );
    }
}
