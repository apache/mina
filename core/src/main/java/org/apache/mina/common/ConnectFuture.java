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
package org.apache.mina.common;

import java.io.IOException;

/**
 * An {@link IoFuture} for asynchronous connect requests.
 *
 * <h3>Example</h3>
 * <pre>
 * IoConnector connector = ...;
 * ConnectFuture future = connector.connect(...);
 * future.join(); // Wait until the connection attempt is finished.
 * IoSession session = future.getSession();
 * session.write(...);
 * </pre>
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ConnectFuture extends IoFuture
{
    /**
     * Returns a new {@link ConnectFuture} which is already marked as 'failed to connect'.
     */
    public static ConnectFuture newFailedFuture( IOException exception )
    {
        ConnectFuture failedFuture = new ConnectFuture();
        failedFuture.setException( exception );
        return failedFuture;
    }
    
    public ConnectFuture()
    {
    }
    
    /**
     * Creates a new instance which uses the specified object as a lock.
     */
    public ConnectFuture( Object lock )
    {
        super( lock );
    }

    /**
     * Returns {@link IoSession} which is the result of connect operation.
     * 
     * @return <tt>null</tt> if the connect operation is not finished yet
     * @throws IOException if connection attempt failed by an exception
     */
    public IoSession getSession() throws IOException
    {
        Object v = getValue();
        if( v instanceof Throwable )
        {
            throw ( IOException ) new IOException( "Failed to get the session." ).initCause( ( Throwable ) v );
        }
        else
        {
            return ( IoSession ) v;
        }
    }

    /**
     * Returns <tt>true</tt> if the connect operation is finished successfully.
     */
    public boolean isConnected()
    {
        return getValue() instanceof IoSession;
    }
    
    /**
     * This method is invoked by MINA internally.  Please do not call this method
     * directly.
     */
    public void setSession( IoSession session )
    {
        setValue( session );
    }
    
    /**
     * This method is invoked by MINA internally.  Please do not call this method
     * directly.
     */
    public void setException( Throwable exception )
    {
        setValue( exception );
    }
}
