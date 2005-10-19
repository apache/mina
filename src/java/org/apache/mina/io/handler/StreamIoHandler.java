package org.apache.mina.io.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.net.SocketTimeoutException;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoHandlerAdapter;
import org.apache.mina.io.IoSession;

/**
 * A {@link IoHandler} that adapts asynchronous MINA events to stream I/O.
 * <p>
 * Please extend this class and implement
 * {@link #processStreamIo(IoSession, InputStream, OutputStream)} to
 * execute your stream I/O logic; <b>please note that you must forward
 * the process request to other thread or thread pool.</b>
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class StreamIoHandler extends IoHandlerAdapter
{
    private static final String KEY_IN = "BlockingIoHandler.in";
    private static final String KEY_OUT = "BlockingIoHandler.out";
    private static final String KEY_STARTED = "BlockingIoHandler.started";
    
    private int readTimeout;
    
    private int writeTimeout;
    
    protected StreamIoHandler()
    {
    }
    
    /**
     * Implement this method to execute your stream I/O logic;
     * <b>please note that you must forward the process request to other
     * thread or thread pool.</b>
     */
    protected abstract void processStreamIo( IoSession session,
                                             InputStream in, OutputStream out );
    
    /**
     * Returns read timeout in seconds.
     * The default value is <tt>0</tt> (disabled).
     */
    public int getReadTimeout()
    {
        return readTimeout;
    }
    
    /**
     * Sets read timeout in seconds.
     * The default value is <tt>0</tt> (disabled).
     */
    public void setReadTimeout( int readTimeout )
    {
        this.readTimeout = readTimeout;
    }
    
    /**
     * Returns write timeout in seconds.
     * The default value is <tt>0</tt> (disabled).
     */
    public int getWriteTimeout()
    {
        return writeTimeout;
    }
    
    /**
     * Sets write timeout in seconds.
     * The default value is <tt>0</tt> (disabled).
     */
    public void setWriteTimeout( int writeTimeout )
    {
        this.writeTimeout = writeTimeout;
    }

    /**
     * Initializes streams and timeout settings.
     */
    public void sessionOpened( IoSession session )
    {
        // Set timeouts
        session.getConfig().setWriteTimeout( writeTimeout );
        session.getConfig().setIdleTime( IdleStatus.READER_IDLE, readTimeout );

        // Create streams
        PipedOutputStream out = new PipedOutputStream();
        session.setAttribute( KEY_OUT, out );
        try
        {
            session.setAttribute( KEY_IN, new PipedInputStream( out ) );
        }
        catch( IOException e )
        {
            throw new StreamIoException( e );
        }
    }
    
    /**
     * Closes input stream.
     */
    public void sessionClosed( IoSession session )
    {
        final PipedOutputStream out = ( PipedOutputStream ) session.getAttribute( KEY_OUT );
        try {
            out.close();
        }
        catch( IOException e )
        {
            throw new StreamIoException( e );
        }
    }

    /**
     * Forwards read data to input stream.
     */
    public void dataRead( IoSession session, ByteBuffer buf )
    {
        final PipedInputStream in = ( PipedInputStream ) session.getAttribute( KEY_IN );
        final PipedOutputStream out = ( PipedOutputStream ) session.getAttribute( KEY_OUT );
        
        java.nio.ByteBuffer nioBuf = buf.buf();
        int offset = nioBuf.position();
        int length = nioBuf.limit() - offset;
        if( !nioBuf.hasArray() )
        {
            ByteBuffer heapBuf = ByteBuffer.allocate( length, false );
            heapBuf.put( buf );
            heapBuf.flip();
            nioBuf = heapBuf.buf();
            offset = 0;
        }
        
        try
        {
            out.write( nioBuf.array(), offset, length );
        }
        catch( IOException e )
        {
            throw new StreamIoException( e );
        }
        finally
        {
            beginService( session, in );
        }
    }

    /**
     * Forwards caught exceptions to input stream.
     */
    public void exceptionCaught( IoSession session, Throwable cause )
    {
        final PipedInputStream in = ( PipedInputStream ) session.getAttribute( KEY_IN );
        
        IOException e = null;
        if( cause instanceof StreamIoException )
        {
            e = ( IOException ) cause.getCause();
        }
        else if( cause instanceof IOException )
        {
            e = ( IOException ) cause;
        }
        
        if( e != null && in != null )
        {
            in.setException( e );
            beginService( session, in );
        }
        else
        {
            cause.printStackTrace();
            session.close();
        }
    }

    /**
     * Handles read timeout.
     */
    public void sessionIdle( IoSession session, IdleStatus status )
    {
        if( status == IdleStatus.READER_IDLE )
        {
            throw new StreamIoException(
                    new SocketTimeoutException( "Read timeout" ) );
        }
    }

    private void beginService( IoSession session, PipedInputStream in )
    {
        if( session.getAttribute( KEY_STARTED ) == null )
        {
            session.setAttribute( KEY_STARTED, Boolean.TRUE );
            processStreamIo( session, in, new ServiceOutputStream( session ) );
        }
    }

    private static class PipedInputStream extends java.io.PipedInputStream
    {
        private IOException exception;

        public PipedInputStream(PipedOutputStream src) throws IOException
        {
            super( src );
        }
        
        public void setException( IOException e )
        {
            this.exception = e;
        }

        public synchronized int read() throws IOException
        {
            throwException();
            return super.read();
        }

        public synchronized int read( byte[] b, int off, int len ) throws IOException
        {
            throwException();
            return super.read( b, off, len );
        }
        
        private void throwException() throws IOException
        {
            if( exception != null )
            {
                throw exception;
            }
        }
    }

    private static class ServiceOutputStream extends OutputStream
    {
        private final IoSession session;
        
        public ServiceOutputStream( IoSession session )
        {
            this.session = session;
        }

        public void close()
        {
            session.close( true );
        }

        public void flush()
        {
        }

        public void write( byte[] b, int off, int len )
        {
            ByteBuffer buf = ByteBuffer.wrap( b, off, len );
            buf.acquire(); // prevent from being pooled.
            session.write( buf, null );
        }

        public void write( byte[] b )
        {
            ByteBuffer buf = ByteBuffer.wrap( b );
            buf.acquire(); // prevent from being pooled.
            session.write( buf, null );
        }

        public void write( int b )
        {
            ByteBuffer buf = ByteBuffer.allocate( 1 );
            buf.put( ( byte ) b );
            buf.flip();
            session.write( buf, null );
        }
    }
    
    private static class StreamIoException extends RuntimeException
    {
        private static final long serialVersionUID = 3976736960742503222L;

        public StreamIoException( IOException cause )
        {
            super(cause);
        }
    }
}
