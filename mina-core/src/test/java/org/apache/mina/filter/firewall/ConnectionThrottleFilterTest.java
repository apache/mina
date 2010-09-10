/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.mina.filter.firewall;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;

import org.apache.mina.core.session.DummySession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * TODO Add documentation
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ConnectionThrottleFilterTest
{
    private ConnectionThrottleFilter filter;

    private DummySession sessionOne;
    private DummySession sessionTwo;

    @Before
    public void setUp() throws Exception
    {
        filter = new ConnectionThrottleFilter();

        sessionOne = new DummySession();
        sessionOne.setRemoteAddress( new InetSocketAddress(1234) );
        sessionTwo = new DummySession();
        sessionTwo.setRemoteAddress( new InetSocketAddress(1235) );
    }

    @After
    public void tearDown() throws Exception
    {
        filter = null;
    }

    @Test
    public void testGoodConnection(){
        filter.setAllowedInterval( 100 );
        filter.isConnectionOk( sessionOne );
        
        try
        {
            Thread.sleep( 1000 );
        }
        catch ( InterruptedException e )
        {
            //e.printStackTrace();
        }

        boolean result = filter.isConnectionOk( sessionOne );
        assertTrue( result );
    }

    @Test
    public void testBadConnection(){
        filter.setAllowedInterval( 1000 );
        filter.isConnectionOk( sessionTwo );
        assertFalse(filter.isConnectionOk( sessionTwo ));
    }
}
