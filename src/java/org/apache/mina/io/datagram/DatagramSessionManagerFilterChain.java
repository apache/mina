package org.apache.mina.io.datagram;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.io.IoFilterChain;
import org.apache.mina.io.IoSession;
import org.apache.mina.io.IoSessionManagerFilterChain;
import org.apache.mina.util.Queue;

/**
 * An {@link IoFilterChain} for datagram transport (UDP/IP).
 * 
 * @author The Apache Directory Project
 */
class DatagramSessionManagerFilterChain extends IoSessionManagerFilterChain {

    DatagramSessionManagerFilterChain( DatagramSessionManager processor )
    {
        super( processor );
    }
    
    protected void doWrite( IoSession session, ByteBuffer buf, Object marker )
    {
        DatagramSession s = ( DatagramSession ) session;
        Queue writeBufferQueue = s.getWriteBufferQueue();
        Queue writeMarkerQueue = s.getWriteMarkerQueue();
        
        synchronized( writeBufferQueue )
        {
            writeBufferQueue.push( buf );
            writeMarkerQueue.push( marker );
            if( writeBufferQueue.size() == 1 )
            {
                // Notify DatagramSessionManager only when writeBufferQueue was empty.
                ( ( DatagramSessionManager ) getManager() ).flushSession( s );
            }
        }
    }
}
