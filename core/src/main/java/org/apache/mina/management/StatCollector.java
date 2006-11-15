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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceListener;
import org.apache.mina.common.IoSession;

/**
 * Collects statistics of an {@link IoService}. It's polling all the sessions of a given
 * IoService. It's attaching a {@link IoSessionStat} object to all the sessions polled
 * and filling the throughput values.
 * 
 * Usage :
 * <pre>
 * IoService service = ...
 * StatCollector collector = new StatCollector( service );
 * collector.start();
 * </pre>
 * 
 * By default the {@link StatCollector} is polling the sessions every 5 seconds. You can 
 * give a different polling time using a second constructor.
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

    
    /**
     * @noinspection StaticNonFinalField
     */
    private static volatile int nextId = 0;
    private final int id = nextId ++;
    
    private final IoService service;
    private Worker worker;
    private int pollingInterval = 5000;
    private List polledSessions;

    // resume of session stats, for simplifying acces to the statistics 
    private long totalProcessedSessions = 0L;
    private float msgWrittenThroughput = 0f;
    private float msgReadThroughput = 0f;
    private float bytesWrittenThroughput = 0f;
    private float bytesReadThroughput = 0f;    
    
    private final IoServiceListener serviceListener = new IoServiceListener()
    {
        public void serviceActivated( IoService service )
        {
        }

        public void serviceDeactivated( IoService service )
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
     * Create a stat collector for the given service with a default polling time of 5 seconds. 
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
     * Start collecting stats for the {@link IoSession} of the service.
     * New sessions or destroyed will be automaticly added or removed.
     */
    public void start()
    {
        synchronized (this) 
        {
            if ( worker != null && worker.isAlive() )
                throw new RuntimeException( "Stat collecting already started" );
    
            // add all current sessions
    
            polledSessions = new ArrayList();
    
            for ( Iterator iter = service.getManagedSessions().iterator(); iter.hasNext(); )
            {
                addSession( ( IoSession ) iter.next() );
            }

            // listen for new ones
            service.addListener( serviceListener );
    
            // start polling
            worker = new Worker();
            worker.start();

        }

    }

    /**
     * Stop collecting stats. all the {@link IoSessionStat} object will be removed of the
     * polled session attachements. 
     */
    public void stop()
    {
        synchronized (this) 
        {
            service.removeListener( serviceListener );

            // stop worker
            worker.stop = true;
            worker.interrupt();
            while( worker.isAlive() )
            {
                try
                {
                    worker.join();
                }
                catch( InterruptedException e )
                {
                    //ignore since this is shutdown time
                }
            }

            for (Iterator iter = polledSessions.iterator(); iter.hasNext();) {
                IoSession session = (IoSession) iter.next();
                session.removeAttribute(KEY);
            }
            polledSessions.clear();
        }
    }

    /**
     * is the stat collector started and polling the {@link IoSession} of the {@link IoService}
     * @return true if started
     */
    public boolean isRunning()
    {
        synchronized (this) 
        {
            return worker != null && worker.stop != true;
        }
    }

    private void addSession( IoSession session )
    {
    	synchronized (this) 
    	{
        	totalProcessedSessions += 1;
        	polledSessions.add( session );
            session.setAttribute( KEY, new IoSessionStat() );
		}
    }

    private void removeSession( IoSession session )
    {
    	synchronized (this)
    	{

    		// remove the session from the list of polled sessions
    		polledSessions.remove( session );

    		// add the bytes processed between last polling and session closing
    		// prevent non seen byte with non-connected protocols like HTTP and datagrams
    		IoSessionStat sessStat = ( IoSessionStat ) session.getAttribute( KEY );
    		
    		// computing with time between polling and closing
        	bytesReadThroughput += ( (float) (session.getReadBytes() - sessStat.lastByteRead) ) /  ( ( System.currentTimeMillis() - sessStat.lastPollingTime ) /1000f ) ;
        	bytesWrittenThroughput += ( (float) (session.getWrittenBytes() - sessStat.lastByteWrite) ) /  ( ( System.currentTimeMillis() - sessStat.lastPollingTime ) /1000f ) ;
        	msgReadThroughput += ( (float) (session.getReadMessages() - sessStat.lastMessageRead) ) /  ( ( System.currentTimeMillis() - sessStat.lastPollingTime ) /1000f ) ;
        	msgWrittenThroughput += ( (float) (session.getWrittenMessages() - sessStat.lastMessageWrite) ) /  ( ( System.currentTimeMillis() - sessStat.lastPollingTime ) /1000f ) ;
        	        	        	
            session.removeAttribute( KEY );
			
		}
    }


    /**
     * total number of sessions processed by the stat collector
     * @return number of sessions
     */
    public long getTotalProcessedSessions()
    {
    	return totalProcessedSessions;
    }
    
	public float getBytesReadThroughput()
	{
		return bytesReadThroughput;
	}

	public float getBytesWrittenThroughput()
	{
		return bytesWrittenThroughput;
	}

	public float getMsgReadThroughput()
	{
		return msgReadThroughput;
	}

	public float getMsgWrittenThroughput() 
	{
		return msgWrittenThroughput;
	}
	
	public long getSessionCount() 
	{
		return polledSessions.size();
	}
	
    private class Worker extends Thread
    {

        boolean stop = false;

        private Worker()
        {
            super( "StatCollectorWorker-"+id );
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
                
                
                msgWrittenThroughput = 0f;
                msgReadThroughput = 0f;
                bytesWrittenThroughput = 0f;
                bytesReadThroughput = 0f;
                
                for ( Iterator iter = polledSessions.iterator(); iter.hasNext(); )
                {
                	
                	// upadating individual session statistics
                    IoSession session = ( IoSession ) iter.next();
                    IoSessionStat sessStat = ( IoSessionStat ) session.getAttribute( KEY );

                    sessStat.byteReadThroughput = ( session.getReadBytes() - sessStat.lastByteRead )
                        / ( pollingInterval / 1000f );
                    bytesReadThroughput += sessStat.byteReadThroughput;
                    
                    sessStat.byteWrittenThroughput = ( session.getWrittenBytes() - sessStat.lastByteWrite )
                        / ( pollingInterval / 1000f );
                    bytesWrittenThroughput += sessStat.byteWrittenThroughput;

                    sessStat.messageReadThroughput = ( session.getReadMessages() - sessStat.lastMessageRead )
                        / ( pollingInterval / 1000f );
                    msgReadThroughput += sessStat.messageReadThroughput;
                    
                    sessStat.messageWrittenThroughput = ( session.getWrittenMessages() - sessStat.lastMessageWrite )
                        / ( pollingInterval / 1000f );
                    msgWrittenThroughput += sessStat.messageWrittenThroughput;
                    
                    sessStat.lastPollingTime = System.currentTimeMillis();
                }
            }
        }
    }
}