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
package org.apache.mina.examples.proxy;

import java.nio.charset.Charset;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.TrafficMask;
import org.apache.mina.util.SessionLog;

/**
 * Base class of {@link org.apache.mina.common.IoHandler} classes which handle
 * proxied connections.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 *
 */
public abstract class AbstractProxyIoHandler extends IoHandlerAdapter
{
    private static Charset CHARSET = Charset.forName( "iso8859-1" );
    
    public void sessionCreated( IoSession session ) throws Exception
    {
        session.setTrafficMask( TrafficMask.NONE );
    }

    public void sessionClosed( IoSession session ) throws Exception 
    {
        if( session.getAttachment() != null )
        {
            ( ( IoSession ) session.getAttachment() ).setAttachment( null );
            ( ( IoSession ) session.getAttachment() ).close();
            session.setAttachment( null );
        }
    }
    
    public void messageReceived( IoSession session, Object message ) throws Exception
    {
        ByteBuffer rb = ( ByteBuffer ) message;
        ByteBuffer wb = ByteBuffer.allocate( rb.remaining() );
        rb.mark();
        wb.put( rb );
        wb.flip();
        ( ( IoSession ) session.getAttachment() ).write( wb );
        rb.reset();
        SessionLog.info( session, rb.getString( CHARSET.newDecoder() ) );
    }
}
