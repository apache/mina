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

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.util.ExpirationListener;
import org.apache.mina.util.ExpiringMap;

/**
 * An {@link IoSessionRecycler} with sessions that time out on inactivity.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * 
 * TODO Change time unit to 'seconds'.
 */
public class ExpiringSessionRecycler implements IoSessionRecycler, ExpirationListener
{
    private ExpiringMap sessionMap;

    public ExpiringSessionRecycler()
    {
        this( ExpiringMap.DEFAULT_EXPIRATION_TIME, ExpiringMap.DEFAULT_EXPIRER_DELAY );
    }

    public ExpiringSessionRecycler( long expirationTimeMillis )
    {
        this( expirationTimeMillis, ExpiringMap.DEFAULT_EXPIRER_DELAY );
    }

    public ExpiringSessionRecycler( long expirationTimeMillis, long expirerDelay )
    {
        // FIXME Use IdentityHashMap if possible.
        sessionMap = new ExpiringMap( expirationTimeMillis, expirerDelay );
        sessionMap.addExpirationListener( this );
    }

    public void put( IoSession session )
    {
        Object key = generateKey( session );
        if ( !sessionMap.containsKey( key ) )
        {
            sessionMap.put( key, session );
        }
    }

    public IoSession recycle( SocketAddress localAddress, SocketAddress remoteAddress )
    {
        Object key = generateKey( localAddress, remoteAddress );
        return ( IoSession ) sessionMap.get( key );
    }

    public void remove( IoSession session )
    {
        Object key = generateKey( session );
        sessionMap.remove( key );
    }

    public void expired( Object expiredObject )
    {
        IoSession expiredSession = ( IoSession ) expiredObject;
        expiredSession.close();
    }

    private Object generateKey( IoSession session )
    {
        return generateKey( session.getLocalAddress(), session.getRemoteAddress() );
    }

    private Object generateKey( SocketAddress localAddress, SocketAddress remoteAddress )
    {
        List key = new ArrayList( 2 );
        key.add( remoteAddress );
        key.add( localAddress );
        return key;
    }
}
