/*
 * @(#) $Id$
 */
package org.apache.mina.protocol;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A {@link ProtocolHandler} that demuxes <code>messageReceived</code> events
 * to the appropriate {@link MessageHandler}.
 * 
 * You can freely register and deregister {@link MessageHandler}s using
 * {@link #registerMessageType(Class, MessageHandler)} and
 * {@link #deregisterMessageType(Class)}.
 * 
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public abstract class DemuxingProtocolHandler implements ProtocolHandler
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
    protected void registerMessageType( Class type, MessageHandler handler )
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
    protected void deregisterMessageType( Class clazz )
    {
        synchronized( type2handler )
        {
            type2handler.remove( clazz );
        }
    }

    /**
     * Forwards the received events into the appropriate {@link MessageHandler}
     * which is registered by {@link #registerMessageType(Class, MessageHandler)}.
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

    /**
     * A handler interface that {@link DemuxingProtocolHandler} forwards
     * <code>messageReceived</code> events to.  You have to register your
     * handler with the type of message you want to get notified using
     * {@link DemuxingProtocolHandler#registerMessageType(Class, MessageHandler)}.
     * 
     * @author The Apache Directory Project
     * @version $Rev$, $Date$
     */
    public interface MessageHandler
    {
        /**
         * A {@link MessageHandler} that does nothing.  This is usefule when
         * you want to ignore messages of the specific type silently.
         */
        static MessageHandler NOOP = new MessageHandler()
        {
            public void messageReceived( ProtocolSession session, Object message )
            {
            }
        };
        
        /**
         * Invoked when the specific type of message is received from the
         * specified <code>session</code>.
         */
        void messageReceived( ProtocolSession session, Object message );
    }

    /**
     * An exception that is thrown when {@link DemuxingProtocolHandler}
     * cannot find any {@link MessageHandler}s associated with the specific
     * message type.  You have to use
     * {@link DemuxingProtocolHandler#registerMessageType(Class, MessageHandler)}
     * to associate a message type and a message handler. 
     * 
     * @author The Apache Directory Project
     * @version $Rev$, $Date$
     */
    public class UnknownMessageTypeException extends RuntimeException
    {
        private static final long serialVersionUID = 3257290227428047158L;

        public UnknownMessageTypeException()
        {
        }

        public UnknownMessageTypeException( String message, Throwable cause )
        {
            super( message, cause );
        }

        public UnknownMessageTypeException( String message )
        {
            super( message );
        }

        public UnknownMessageTypeException( Throwable cause )
        {
            super( cause );
        }
    }
}
