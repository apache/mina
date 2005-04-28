/**
 * 
 */
package org.apache.mina.protocol.io;

import java.net.SocketAddress;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.SessionConfig;
import org.apache.mina.common.TransportType;
import org.apache.mina.io.IoSession;
import org.apache.mina.protocol.ProtocolDecoder;
import org.apache.mina.protocol.ProtocolDecoderOutput;
import org.apache.mina.protocol.ProtocolEncoder;
import org.apache.mina.protocol.ProtocolEncoderOutput;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.io.IoAdapter.SessionHandlerAdapter;
import org.apache.mina.util.Queue;

/**
 * A {@link ProtocolSession} that is backed by {@link IoSession}.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class IoProtocolSession implements ProtocolSession
{
    private final IoAdapter ioAdapter;

    final IoSession session;

    final SessionHandlerAdapter shAdapter;

    final Queue writeQueue = new Queue();
    
    final ProtocolEncoder encoder;
    
    final ProtocolDecoder decoder;

    final ProtocolEncoderOutputImpl encOut;

    final ProtocolDecoderOutputImpl decOut;

    IoProtocolSession( IoAdapter ioAdapter, IoSession session,
                       SessionHandlerAdapter shAdapter )
    {
        this.ioAdapter = ioAdapter;
        this.session = session;
        this.shAdapter = shAdapter;
        this.encoder = shAdapter.codecFactory.newEncoder();
        this.decoder = shAdapter.codecFactory.newDecoder();
        this.encOut = new ProtocolEncoderOutputImpl();
        this.decOut = new ProtocolDecoderOutputImpl();
    }
    
    /**
     * Returns the {@link IoSession} this session is backed by.
     */
    public IoSession getIoSession()
    {
        return session;   
    }

    public ProtocolHandler getHandler()
    {
        return shAdapter.handler;
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
        this.ioAdapter.filters.filterWrite( this, message );
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

    static class ProtocolEncoderOutputImpl implements ProtocolEncoderOutput
    {
    
        final Queue queue = new Queue();
        
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
        
    static class ProtocolDecoderOutputImpl implements ProtocolDecoderOutput
    {
        final Queue messageQueue = new Queue();
        
        private ProtocolDecoderOutputImpl()
        {
        }
        
        public void write( Object message )
        {
            messageQueue.push( message );
        }
    }
}