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


import java.util.Iterator;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.management.StatCollector;


/**
 * @author The Apache MINA Project (dev@mina.apache.org)
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
        return service.getManagedSessions().size();
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
    	return collector.getBytesReadThroughput();
    }


    public float getTotalByteWrittenThroughput()
    {
    	return collector.getBytesWrittenThroughput();
    }


    public float getTotalMessageReadThroughput()
    {
    	return collector.getMsgReadThroughput();
    }


    public float getTotalMessageWrittenThroughput()
    {
    	return collector.getMsgWrittenThroughput();
    }


    public float getAverageByteReadThroughput()
    {
        return collector.getBytesReadThroughput() / collector.getSessionCount();
    }


    public float getAverageByteWrittenThroughput()
    {
        return collector.getBytesWrittenThroughput() / collector.getSessionCount();    
    }


    public float getAverageMessageReadThroughput()
    {
        return collector.getMsgReadThroughput() / collector.getSessionCount();
    }


    public float getAverageMessageWrittenThroughput()
    {
        return collector.getMsgWrittenThroughput() / collector.getSessionCount();    
    }
    

    public void closeAllSessions()
    {
        for ( Iterator iter = service.getManagedSessions().iterator(); iter.hasNext(); )
        {
            IoSession session = ( IoSession ) iter.next();
            session.close();
        }
    }

    public ObjectName preRegister( MBeanServer server, ObjectName name ) throws Exception 
    {
        return name;
    }

    public void postRegister( Boolean registrationDone )
    {
        if( registrationDone.booleanValue() )
        {
            if( autoStartCollecting )
            {
                startCollectingStats();
            }

        }
    }

    public void preDeregister() throws Exception
    {
        if( collector != null && collector.isRunning() )
        {
            stopCollectingStats();
        }
    }

    public void postDeregister()
    {
    }
}