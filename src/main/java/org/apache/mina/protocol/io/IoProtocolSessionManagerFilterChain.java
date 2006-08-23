package org.apache.mina.protocol.io;

import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.ProtocolSessionManager;
import org.apache.mina.protocol.ProtocolSessionManagerFilterChain;
import org.apache.mina.util.Queue;

class IoProtocolSessionManagerFilterChain extends ProtocolSessionManagerFilterChain {

    IoProtocolSessionManagerFilterChain( ProtocolSessionManager manager )
    {
        super( manager );
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
