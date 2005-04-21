/*
 * @(#) $Id$
 */
package org.apache.mina.protocol.vmpipe;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolHandlerFilterAdapter;
import org.apache.mina.protocol.ProtocolSession;

/**
 * Sets last(Read|Write)Time for {@link VmPipeSession}s. 
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
class VmPipeFilter extends ProtocolHandlerFilterAdapter
{
    public void messageReceived( ProtocolHandler nextHandler,
                                 ProtocolSession session, Object message )
    {
        VmPipeSession vps = ( VmPipeSession ) session;

        vps.setIdle( IdleStatus.BOTH_IDLE, false );
        vps.setIdle( IdleStatus.READER_IDLE, false );
        vps.increaseReadBytes( 0 );

        // fire messageSent event first
        vps.remoteFilters.messageSent( vps.remoteSession, message );

        // and then messageReceived
        nextHandler.messageReceived( session, message );
    }

    public void messageSent( ProtocolHandler nextHandler,
                            ProtocolSession session, Object message )
    {
        VmPipeSession vps = ( VmPipeSession ) session;
        vps.setIdle( IdleStatus.BOTH_IDLE, false );
        vps.setIdle( IdleStatus.WRITER_IDLE, false );
        vps.increaseWrittenBytes( 0 );

        nextHandler.messageSent( session, message );
    }
}