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
package org.apache.mina.transport.socket.nio.support;

/**
 * A base interface for {@link DatagramAcceptorDelegate} and {@link DatagramConnectorDelegate}.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
interface DatagramService {
    /**
     * Requests this processor to flush the write buffer of the specified
     * session.  This method is invoked by MINA internally.
     */
    void flushSession(DatagramSessionImpl session);

    /**
     * Requests this processor to close the specified session.
     * This method is invoked by MINA internally.
     */
    void closeSession(DatagramSessionImpl session);

    /**
     * Requests this processor to update the traffic mask for the specified
     * session. This method is invoked by MINA internally.
     */
    void updateTrafficMask(DatagramSessionImpl session);
}