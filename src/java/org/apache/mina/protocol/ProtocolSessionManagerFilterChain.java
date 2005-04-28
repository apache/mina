package org.apache.mina.protocol;

import org.apache.mina.common.IdleStatus;

/**
 * TODO document me
 * 
 * @author The Apache Directory Project
 */
public abstract class ProtocolSessionManagerFilterChain extends AbstractProtocolHandlerFilterChain {

    private final ProtocolSessionManager manager;

    protected ProtocolSessionManagerFilterChain( ProtocolSessionManager manager )
    {
        this.manager = manager;
    }
    
    public ProtocolSessionManager getManager()
    {
        return manager;
    }
    
    protected ProtocolHandlerFilter createTailFilter()
    {
        return new ProtocolHandlerFilter()
        {
            public void sessionOpened( NextFilter nextFilter, ProtocolSession session )
            {
                ( ( ProtocolSessionFilterChain ) session.getFilterChain() ).sessionOpened( session );
            }

            public void sessionClosed( NextFilter nextFilter, ProtocolSession session )
            {
                ( ( ProtocolSessionFilterChain ) session.getFilterChain() ).sessionClosed( session );
            }

            public void sessionIdle( NextFilter nextFilter, ProtocolSession session,
                                    IdleStatus status )
            {
                ( ( ProtocolSessionFilterChain ) session.getFilterChain() ).sessionIdle( session, status );
            }

            public void exceptionCaught( NextFilter nextFilter,
                                        ProtocolSession session, Throwable cause )
            {
                ( ( ProtocolSessionFilterChain ) session.getFilterChain() ).exceptionCaught( session, cause );
            }

            public void messageReceived( NextFilter nextFilter, ProtocolSession session,
                                         Object message )
            {
                ( ( ProtocolSessionFilterChain ) session.getFilterChain() ).messageReceived( session, message );
            }

            public void messageSent( NextFilter nextFilter, ProtocolSession session,
                                     Object message )
            {
                ( ( ProtocolSessionFilterChain ) session.getFilterChain() ).messageSent( session, message );
            }

            public void filterWrite( NextFilter nextFilter,
                                     ProtocolSession session, Object message )
            {
                nextFilter.filterWrite( session, message );
            }
        };
    }
}
