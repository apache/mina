package org.apache.mina.io.datagram;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.io.AbstractIoHandlerFilterChain;
import org.apache.mina.io.IoSession;
import org.apache.mina.util.Queue;

/**
 * An {@link IoHandlerFilterChain} for datagram transport (UDP/IP).
 * 
 * @author The Apache Directory Project
 */
class DatagramFilterChain extends AbstractIoHandlerFilterChain {

    private final DatagramProcessor processor;

    DatagramFilterChain( DatagramProcessor processor )
    {
        this.processor = processor;
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
        }

        processor.flushSession( s );
    }
}
