package org.apache.mina.io.socket;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.FilterChainType;
import org.apache.mina.io.AbstractIoHandlerFilterChain;
import org.apache.mina.io.IoSession;
import org.apache.mina.util.Queue;

class SocketFilterChain extends AbstractIoHandlerFilterChain {

    SocketFilterChain( FilterChainType type )
    {
        super( type );
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
