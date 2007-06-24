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
package org.apache.mina.filter;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.SessionLog;

/**
 * A {@link IoFilter} which blocks connections from connecting
 * at a rate faster than the specified interval.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class ConnectionThrottleFilter extends IoFilterAdapter {
    private static final long DEFAULT_TIME = 1000;  
    private long allowedInterval;
    private final Map<String,Long> clients;

    /**
     * Default constructor.  Sets the wait time to 1 second
     */
    public ConnectionThrottleFilter() {
        this( DEFAULT_TIME );
    }

    /**
     * Constructor that takes in a specified wait time.
     * 
     * @param allowedInterval
     * 	The number of milliseconds a client is allowed to wait
     * 	before making another successful connection
     * 
     */
    public ConnectionThrottleFilter( long allowedInterval ){
        this.allowedInterval = allowedInterval;
        clients = new ConcurrentHashMap<String,Long>();
    }

    /**
     * Sets the interval between connections from a client. 
     * This value is measured in milliseconds.
     * 
     * @param allowedInterval
     * 	The number of milliseconds a client is allowed to wait
     * 	before making another successful connection
     */
    public void setAllowedInterval(long allowedInterval) {
        this.allowedInterval = allowedInterval;
    }

    /**
     * Method responsible for deciding if a connection is OK
     * to continue
     * 
     * @param session
     * 	The new session that will be verified
     * @return
     * 	True if the session meets the criteria, otherwise false
     */
    protected boolean isConnectionOk( IoSession session ){
        SocketAddress remoteAddress = session.getRemoteAddress();
        if( remoteAddress instanceof InetSocketAddress )
        {
            InetSocketAddress addr = (InetSocketAddress)remoteAddress;
            long now = System.currentTimeMillis();
            
            if( clients.containsKey(addr.getAddress().getHostAddress())){
                Long lastConnTime = clients.get(addr.getAddress().getHostAddress());
                clients.put(addr.getAddress().getHostAddress(), now);

                // if the interval between now and the last connection is 
                // less than the allowed interval, return false
                if( (now-lastConnTime) < allowedInterval ){
                    return false;
                } else {
                	return true;
                }
            } else {
                clients.put( addr.getAddress().getHostAddress(), now );
                return true;
            }
        }
       
        return false;
    }
   
    @Override
    public void sessionCreated(NextFilter nextFilter, IoSession session) throws Exception {
        if( ! isConnectionOk(session)){
             SessionLog.info( session, "Connections coming in too fast; closing." );
             session.close();
        }
    }
}