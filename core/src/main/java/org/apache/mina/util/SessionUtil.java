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
package org.apache.mina.util;

import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.transport.socket.nio.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;

/**
 * A session utility
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SessionUtil
{
    public static void initialize( IoSession session )
    {
        IoSessionConfig cfg = session.getConfig();
        if( cfg instanceof SocketSessionConfig )
        {
            SocketSessionConfig sCfg = ( SocketSessionConfig ) cfg;
            sCfg.setReuseAddress( true );
            sCfg.setKeepAlive( true );
        }
        else if( cfg instanceof DatagramSessionConfig )
        {
            DatagramSessionConfig dCfg = ( DatagramSessionConfig ) cfg;
            dCfg.setReuseAddress( true );
        }
    }

    private SessionUtil()
    {
    }
}
