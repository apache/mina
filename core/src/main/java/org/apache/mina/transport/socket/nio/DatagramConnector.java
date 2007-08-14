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
package org.apache.mina.transport.socket.nio;

import java.util.concurrent.Executor;

import org.apache.mina.common.IoConnectorWrapper;
import org.apache.mina.common.IoConnector;
import org.apache.mina.transport.socket.nio.support.DatagramConnectorDelegate;
import org.apache.mina.util.NewThreadExecutor;

/**
 * {@link IoConnector} for datagram transport (UDP/IP).
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DatagramConnector extends IoConnectorWrapper {
    /**
     * Creates a new instance using a NewThreadExecutor 
     */
    public DatagramConnector() {
        init(new DatagramConnectorDelegate(this, new NewThreadExecutor()));
    }

    /**
     * Creates a new instance.
     * 
     * @param executor Executor to use for launching threads
     */
    public DatagramConnector(Executor executor) {
        init(new DatagramConnectorDelegate(this, executor));
    }

    @Override
    public DatagramSessionConfig getSessionConfig() {
        return ((DatagramConnectorDelegate) connector).getSessionConfig();
    }
}
