package org.apache.mina.protocol.vmpipe;

import org.apache.mina.common.FilterChainType;
import org.apache.mina.protocol.AbstractProtocolHandlerFilterChain;
import org.apache.mina.protocol.ProtocolSession;

class VmPipeFilterChain extends AbstractProtocolHandlerFilterChain {

    VmPipeFilterChain( FilterChainType type )
    {
        super(type);
    }

    protected void doWrite(ProtocolSession session, Object message)
    {
        VmPipeSession s = ( VmPipeSession ) session;
        
        synchronized( s.lock )
        {
            if( s.closed )
                throw new IllegalStateException( "Session is closed." );
            s.remoteFilters.messageReceived( null,
                                             s.remoteSession, message );
        }
    }
}
