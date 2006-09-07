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

import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoServiceListener;
import org.apache.mina.common.IoSession;
import org.apache.mina.management.IoSessionStat;
import org.apache.mina.management.StatCollector;


/**
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 */
public class IoServiceManager implements IoServiceManagerMBean
{
    private IoService service;

    private StatCollector collector = null;


    public IoServiceManager( IoService service )
    {
        this.service = service;
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


    public void startCollectingStats( int millisecondsPolling )
    {
        if ( collector != null && collector.isRunning() )
        {
            throw new RuntimeException( "Already collecting stats" );
        }

        collector = new StatCollector( service, millisecondsPolling );
        collector.start();

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
                total += ( ( IoSessionStat ) session.getAttribute( StatCollector.STAT_ID ) )
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
                total += ( ( IoSessionStat ) session.getAttribute( StatCollector.STAT_ID ) )
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
                total += ( ( IoSessionStat ) session.getAttribute( StatCollector.STAT_ID ) )
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
                total += ( ( IoSessionStat ) session.getAttribute( StatCollector.STAT_ID ) )
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
                total += ( ( IoSessionStat ) session.getAttribute( StatCollector.STAT_ID ) )
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
                total += ( ( IoSessionStat ) session.getAttribute( StatCollector.STAT_ID ) )
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
                total += ( ( IoSessionStat ) session.getAttribute( StatCollector.STAT_ID ) )
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
                total += ( ( IoSessionStat ) session.getAttribute( StatCollector.STAT_ID ) )
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
}
