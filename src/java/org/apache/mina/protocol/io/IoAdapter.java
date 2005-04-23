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

import java.net.SocketAddress;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.SessionConfig;
import org.apache.mina.common.TransportType;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoSession;
import org.apache.mina.protocol.ProtocolCodecFactory;
import org.apache.mina.protocol.ProtocolDecoder;
import org.apache.mina.protocol.ProtocolDecoderOutput;
import org.apache.mina.protocol.ProtocolEncoder;
import org.apache.mina.protocol.ProtocolEncoderOutput;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolHandlerFilterChain;
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
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class IoAdapter
{
    private static final String KEY = "IoAdapter.ProtocolSession";

    private final IoProtocolFilterChain filters = new IoProtocolFilterChain();

    IoAdapter()
    {
    }
    
    public ProtocolHandlerFilterChain getFilterChain()
    {
        return filters;
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
        private final ProtocolCodecFactory codecFactory;
        private final ProtocolHandler handler;

        public SessionHandlerAdapter( ProtocolProvider protocolProvider )
        {
            codecFactory = protocolProvider.getCodecFactory();
            this.handler = protocolProvider.getHandler();
        }

        public void sessionOpened( IoSession session )
        {
            filters.sessionOpened( getProtocolSession( session ) );
        }

        public void sessionClosed( IoSession session )
        {
            filters.sessionClosed( getProtocolSession( session ) );
        }

        public void sessionIdle( IoSession session, IdleStatus status )
        {
            filters.sessionIdle( getProtocolSession( session ), status );
        }

        public void exceptionCaught( IoSession session, Throwable cause )
        {
            filters.exceptionCaught( getProtocolSession( session ), cause );
        }

        public void dataRead( IoSession session, ByteBuffer in )
        {
            ProtocolSessionImpl psession = getProtocolSession( session );
            ProtocolDecoder decoder = psession.decoder;
            try
            {
                synchronized( decoder )
                {
                    decoder.decode( psession, in, psession.decOut );
                }

                Queue queue = psession.decOut.messageQueue;
                synchronized( queue )
                {
                    if( !queue.isEmpty() )
                    {
                        do
                        {
                            filters.messageReceived( psession, queue.pop() );
                        }
                        while( !queue.isEmpty() );
                    }
                }
            }
            catch( ProtocolViolationException pve )
            {
                pve.setBuffer( in );
                filters.exceptionCaught( psession, pve );
            }
            catch( Throwable t )
            {
                filters.exceptionCaught( psession, t );
            }
        }

        public void dataWritten( IoSession session, Object marker )
        {
            if( marker == null )
                return;
            filters.messageSent( getProtocolSession( session ),
                                 marker );
        }

        void doWrite( IoSession session )
        {
            ProtocolSessionImpl psession = getProtocolSession( session );
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

                        Queue queue = psession.encOut.queue;
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
                filters.exceptionCaught( psession, t );
            }
        }

        private ProtocolSessionImpl getProtocolSession( IoSession session )
        {
            ProtocolSessionImpl psession =
                ( ProtocolSessionImpl ) session.getAttribute( KEY );
            if( psession == null )
            {
                synchronized( session )
                {
                    psession =
                        ( ProtocolSessionImpl ) session.getAttribute( KEY );
                    if( psession == null )
                    {
                        psession = new ProtocolSessionImpl( session, this );
                        session.setAttribute( KEY, psession );
                    }
                }
            }

            return psession;
        }
    }

    class ProtocolSessionImpl implements ProtocolSession
    {
        final IoSession session;

        final SessionHandlerAdapter adapter;

        final Queue writeQueue = new Queue();
        
        private final ProtocolEncoder encoder;
        
        private final ProtocolDecoder decoder;

        private final ProtocolEncoderOutputImpl encOut;

        private final ProtocolDecoderOutputImpl decOut;

        private ProtocolSessionImpl( IoSession session,
                                    SessionHandlerAdapter adapter )
        {
            this.session = session;
            this.adapter = adapter;
            this.encoder = adapter.codecFactory.newEncoder();
            this.decoder = adapter.codecFactory.newDecoder();
            this.encOut = new ProtocolEncoderOutputImpl();
            this.decOut = new ProtocolDecoderOutputImpl();
        }

        public ProtocolHandler getHandler()
        {
            return adapter.handler;
        }

        public ProtocolEncoder getEncoder()
        {
            return encoder;
        }

        public ProtocolDecoder getDecoder()
        {
            return decoder;
        }

        public void close()
        {
            session.close();
        }
        
        public void close( boolean wait )
        {
            session.close( wait );
        }

        public Object getAttachment()
        {
            return session.getAttachment();
        }

        public Object setAttachment( Object attachment )
        {
            return session.setAttachment( attachment );
        }

        public Object getAttribute( String key )
        {
            return session.getAttribute( key );
        }

        public Object setAttribute( String key, Object value )
        {
            return session.setAttribute( key, value );
        }
        
        public Object removeAttribute( String key )
        {
            return session.removeAttribute( key );
        }

        public Set getAttributeKeys()
        {
            return session.getAttributeKeys();
        }

        public void write( Object message )
        {
            filters.filterWrite( this, message );
        }

        public TransportType getTransportType()
        {
            return session.getTransportType();
        }

        public boolean isConnected()
        {
            return session.isConnected();
        }

        public SessionConfig getConfig()
        {
            return session.getConfig();
        }

        public SocketAddress getRemoteAddress()
        {
            return session.getRemoteAddress();
        }

        public SocketAddress getLocalAddress()
        {
            return session.getLocalAddress();
        }

        public long getReadBytes()
        {
            return session.getReadBytes();
        }

        public long getWrittenBytes()
        {
            return session.getWrittenBytes();
        }

        public long getLastIoTime()
        {
            return session.getLastIoTime();
        }

        public long getLastReadTime()
        {
            return session.getLastReadTime();
        }

        public long getLastWriteTime()
        {
            return session.getLastWriteTime();
        }

        public boolean isIdle( IdleStatus status )
        {
            return session.isIdle( status );
        }
    }

    private static class ProtocolEncoderOutputImpl implements
            ProtocolEncoderOutput
    {

        private final Queue queue = new Queue();

        private ProtocolEncoderOutputImpl()
        {
        }

        public void write( ByteBuffer buf )
        {
            queue.push( buf );
        }
        
        public void mergeAll()
        {
            int sum = 0;
            final int size = queue.size();
            
            if( size < 2 )
            {
                // no need to merge!
                return;
            }
            
            // Get the size of merged BB
            for( int i = size - 1; i >= 0; i -- )
            {
                sum += ( ( ByteBuffer ) queue.get( i ) ).remaining();
            }
            
            // Allocate a new BB that will contain all fragments
            ByteBuffer newBuf = ByteBuffer.allocate( sum );
            
            // and merge all.
            for( ;; )
            {
                ByteBuffer buf = ( ByteBuffer ) queue.pop();
                if( buf == null )
                {
                    break;
                }

                newBuf.put( buf );
                buf.release();
            }
            
            // Push the new buffer finally.
            newBuf.flip();
            queue.push(newBuf);
        }
    }

    private static class ProtocolDecoderOutputImpl implements
            ProtocolDecoderOutput
    {

        private final Queue messageQueue = new Queue();

        private ProtocolDecoderOutputImpl()
        {
        }

        public void write( Object message )
        {
            messageQueue.push( message );
        }
    }
}
