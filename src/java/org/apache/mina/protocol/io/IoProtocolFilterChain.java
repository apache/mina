package org.apache.mina.protocol.io;

import org.apache.mina.protocol.AbstractProtocolHandlerFilterChain;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.util.Queue;

class IoProtocolFilterChain extends AbstractProtocolHandlerFilterChain {

    IoProtocolFilterChain()
    {
    }

    protected void doWrite( ProtocolSession session, Object message )
    {
        IoProtocolSession s = ( IoProtocolSession ) session;
        Queue writeQueue = s.writeQueue;
        synchronized( writeQueue )
        {
            writeQueue.push( message );
        }

        s.shAdapter.doWrite( s.session );
    }
}
