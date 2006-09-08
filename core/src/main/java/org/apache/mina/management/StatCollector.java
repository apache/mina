/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.mina.management;


import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoServiceListener;
import org.apache.mina.common.IoSession;


/**
 * Collects statistics of an {@link IoService}. It's polling all the sessions of a given
 * IoService. It's attaching a {@link IoSessionStat} object to all the sessions polled
 * and filling the throughput values.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class StatCollector
{
    /**
     * The session attribute key for {@link IoSessionStat}.
     */
    public static final String KEY = StatCollector.class.getName() + ".stat";

    private final IoService service;
    private Worker worker;
    private int pollingInterval = 5000;
    private List polledSessions;

    private final IoServiceListener serviceListener = new IoServiceListener()
    {
        public void serviceActivated( IoService service, SocketAddress serviceAddress, IoHandler handler,
            IoServiceConfig config )
        {
        }

        public void serviceDeactivated( IoService service, SocketAddress serviceAddress, IoHandler handler,
            IoServiceConfig config )
        {
        }

        public void sessionCreated( IoSession session )
        {
            addSession( session );
        }

        public void sessionDestroyed( IoSession session )
        {
            removeSession( session );
        }
    };

    /**
     * create a stat collector for the given given service with the default polling time 
     * @param service the IoService to inspect
     */
    public StatCollector( IoService service )
    {
        this( service,5000 );

    }

    /**
     * create a stat collector for the given given service
     * @param service the IoService to inspect
     * @param pollingInterval milliseconds
     */
    public StatCollector( IoService service, int pollingInterval )
    {
        this.service = service;
        this.pollingInterval = pollingInterval;
    }

    /**
     * start collecting stats
     */
    public void start()
    {
        // TODO Make this method thread-safe.
        if ( worker != null && worker.isAlive() )
            throw new RuntimeException( "Stat collecting already started" );

        // add all current sessions

        polledSessions = new ArrayList();

        for ( Iterator iter = service.getManagedServiceAddresses().iterator(); iter.hasNext(); )
        {
            SocketAddress element = ( SocketAddress ) iter.next();

            for ( Iterator iter2 = service.getManagedSessions( element ).iterator(); iter2.hasNext(); )
            {
                addSession( ( IoSession ) iter2.next() );

            }
        }

        // listen for new ones
        service.addListener( serviceListener );

        // start polling
        worker = new Worker();
        worker.start();
    }

    /**
     * stop collecting stats
     */
    public void stop()
    {
        // TODO Make this method thread-safe.
        // TODO Make the worker stop immediately, not waiting for the next loop.
        service.removeListener( serviceListener );

        // stop worker
        worker.stop = true;
        try
        {
            synchronized ( worker )
            {
                worker.join();
            }
        }
        catch ( InterruptedException e )
        {
            // ignore
        }

        polledSessions.clear();

    }

    /**
     * is the stat collector collecting
     * @return true if started
     */
    public boolean isRunning()
    {
        // TODO Make this method thread-safe
        return worker != null && worker.stop != true;
    }

    private void addSession( IoSession session )
    {
        polledSessions.add( session );
        session.setAttribute( KEY, new IoSessionStat() );
    }

    private void removeSession( IoSession session )
    {
        polledSessions.remove( session );
        session.removeAttribute( KEY );
    }

    private class Worker extends Thread
    {

        boolean stop = false;

        private Worker()
        {
            // TODO Append a thread ID or any distingushed information to the thread name.
            super( "StatCollectorWorker" );
        }

        public void run()
        {
            while ( !stop )
            {
                for ( Iterator iter = polledSessions.iterator(); iter.hasNext(); )
                {
                    IoSession session = ( IoSession ) iter.next();
                    IoSessionStat sessStat = ( IoSessionStat ) session.getAttribute( KEY );

                    sessStat.lastByteRead = session.getReadBytes();
                    sessStat.lastByteWrite = session.getWrittenBytes();
                    sessStat.lastMessageRead = session.getReadMessages();
                    sessStat.lastMessageWrite = session.getWrittenMessages();
                }

                // wait polling time
                try
                {
                    Thread.sleep( pollingInterval );
                }
                catch ( InterruptedException e )
                {
                }

                for ( Iterator iter = polledSessions.iterator(); iter.hasNext(); )
                {
                    IoSession session = ( IoSession ) iter.next();
                    IoSessionStat sessStat = ( IoSessionStat ) session.getAttribute( KEY );

                    sessStat.byteReadThroughput = ( session.getReadBytes() - sessStat.lastByteRead )
                        / ( pollingInterval / 1000f );
                    sessStat.byteWrittenThroughput = ( session.getWrittenBytes() - sessStat.lastByteWrite )
                        / ( pollingInterval / 1000f );

                    sessStat.messageReadThroughput = ( session.getReadMessages() - sessStat.lastMessageRead )
                        / ( pollingInterval / 1000f );
                    sessStat.messageWrittenThroughput = ( session.getWrittenMessages() - sessStat.lastMessageWrite )
                        / ( pollingInterval / 1000f );

                }
            }
        }
    }

}
