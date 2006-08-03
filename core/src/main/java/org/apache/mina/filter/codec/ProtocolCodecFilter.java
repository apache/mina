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
package org.apache.mina.filter.codec;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ByteBufferProxy;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.common.support.BaseIoSession;
import org.apache.mina.common.support.DefaultWriteFuture;
import org.apache.mina.filter.codec.support.SimpleProtocolDecoderOutput;
import org.apache.mina.filter.codec.support.SimpleProtocolEncoderOutput;
import org.apache.mina.util.SessionLog;

/**
 * An {@link IoFilter} which translates binary or protocol specific data into
 * message object and vice versa using {@link ProtocolCodecFactory},
 * {@link ProtocolEncoder}, or {@link ProtocolDecoder}.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ProtocolCodecFilter extends IoFilterAdapter
{
    public static final String ENCODER = ProtocolCodecFilter.class.getName() + ".encoder";
    public static final String DECODER = ProtocolCodecFilter.class.getName() + ".decoder";
    
    private static final Class[] EMPTY_PARAMS = new Class[0];

    private final ProtocolCodecFactory factory;
    
    public ProtocolCodecFilter( ProtocolCodecFactory factory )
    {
        if( factory == null )
        {
            throw new NullPointerException( "factory" );
        }
        this.factory = factory;
    }
    
    public ProtocolCodecFilter( final ProtocolEncoder encoder, final ProtocolDecoder decoder )
    {
        if( encoder == null )
        {
            throw new NullPointerException( "encoder" );
        }
        if( decoder == null )
        {
            throw new NullPointerException( "decoder" );
        }
        
        this.factory = new ProtocolCodecFactory()
        {
            public ProtocolEncoder getEncoder()
            {
                return encoder;
            }

            public ProtocolDecoder getDecoder()
            {
                return decoder;
            }
        };
    }
    
    public ProtocolCodecFilter( final Class encoderClass, final Class decoderClass )
    {
        if( encoderClass == null )
        {
            throw new NullPointerException( "encoderClass" );
        }
        if( decoderClass == null )
        {
            throw new NullPointerException( "decoderClass" );
        }
        if( !ProtocolEncoder.class.isAssignableFrom( encoderClass ) )
        {
            throw new IllegalArgumentException( "encoderClass: " + encoderClass.getName() );
        }
        if( !ProtocolDecoder.class.isAssignableFrom( decoderClass ) )
        {
            throw new IllegalArgumentException( "decoderClass: " + decoderClass.getName() );
        }
        try
        {
            encoderClass.getConstructor( EMPTY_PARAMS );
        }
        catch( NoSuchMethodException e )
        {
            throw new IllegalArgumentException( "encoderClass doesn't have a public default constructor." );
        }
        try
        {
            decoderClass.getConstructor( EMPTY_PARAMS );
        }
        catch( NoSuchMethodException e )
        {
            throw new IllegalArgumentException( "decoderClass doesn't have a public default constructor." );
        }
        
        this.factory = new ProtocolCodecFactory()
        {
            public ProtocolEncoder getEncoder() throws Exception
            {
                return ( ProtocolEncoder ) encoderClass.newInstance();
            }

            public ProtocolDecoder getDecoder() throws Exception
            {
                return ( ProtocolDecoder ) decoderClass.newInstance();
            }
        };
    }

    public void onPreAdd( IoFilterChain parent, String name, NextFilter nextFilter ) throws Exception
    {
        if( parent.contains( ProtocolCodecFilter.class ) )
        {
            throw new IllegalStateException( "A filter chain cannot contain more than one ProtocolCodecFilter." );
        }
    }

    public void messageReceived( NextFilter nextFilter, IoSession session, Object message ) throws Exception
    {
        if( !( message instanceof ByteBuffer ) )
        {
            nextFilter.messageReceived( session, message );
            return;
        }

        ByteBuffer in = ( ByteBuffer ) message;
        ProtocolDecoder decoder = getDecoder( session );
        ProtocolDecoderOutput decoderOut = getDecoderOut( session, nextFilter );
        
        try
        {
            decoder.decode( session, in, decoderOut );
        }
        catch( Throwable t )
        {
            ProtocolDecoderException pde;
            if( t instanceof ProtocolDecoderException )
            {
                pde = ( ProtocolDecoderException ) t;
            }
            else
            {
                pde = new ProtocolDecoderException( t );
            }
            pde.setHexdump( in.getHexDump() );
            throw pde;
        }
        finally
        {
            // Dispose the decoder if this session is connectionless.
            if( session.getTransportType().isConnectionless() )
            {
                disposeDecoder( session );
            }

            // Release the read buffer.
            in.release();

            decoderOut.flush();
        }
    }

    public void messageSent( NextFilter nextFilter, IoSession session, Object message ) throws Exception
    {
        if( ! ( message instanceof MessageByteBuffer ) )
        {
            nextFilter.messageSent( session, message );
            return;
        }

        MessageByteBuffer buf = ( MessageByteBuffer ) message;
        try
        {
            buf.release();
        }
        finally
        {
            nextFilter.messageSent( session, buf.message );
        }
    }
    
    public void filterWrite( NextFilter nextFilter, IoSession session, WriteRequest writeRequest ) throws Exception
    {
        Object message = writeRequest.getMessage();
        if( message instanceof ByteBuffer )
        {
            nextFilter.filterWrite( session, writeRequest );
            return;
        }

        ProtocolEncoder encoder = getEncoder( session );
        ProtocolEncoderOutputImpl encoderOut = getEncoderOut( session, nextFilter, writeRequest );
        
        try
        {
            encoder.encode( session, message, encoderOut );
            
            ((BaseIoSession)session).increaseWrittenMessages();
        }
        catch( Throwable t )
        {
            ProtocolEncoderException pee;
            if( t instanceof ProtocolEncoderException )
            {
                pee = ( ProtocolEncoderException ) t;
            }
            else
            {
                pee = new ProtocolEncoderException( t );
            }
            throw pee;
        }
        finally
        {
            encoderOut.flush();

            // Dispose the encoder if this session is connectionless.
            if( session.getTransportType().isConnectionless() )
            {
                disposeEncoder( session );
            }
        }
    }
    
    public void sessionClosed( NextFilter nextFilter, IoSession session ) throws Exception
    {
        disposeEncoder( session );
        disposeDecoder( session );
        nextFilter.sessionClosed( session );
    }

    private ProtocolEncoder getEncoder( IoSession session ) throws Exception
    {
        ProtocolEncoder encoder = ( ProtocolEncoder ) session.getAttribute( ENCODER );
        if( encoder == null )
        {
            encoder = factory.getEncoder();
            session.setAttribute( ENCODER, encoder );
        }
        return encoder;
    }
    
    private ProtocolEncoderOutputImpl getEncoderOut( IoSession session, NextFilter nextFilter, WriteRequest writeRequest )
    {
        return new ProtocolEncoderOutputImpl( session, nextFilter, writeRequest );
    }
    
    private ProtocolDecoder getDecoder( IoSession session ) throws Exception
    {
        ProtocolDecoder decoder = ( ProtocolDecoder ) session.getAttribute( DECODER );
        if( decoder == null )
        {
            decoder = factory.getDecoder();
            session.setAttribute( DECODER, decoder );
        }
        return decoder;
    }
    
    private ProtocolDecoderOutput getDecoderOut( IoSession session, NextFilter nextFilter )
    {
        return new SimpleProtocolDecoderOutput( session, nextFilter );
    }
    
    private void disposeEncoder( IoSession session )
    {
        ProtocolEncoder encoder = ( ProtocolEncoder ) session.removeAttribute( ENCODER );
        if( encoder == null )
        {
            return;
        }

        try
        {
            encoder.dispose( session );
        }
        catch( Throwable t )
        {
            SessionLog.warn(
                    session,
                    "Failed to dispose: " + encoder.getClass().getName() +
                    " (" + encoder + ')' );
        }
    }

    private void disposeDecoder( IoSession session )
    {
        ProtocolDecoder decoder = ( ProtocolDecoder ) session.removeAttribute( DECODER );
        if( decoder == null )
        {
            return;
        }

        try
        {
            decoder.dispose( session );
        }
        catch( Throwable t )
        {
            SessionLog.warn(
                    session,
                    "Falied to dispose: " + decoder.getClass().getName() +
                    " (" + decoder + ')' );
        }
    }

    private static class MessageByteBuffer extends ByteBufferProxy
    {
        private final Object message;
        
        private MessageByteBuffer( ByteBuffer buf, Object message )
        {
            super( buf );
            this.message = message;
        }
    }
    
    private static class ProtocolEncoderOutputImpl extends SimpleProtocolEncoderOutput
    {
        private final IoSession session;
        private final NextFilter nextFilter;
        private final WriteRequest writeRequest;
        
        public ProtocolEncoderOutputImpl( IoSession session, NextFilter nextFilter, WriteRequest writeRequest )
        {
            this.session = session;
            this.nextFilter = nextFilter;
            this.writeRequest = writeRequest;
        }

        protected WriteFuture doFlush( ByteBuffer buf )
        {
            WriteFuture future;
            if( writeRequest != null )
            {
                future = writeRequest.getFuture();
                nextFilter.filterWrite(
                        session,
                        new WriteRequest(
                                new MessageByteBuffer(
                                        buf, writeRequest.getMessage() ), future, writeRequest.getDestination() ) );
            }
            else
            {
                future = new DefaultWriteFuture( session );
                nextFilter.filterWrite( session, new WriteRequest( buf, future ) );
            }
            return future;
        }
    }
}
