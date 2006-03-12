package org.apache.mina.transport.vmpipe.support;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.support.AbstractIoFilterChain;

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
                        byteCount = rb.remaining();
                        ByteBuffer wb = ByteBuffer.allocate( rb.remaining() );
                        wb.put( rb );
                        wb.flip();
                        messageCopy = wb;
                    }
                    
                    s.increaseWrittenBytes( byteCount );
                    s.increaseWrittenWriteRequests();
    
                    ( ( VmPipeFilterChain ) s.getFilterChain() ).messageSent( s, message );
                    ( ( VmPipeFilterChain ) s.remoteSession.getFilterChain() )
                                .messageReceived( s.remoteSession, messageCopy );
                    
                    writeRequest.getFuture().setWritten( true );
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
                s.getManagedSessions().remove( s );
                ( ( VmPipeFilterChain ) s.getFilterChain() ).sessionClosed( session );
                session.getCloseFuture().setClosed();
                s.remoteSession.close();
            }
        }
    }
    
}
