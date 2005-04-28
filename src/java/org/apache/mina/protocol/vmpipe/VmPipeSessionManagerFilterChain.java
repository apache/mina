package org.apache.mina.protocol.vmpipe;

import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.ProtocolSessionManager;
import org.apache.mina.protocol.ProtocolSessionManagerFilterChain;

class VmPipeSessionManagerFilterChain extends ProtocolSessionManagerFilterChain {

    VmPipeSessionManagerFilterChain( ProtocolSessionManager manager )
    {
        super( manager );
    }

    protected void doWrite( ProtocolSession session, Object message )
    {
        VmPipeSession s = ( VmPipeSession ) session;
        
        synchronized( s.lock )
        {
            if( s.closed )
                throw new IllegalStateException( "Session is closed." );
            s.remoteSession.getManagerFilterChain().messageReceived( s.remoteSession, message );
        }
    }
}
