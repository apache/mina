/**
 * 
 */
package org.apache.mina.protocol.io;

import java.net.SocketAddress;
import java.util.Set;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.SessionConfig;
import org.apache.mina.common.TransportType;
import org.apache.mina.io.IoSession;
import org.apache.mina.protocol.ProtocolDecoder;
import org.apache.mina.protocol.ProtocolEncoder;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolFilterChain;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.ProtocolSessionFilterChain;
import org.apache.mina.protocol.ProtocolSessionManagerFilterChain;
import org.apache.mina.protocol.SimpleProtocolDecoderOutput;
import org.apache.mina.protocol.SimpleProtocolEncoderOutput;
import org.apache.mina.protocol.io.IoAdapter.SessionHandlerAdapter;
import org.apache.mina.util.Queue;

/**
 * A {@link ProtocolSession} that is backed by {@link IoSession}.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IoProtocolSession implements ProtocolSession
{
    private final ProtocolSessionFilterChain filterChain;

    final IoSession session;

    final SessionHandlerAdapter shAdapter;

    final Queue writeQueue = new Queue();
    
    final ProtocolEncoder encoder;
    
    final ProtocolDecoder decoder;

    final SimpleProtocolEncoderOutput encOut;

    final SimpleProtocolDecoderOutput decOut;

    IoProtocolSession( ProtocolSessionManagerFilterChain managerFilterChain,
                       IoSession session, SessionHandlerAdapter shAdapter )
    {
        this.filterChain = new ProtocolSessionFilterChain( managerFilterChain );
        this.session = session;
        this.shAdapter = shAdapter;
        this.encoder = shAdapter.codecFactory.newEncoder();
        this.decoder = shAdapter.codecFactory.newDecoder();
        this.encOut = new SimpleProtocolEncoderOutput();
        this.decOut = new SimpleProtocolDecoderOutput();
    }
    
    /**
     * Returns the {@link IoSession} this session is backed by.
     */
    public IoSession getIoSession()
    {
        return session;   
    }
    
    public ProtocolFilterChain getFilterChain()
    {
        return filterChain;
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
        filterChain.filterWrite( this, message );
    }

    public long getWrittenWriteRequests()
    {
        return session.getWrittenWriteRequests();
    }

    public int getScheduledWriteRequests()
    {
        return session.getScheduledWriteRequests();
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

    public long getCreationTime()
    {
        return session.getCreationTime();
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

    public int getIdleCount( IdleStatus status )
    {
        return session.getIdleCount( status );
    }

    public long getLastIdleTime( IdleStatus status )
    {
        return session.getLastIdleTime( status );
    }
}