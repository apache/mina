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
 * TODO Document me.
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

                            if( !session.bothIdle )
                            {
                                idleTime = config
                                        .getIdleTimeInMillis( IdleStatus.BOTH_IDLE );
                                session.bothIdle = idleTime > 0L
                                                   && ( currentTime - session.lastReadTime ) > idleTime;
                                if( session.bothIdle )
                                    session.localFilterManager
                                            .fireSessionIdle(
                                                              session,
                                                              IdleStatus.BOTH_IDLE );
                            }

                            if( !session.readerIdle )
                            {
                                idleTime = config
                                        .getIdleTimeInMillis( IdleStatus.READER_IDLE );
                                session.readerIdle = idleTime > 0L
                                                     && ( currentTime - session.lastReadTime ) > idleTime;
                                if( session.readerIdle )
                                    session.localFilterManager
                                            .fireSessionIdle(
                                                              session,
                                                              IdleStatus.READER_IDLE );
                            }

                            if( !session.writerIdle )
                            {
                                idleTime = config
                                        .getIdleTimeInMillis( IdleStatus.WRITER_IDLE );
                                session.writerIdle = idleTime > 0L
                                                     && ( currentTime - session.lastReadTime ) > idleTime;
                                if( session.writerIdle )
                                    session.localFilterManager
                                            .fireSessionIdle(
                                                              session,
                                                              IdleStatus.WRITER_IDLE );
                            }
                        }
                    }
                }
            }
        }
    }
}