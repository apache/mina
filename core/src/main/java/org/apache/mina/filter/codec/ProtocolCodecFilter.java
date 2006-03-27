package org.apache.mina.filter.codec;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ByteBufferProxy;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.filter.codec.support.SimpleProtocolDecoderOutput;
import org.apache.mina.filter.codec.support.SimpleProtocolEncoderOutput;
import org.apache.mina.util.Queue;
import org.apache.mina.util.SessionLog;

public class ProtocolCodecFilter extends IoFilterAdapter
{
    public static final String ENCODER = ProtocolCodecFilter.class.getName() + ".encoder";
    public static final String DECODER = ProtocolCodecFilter.class.getName() + ".decoder";
    public static final String ENCODER_OUT = ProtocolCodecFilter.class.getName() + ".encoderOutput";
    public static final String DECODER_OUT = ProtocolCodecFilter.class.getName() + ".decoderOutput";
    
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
        SimpleProtocolDecoderOutput decoderOut = getDecoderOut( session );
        
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

            Queue queue = decoderOut.getMessageQueue();
            while( !queue.isEmpty() )
            {
                nextFilter.messageReceived( session, queue.pop() );
            }
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
        ProtocolEncoderOutputImpl encoderOut = getEncoderOut( session );
        encoderOut.nextFilter = nextFilter;
        
        try
        {
            encoder.encode( session, message, encoderOut );
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
            // Dispose the encoder if this session is connectionless.
            if( session.getTransportType().isConnectionless() )
            {
                disposeEncoder( session );
            }
        }

        encoderOut.writeRequest = writeRequest;
        encoderOut.flush();
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
    
    private ProtocolEncoderOutputImpl getEncoderOut( IoSession session )
    {
        ProtocolEncoderOutputImpl out = ( ProtocolEncoderOutputImpl ) session.getAttribute( ENCODER_OUT );
        if( out == null )
        {
            out = new ProtocolEncoderOutputImpl( session );
            session.setAttribute( ENCODER_OUT, out );
        }
        return out;
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
    
    private SimpleProtocolDecoderOutput getDecoderOut( IoSession session )
    {
        SimpleProtocolDecoderOutput out = ( SimpleProtocolDecoderOutput ) session.getAttribute( DECODER_OUT );
        if( out == null )
        {
            out = new SimpleProtocolDecoderOutput();
            session.setAttribute( DECODER_OUT, out );
        }
        return out;
    }
    
    private void disposeEncoder( IoSession session )
    {
        session.removeAttribute( ENCODER_OUT );
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
        session.removeAttribute( DECODER_OUT );
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
        private NextFilter nextFilter;
        private WriteRequest writeRequest;
        
        public ProtocolEncoderOutputImpl( IoSession session )
        {
            this.session = session;
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
                                        buf, writeRequest.getMessage() ), future ) );
            }
            else
            {
                future = new WriteFuture();
                nextFilter.filterWrite( session, new WriteRequest( buf, future ) );
            }
            return future;
        }
    }
}
