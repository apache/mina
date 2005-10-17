/*
 * @(#) $Id$
 */
package org.apache.mina.protocol.handler;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolHandlerAdapter;
import org.apache.mina.protocol.ProtocolSession;

/**
 * A {@link ProtocolHandler} that demuxes <code>messageReceived</code> events
 * to the appropriate {@link MessageHandler}.
 * 
 * You can freely register and deregister {@link MessageHandler}s using
 * {@link #addMessageHandler(Class, MessageHandler)} and
 * {@link #removeMessageHandler(Class)}.
 * 
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class DemuxingProtocolHandler extends ProtocolHandlerAdapter
{
    private final Map type2handler = new HashMap();

    /**
     * Creates a new instance with no registered {@link MessageHandler}s.
     */
    protected DemuxingProtocolHandler()
    {
    }

    /**
     * Registers a {@link MessageHandler} that receives the messages of
     * the specified <code>type</code>.
     */
    public void addMessageHandler( Class type, MessageHandler handler )
    {
        synchronized( type2handler )
        {
            type2handler.put( type, handler );
        }
    }

    /**
     * Deregisters a {@link MessageHandler} that receives the messages of
     * the specified <code>type</code>.
     */
    public void removeMessageHandler( Class type )
    {
        synchronized( type2handler )
        {
            type2handler.remove( type );
        }
    }
    
    /**
     * Returns the {@link MessageHandler} which is registered to process
     * the specified <code>type</code>. 
     */
    public MessageHandler getMessageHandler( Class type )
    {
        return ( MessageHandler ) type2handler.get( type );
    }
    
    /**
     * Returns the {@link Map} which contains all messageType-{@link MessageHandler}
     * pairs registered to this handler.
     */
    public Map getMessageHandlerMap()
    {
        return Collections.unmodifiableMap( type2handler );
    }

    /**
     * Forwards the received events into the appropriate {@link MessageHandler}
     * which is registered by {@link #addMessageHandler(Class, MessageHandler)}.
     */
    public void messageReceived( ProtocolSession session, Object message )
    {
        MessageHandler handler = findHandler( message.getClass() );
        if( handler != null )
        {
            handler.messageReceived( session, message );
        }
        else
        {
            throw new UnknownMessageTypeException(
                    "No message handler found for message: " + message );
        }
    }

    private MessageHandler findHandler( Class type )
    {
        MessageHandler handler = ( MessageHandler ) type2handler.get( type );
        if( handler == null )
        {
            handler = findHandler( type, new HashSet() );
        }

        return handler;
    }

    private MessageHandler findHandler( Class type, Set triedClasses )
    {
        MessageHandler handler;

        if( triedClasses.contains( type ) )
            return null;
        triedClasses.add( type );

        handler = ( MessageHandler ) type2handler.get( type );
        if( handler == null )
        {
            handler = findHandler( type, triedClasses );
            if( handler != null )
                return handler;

            Class[] interfaces = type.getInterfaces();
            for( int i = 0; i < interfaces.length; i ++ )
            {
                handler = findHandler( interfaces[ i ], triedClasses );
                if( handler != null )
                    return handler;
            }

            return null;
        }
        else
            return handler;
    }
}
