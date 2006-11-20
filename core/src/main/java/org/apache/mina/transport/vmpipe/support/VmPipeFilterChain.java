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
package org.apache.mina.transport.vmpipe.support;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.support.AbstractIoFilterChain;

/**
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class VmPipeFilterChain extends AbstractIoFilterChain {

    public VmPipeFilterChain( IoSession session )
    {
        super( session );
    }

    public void fireMessageReceived( IoSession session, Object message )
    {
        VmPipeSessionImpl s = ( VmPipeSessionImpl ) session;
        synchronized( s.lock )
        {
            if( !s.getTrafficMask().isReadable() )
            {
                synchronized( s.pendingDataQueue )
                {
                    s.pendingDataQueue.offer( message );
                }
            }
            else
            {
                int byteCount = 1;
                if( message instanceof ByteBuffer )
                {
                    byteCount = ( ( ByteBuffer ) message ).remaining();
                }
                
                s.increaseReadBytes( byteCount );
                
                super.fireMessageReceived( s, message );
            }
        }
    }

    protected void doWrite( IoSession session, WriteRequest writeRequest )
    {
        VmPipeSessionImpl s = ( VmPipeSessionImpl ) session;
        synchronized( s.lock )
        {
            if( s.isConnected() )
            {
                
                if( !s.getTrafficMask().isWritable() )
                {
                    synchronized( s.pendingDataQueue )
                    {
                        s.pendingDataQueue.offer( writeRequest );
                    }
                }
                else
                {
                
                    Object message = writeRequest.getMessage();
                    
                    int byteCount = 1;
                    Object messageCopy = message;
                    if( message instanceof ByteBuffer )
                    {
                        ByteBuffer rb = ( ByteBuffer ) message;
                        rb.mark();
                        byteCount = rb.remaining();
                        ByteBuffer wb = ByteBuffer.allocate( rb.remaining() );
                        wb.put( rb );
                        wb.flip();
                        rb.reset();
                        messageCopy = wb;
                    }
                    
                    s.increaseWrittenBytes( byteCount );
                    s.increaseWrittenMessages();
    
                    s.getFilterChain().fireMessageSent( s, writeRequest );
                    s.getRemoteSession().getFilterChain()
                                .fireMessageReceived( s.getRemoteSession(), messageCopy );
                }
            }
            else 
            {
                writeRequest.getFuture().setWritten( false );
            }
        }
    }

    protected void doClose( IoSession session )
    {
        VmPipeSessionImpl s = ( VmPipeSessionImpl ) session;
        synchronized( s.lock )
        {
            if( !session.getCloseFuture().isClosed() )
            {
                s.getServiceListeners().fireSessionDestroyed( s );
                s.getRemoteSession().close();
            }
        }
    }
    
}
