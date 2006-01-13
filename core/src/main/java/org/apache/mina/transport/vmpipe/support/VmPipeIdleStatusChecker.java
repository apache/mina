/*
 * @(#) $Id$
 */
package org.apache.mina.transport.vmpipe.support;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.support.AbstractIoFilterChain;

/**
 * Dectects idle sessions and fires <tt>sessionIdle</tt> events to them. 
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class VmPipeIdleStatusChecker
{
    private static final VmPipeIdleStatusChecker INSTANCE = new VmPipeIdleStatusChecker();
    
    public static VmPipeIdleStatusChecker getInstance()
    {
        return INSTANCE;
    }

    private final Map sessions = new IdentityHashMap(); // will use as a set

    private final Worker worker = new Worker();

    private VmPipeIdleStatusChecker()
    {
        worker.start();
    }

    public void addSession( VmPipeSessionImpl session )
    {
        synchronized( sessions )
        {
            sessions.put( session, session );
        }
    }

    private class Worker extends Thread
    {
        private Worker()
        {
            super( "VmPipeIdleStatusChecker" );
            setDaemon( true );
        }

        public void run()
        {
            for( ;; )
            {
                try
                {
                    Thread.sleep( 1000 );
                }
                catch( InterruptedException e )
                {
                }

                long currentTime = System.currentTimeMillis();

                synchronized( sessions )
                {
                    Iterator it = sessions.keySet().iterator();
                    while( it.hasNext() )
                    {
                        VmPipeSessionImpl session = ( VmPipeSessionImpl ) it.next();
                        if( !session.isConnected() )
                        {
                            it.remove();
                        }
                        else
                        {
                            notifyIdleSession( session, currentTime );
                        }
                    }
                }
            }
        }
    }
    
    private void notifyIdleSession( VmPipeSessionImpl session, long currentTime )
    {
        notifyIdleSession0(
                session, currentTime,
                session.getIdleTimeInMillis( IdleStatus.BOTH_IDLE ),
                IdleStatus.BOTH_IDLE,
                Math.max( session.getLastIoTime(), session.getLastIdleTime( IdleStatus.BOTH_IDLE ) ) );
        notifyIdleSession0(
                session, currentTime,
                session.getIdleTimeInMillis( IdleStatus.READER_IDLE ),
                IdleStatus.READER_IDLE,
                Math.max( session.getLastReadTime(), session.getLastIdleTime( IdleStatus.READER_IDLE ) ) );
        notifyIdleSession0(
                session, currentTime,
                session.getIdleTimeInMillis( IdleStatus.WRITER_IDLE ),
                IdleStatus.WRITER_IDLE,
                Math.max( session.getLastWriteTime(), session.getLastIdleTime( IdleStatus.WRITER_IDLE ) ) );
    }

    private void notifyIdleSession0( VmPipeSessionImpl session, long currentTime,
                                    long idleTime, IdleStatus status,
                                    long lastIoTime )
    {
        if( idleTime > 0 && lastIoTime != 0
            && ( currentTime - lastIoTime ) >= idleTime )
        {
            session.increaseIdleCount( status );
            ( ( AbstractIoFilterChain ) session.getFilterChain() ).sessionIdle( session, status );
        }
    }

}