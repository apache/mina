/*
 * @(#) $Id$
 */
package org.apache.mina.protocol.vmpipe;

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

        vps.bothIdle = vps.readerIdle = false;
        vps.lastReadTime = System.currentTimeMillis();

        // fire messageSent event first
        vps.remoteFilterManager.fireMessageSent( vps.remoteSession, message );

        // and then messageReceived
        nextHandler.messageReceived( session, message );
    }

    public void messageSent( ProtocolHandler nextHandler,
                            ProtocolSession session, Object message )
    {
        VmPipeSession vps = ( VmPipeSession ) session;
        vps.bothIdle = vps.writerIdle = false;
        vps.lastWriteTime = System.currentTimeMillis();

        nextHandler.messageSent( session, message );
    }
}