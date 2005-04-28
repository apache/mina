package org.apache.mina.io.socket;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.io.IoHandlerFilterChain;
import org.apache.mina.io.IoSession;
import org.apache.mina.io.IoSessionManager;
import org.apache.mina.io.IoSessionManagerFilterChain;
import org.apache.mina.util.Queue;

/**
 * An {@link IoHandlerFilterChain} for socket transport (TCP/IP).
 * 
 * @author The Apache Directory Project
 */
class SocketSessionManagerFilterChain extends IoSessionManagerFilterChain {

    SocketSessionManagerFilterChain( IoSessionManager manager )
    {
        super( manager );
    }

    protected void doWrite( IoSession session, ByteBuffer buf, Object marker )
    {
        SocketSession s = ( SocketSession ) session;
        Queue writeBufferQueue = s.getWriteBufferQueue();
        Queue writeMarkerQueue = s.getWriteMarkerQueue();
        
        synchronized( writeBufferQueue )
        {
            writeBufferQueue.push( buf );
            writeMarkerQueue.push( marker );
        }

        SocketIoProcessor.getInstance().flushSession( s );
    }
}
