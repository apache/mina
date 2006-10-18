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
package org.apache.mina.integration.jmx;


import java.net.SocketAddress;
import java.util.Iterator;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.management.IoSessionStat;
import org.apache.mina.management.StatCollector;


/**
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IoServiceManager implements IoServiceManagerMBean, MBeanRegistration
{
    private IoService service;

    private StatCollector collector = null;

    private int milliSecondsPolling; 

    private boolean autoStartCollecting = false;
    
    public IoServiceManager( IoService service , int milliSecondsPolling, boolean autoStartCollecting)
    {
    	this.autoStartCollecting = autoStartCollecting;
        this.service = service;
        this.milliSecondsPolling = milliSecondsPolling;
    }

    public IoServiceManager( IoService service, int milliSecondsPolling ) 
    {
    	this( service, milliSecondsPolling, false );
    }

    public IoServiceManager( IoService service ) 
    {
    	this( service, 5000, false );
    }


    public int getManagedSessionCount()
    {

        int count = 0;
        for ( Iterator iter = service.getManagedServiceAddresses().iterator(); iter.hasNext(); )
        {
            SocketAddress element = ( SocketAddress ) iter.next();

            count += service.getManagedSessions( element ).size();
        }
        return count;
    }


    public void startCollectingStats()
    {
        if ( collector != null && collector.isRunning() )
        {
            throw new RuntimeException( "Already collecting stats" );
        }

        collector = new StatCollector( service, milliSecondsPolling );
        collector.start();
    }
    
    public int getStatsPollingInterval()
    {
    	return milliSecondsPolling;
    }
    
    public void setStatsPollingInterval( int millisecondsPolling ) 
    {
    	this.milliSecondsPolling = millisecondsPolling;
    }

    public void stopCollectingStats()
    {
        if ( collector != null && collector.isRunning() )
            collector.stop();

    }


    public float getTotalByteReadThroughput()
    {
        float total = 0;
        for ( Iterator iter = service.getManagedServiceAddresses().iterator(); iter.hasNext(); )
        {
            SocketAddress element = ( SocketAddress ) iter.next();

            for ( Iterator iter2 = service.getManagedSessions( element ).iterator(); iter2.hasNext(); )
            {
                IoSession session = ( IoSession ) iter2.next();
                total += ( ( IoSessionStat ) session.getAttribute( StatCollector.KEY ) )
                    .getByteReadThroughput();
            }
        }
        return total;
    }


    public float getTotalByteWrittenThroughput()
    {
        float total = 0;
        for ( Iterator iter = service.getManagedServiceAddresses().iterator(); iter.hasNext(); )
        {
            SocketAddress element = ( SocketAddress ) iter.next();

            for ( Iterator iter2 = service.getManagedSessions( element ).iterator(); iter2.hasNext(); )
            {
                IoSession session = ( IoSession ) iter2.next();
                total += ( ( IoSessionStat ) session.getAttribute( StatCollector.KEY ) )
                    .getByteWrittenThroughput();
            }
        }
        return total;
    }


    public float getTotalMessageReadThroughput()
    {
        float total = 0;
        for ( Iterator iter = service.getManagedServiceAddresses().iterator(); iter.hasNext(); )
        {
            SocketAddress element = ( SocketAddress ) iter.next();

            for ( Iterator iter2 = service.getManagedSessions( element ).iterator(); iter2.hasNext(); )
            {
                IoSession session = ( IoSession ) iter2.next();
                total += ( ( IoSessionStat ) session.getAttribute( StatCollector.KEY ) )
                    .getMessageReadThroughput();
            }
        }
        return total;
    }


    public float getTotalMessageWrittenThroughput()
    {
        float total = 0;
        for ( Iterator iter = service.getManagedServiceAddresses().iterator(); iter.hasNext(); )
        {
            SocketAddress element = ( SocketAddress ) iter.next();

            for ( Iterator iter2 = service.getManagedSessions( element ).iterator(); iter2.hasNext(); )
            {
                IoSession session = ( IoSession ) iter2.next();
                total += ( ( IoSessionStat ) session.getAttribute( StatCollector.KEY ) )
                    .getMessageWrittenThroughput();
            }
        }
        return total;
    }


    public float getAverageByteReadThroughput()
    {
        float total = 0;
        int count = 0;
        for ( Iterator iter = service.getManagedServiceAddresses().iterator(); iter.hasNext(); )
        {
            SocketAddress element = ( SocketAddress ) iter.next();

            for ( Iterator iter2 = service.getManagedSessions( element ).iterator(); iter2.hasNext(); )
            {
                IoSession session = ( IoSession ) iter2.next();
                total += ( ( IoSessionStat ) session.getAttribute( StatCollector.KEY ) )
                    .getByteReadThroughput();
                count++;
            }
        }
        return total / count;
    }


    public float getAverageByteWrittenThroughput()
    {
        float total = 0;
        int count = 0;
        for ( Iterator iter = service.getManagedServiceAddresses().iterator(); iter.hasNext(); )
        {
            SocketAddress element = ( SocketAddress ) iter.next();

            for ( Iterator iter2 = service.getManagedSessions( element ).iterator(); iter2.hasNext(); )
            {
                IoSession session = ( IoSession ) iter2.next();
                total += ( ( IoSessionStat ) session.getAttribute( StatCollector.KEY ) )
                    .getByteWrittenThroughput();
                count++;
            }
        }
        return total / count;
    }


    public float getAverageMessageReadThroughput()
    {
        float total = 0;
        int count = 0;
        for ( Iterator iter = service.getManagedServiceAddresses().iterator(); iter.hasNext(); )
        {
            SocketAddress element = ( SocketAddress ) iter.next();

            for ( Iterator iter2 = service.getManagedSessions( element ).iterator(); iter2.hasNext(); )
            {
                IoSession session = ( IoSession ) iter2.next();
                total += ( ( IoSessionStat ) session.getAttribute( StatCollector.KEY ) )
                    .getMessageReadThroughput();
                count++;
            }
        }
        return total / count;
    }


    public float getAverageMessageWrittenThroughput()
    {
        float total = 0;
        int count = 0;
        for ( Iterator iter = service.getManagedServiceAddresses().iterator(); iter.hasNext(); )
        {
            SocketAddress element = ( SocketAddress ) iter.next();

            for ( Iterator iter2 = service.getManagedSessions( element ).iterator(); iter2.hasNext(); )
            {
                IoSession session = ( IoSession ) iter2.next();
                total += ( ( IoSessionStat ) session.getAttribute( StatCollector.KEY ) )
                    .getMessageWrittenThroughput();
                count++;
            }
        }
        return total / count;
    }


    public void closeAllSessions()
    {
        for ( Iterator iter = service.getManagedServiceAddresses().iterator(); iter.hasNext(); )
        {
            SocketAddress element = ( SocketAddress ) iter.next();

            for ( Iterator iter2 = service.getManagedSessions( element ).iterator(); iter2.hasNext(); )
            {
                IoSession session = ( IoSession ) iter2.next();
                session.close();
            }
        }

    }

	public ObjectName preRegister( MBeanServer server, ObjectName name ) throws Exception 
	{
		return name;
	}

	public void postRegister( Boolean registrationDone ) 
	{
		if( registrationDone ) 
		{
			if( autoStartCollecting ) 
			{
				startCollectingStats();
			}
				
		}
	}
	
	public void preDeregister() throws Exception 
	{
		if ( collector != null && collector.isRunning() ) 
		{
			stopCollectingStats();
		}
	}

	public void postDeregister() 
	{
	}

}