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

import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoSessionRecycler;

/**
 * An {@link IoServiceConfig} for {@link DatagramAcceptor} and {@link DatagramConnector}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface DatagramServiceConfig extends IoServiceConfig {
    DatagramSessionConfig getSessionConfig();

    /**
     * Returns the {@link IoSessionRecycler} for this service.
     */
    IoSessionRecycler getSessionRecycler();

    /**
     * Sets the {@link IoSessionRecycler} for this service.
     * 
     * @param sessionRecycler <tt>null</tt> to use the default recycler
     */
    void setSessionRecycler(IoSessionRecycler sessionRecycler);
}
