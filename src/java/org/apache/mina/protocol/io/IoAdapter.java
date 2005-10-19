/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.mina.protocol.io;


import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoSession;
import org.apache.mina.protocol.ProtocolCodecFactory;
import org.apache.mina.protocol.ProtocolDecoder;
import org.apache.mina.protocol.ProtocolEncoder;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolFilterChain;
import org.apache.mina.protocol.ProtocolProvider;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.ProtocolViolationException;
import org.apache.mina.util.Queue;

/**
 * Adapts the specified {@link ProtocolProvider} to {@link IoHandler}.
 * This class is used by {@link IoProtocolAcceptor} and {@link IoProtocolConnector}
 * internally.
 * <p>
 * It is a bridge between I/O layer and Protocol layer.  Protocol layer itself
 * cannot do any real I/O, but it translates I/O events to more higher level
 * ones and vice versa.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
class IoAdapter
{
    private static final String KEY = "IoAdapter.ProtocolSession";

    private final IoProtocolSessionManagerFilterChain managerFilterChain;

    IoAdapter( IoProtocolSessionManagerFilterChain filters )
    {
        this.managerFilterChain = filters;
    }
    
    public ProtocolFilterChain getFilterChain()
    {
        return managerFilterChain;
    }

    /**
     * Converts the specified <code>protocolProvider</code> to {@link IoAdapter}
     * to use for actual I/O.
     * 
     * @return a new I/O handler for the specified <code>protocolProvider</code>
     */
    public IoHandler adapt( ProtocolProvider protocolProvider )
    {
        return new SessionHandlerAdapter( protocolProvider );
    }

    /**
     * Returns {@link ProtocolSession} of the specified {@link IoSession}.
     */
    public ProtocolSession toProtocolSession( IoSession session )
    {
        IoHandler handler = session.getHandler();
        if( handler instanceof SessionHandlerAdapter )
        {
            SessionHandlerAdapter sha = ( SessionHandlerAdapter ) handler;
            return sha.getProtocolSession( session );
        }
        else
        {
            throw new IllegalArgumentException( "Not adapted from IoAdapter." );
        }
    }

    class SessionHandlerAdapter implements IoHandler
    {
        final ProtocolCodecFactory codecFactory;
        final ProtocolHandler handler;

        public SessionHandlerAdapter( ProtocolProvider protocolProvider )
        {
            codecFactory = protocolProvider.getCodecFactory();
            this.handler = protocolProvider.getHandler();
        }
        
        public void sessionCreated( IoSession session ) throws Exception
        {
            handler.sessionCreated( getProtocolSession( session ) );
        }

        public void sessionOpened( IoSession session )
        {
            managerFilterChain.sessionOpened( getProtocolSession( session ) );
        }

        public void sessionClosed( IoSession session )
        {
            managerFilterChain.sessionClosed( getProtocolSession( session ) );
        }

        public void sessionIdle( IoSession session, IdleStatus status )
        {
            managerFilterChain.sessionIdle( getProtocolSession( session ), status );
        }

        public void exceptionCaught( IoSession session, Throwable cause )
        {
            managerFilterChain.exceptionCaught( getProtocolSession( session ), cause );
        }

        public void dataRead( IoSession session, ByteBuffer in )
        {
            IoProtocolSession psession = getProtocolSession( session );
            ProtocolDecoder decoder = psession.decoder;
            try
            {
                synchronized( decoder )
                {
                    decoder.decode( psession, in, psession.decOut );
                }

                Queue queue = psession.decOut.getMessageQueue();
                synchronized( queue )
                {
                    if( !queue.isEmpty() )
                    {
                        do
                        {
                            managerFilterChain.messageReceived( psession, queue.pop() );
                        }
                        while( !queue.isEmpty() );
                    }
                }
            }
            catch( ProtocolViolationException pve )
            {
                pve.setHexdump( in.getHexDump() );
                managerFilterChain.exceptionCaught( psession, pve );
            }
            catch( Throwable t )
            {
                managerFilterChain.exceptionCaught( psession, t );
            }
        }

        public void dataWritten( IoSession session, Object marker )
        {
            if( marker == null )
                return;
            managerFilterChain.messageSent( getProtocolSession( session ),
                                 marker );
        }

        void doWrite( IoSession session )
        {
            IoProtocolSession psession = getProtocolSession( session );
            ProtocolEncoder encoder = psession.encoder;
            Queue writeQueue = psession.writeQueue;

            if( writeQueue.isEmpty() )
            {
                return;
            }

            try
            {
                while( !writeQueue.isEmpty() )
                {
                    synchronized( writeQueue )
                    {
                        Object message = writeQueue.pop();
                        if( message == null )
                            break;

                        Queue queue = psession.encOut.getBufferQueue();
                        encoder.encode( psession, message, psession.encOut );
                        for( ;; )
                        {
                            ByteBuffer buf = ( ByteBuffer ) queue.pop();
                            if( buf == null )
                                break;
                            // use marker only if it is the last ByteBuffer
                            Object marker = queue.isEmpty() ? message : null;
                            session.write( buf, marker );
                        }
                    }
                }
            }
            catch( Throwable t )
            {
                managerFilterChain.exceptionCaught( psession, t );
            }
        }

        private IoProtocolSession getProtocolSession( IoSession session )
        {
            IoProtocolSession psession =
                ( IoProtocolSession ) session.getAttribute( KEY );
            if( psession == null )
            {
                synchronized( session )
                {
                    psession =
                        ( IoProtocolSession ) session.getAttribute( KEY );
                    if( psession == null )
                    {
                        psession = new IoProtocolSession(
                                IoAdapter.this.managerFilterChain, session, this );
                        session.setAttribute( KEY, psession );
                    }
                }
            }

            return psession;
        }
    }
}
