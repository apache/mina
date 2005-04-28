/*
 * @(#) $Id$
 */
package org.apache.mina.protocol.vmpipe;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.SessionConfig;

/**
 * Dectects idle sessions and fires <tt>sessionIdle</tt> events to them. 
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
class VmPipeIdleStatusChecker
{
    static final VmPipeIdleStatusChecker INSTANCE = new VmPipeIdleStatusChecker();

    private final Map sessions = new IdentityHashMap(); // will use as a set

    private final Worker worker = new Worker();

    private VmPipeIdleStatusChecker()
    {
        worker.start();
    }

    void addSession( VmPipeSession session )
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
                        VmPipeSession session = ( VmPipeSession ) it.next();
                        if( !session.isConnected() )
                        {
                            it.remove();
                        }
                        else
                        {
                            long idleTime;
                            SessionConfig config = session.getConfig();

                            if( !session.isIdle( IdleStatus.BOTH_IDLE ) )
                            {
                                idleTime = config
                                        .getIdleTimeInMillis( IdleStatus.BOTH_IDLE );
                                session.setIdle( IdleStatus.BOTH_IDLE,
                                                 idleTime > 0L
                                                 && ( currentTime - session.getLastIoTime() ) > idleTime );
                                if( session.isIdle( IdleStatus.BOTH_IDLE ) )
                                    session.getManagerFilterChain()
                                            .sessionIdle( session,
                                                          IdleStatus.BOTH_IDLE );
                            }

                            if( !session.isIdle( IdleStatus.READER_IDLE ) )
                            {
                                idleTime = config
                                        .getIdleTimeInMillis( IdleStatus.READER_IDLE );
                                session.setIdle( IdleStatus.READER_IDLE,
                                                 idleTime > 0L
                                                 && ( currentTime - session.getLastReadTime() ) > idleTime );
                                if( session.isIdle( IdleStatus.READER_IDLE ) )
                                    session.getManagerFilterChain()
                                            .sessionIdle( session,
                                                          IdleStatus.READER_IDLE );
                            }

                            if( !session.isIdle( IdleStatus.WRITER_IDLE ) )
                            {
                                idleTime = config
                                        .getIdleTimeInMillis( IdleStatus.WRITER_IDLE );
                                session.setIdle( IdleStatus.WRITER_IDLE,
                                                 idleTime > 0L
                                                 && ( currentTime - session.getLastWriteTime() ) > idleTime );
                                if( session.isIdle( IdleStatus.WRITER_IDLE ) )
                                    session.getManagerFilterChain()
                                            .sessionIdle( session,
                                                          IdleStatus.WRITER_IDLE );
                            }
                        }
                    }
                }
            }
        }
    }
}