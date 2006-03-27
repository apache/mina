/*
 *   @(#) $Id: StreamIoHandler.java 350169 2005-12-01 05:17:41Z trustin $
 *
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.mina.transport.vmpipe.support;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.support.AbstractIoFilterChain;

/**
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev: 350169 $, $Date: 2005-12-01 14:17:41 +0900 (Thu, 01 Dec 2005) $
 */
public class VmPipeFilterChain extends AbstractIoFilterChain {

    public VmPipeFilterChain( IoSession session )
    {
        super( session );
    }

    public void messageReceived( IoSession session, Object message )
    {
        VmPipeSessionImpl s = ( VmPipeSessionImpl ) session;
        synchronized( s.lock )
        {
            if( !s.getTrafficMask().isReadable() )
            {
                synchronized( s.pendingDataQueue )
                {
                    s.pendingDataQueue.push( message );
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
                
                super.messageReceived( s, message );
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
                        s.pendingDataQueue.push( writeRequest );
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
                    s.increaseWrittenWriteRequests();
    
                    ( ( VmPipeFilterChain ) s.getFilterChain() ).messageSent( s, writeRequest );
                    ( ( VmPipeFilterChain ) s.remoteSession.getFilterChain() )
                                .messageReceived( s.remoteSession, messageCopy );
                }
            }
            else 
            {
                ( ( VmPipeFilterChain ) s.getFilterChain() ).messageNotSent( s, writeRequest );
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
                s.getManagedSessions().remove( s );
                ( ( VmPipeFilterChain ) s.getFilterChain() ).sessionClosed( session );
                s.remoteSession.close();
            }
        }
    }
    
}
