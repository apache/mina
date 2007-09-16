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
package org.apache.mina.common;

/**
 * An internal interface to represent an 'I/O processor' that performs
 * actual I/O operations for {@link IoSession}s.  It abstracts existing
 * reactor frameworks such as Java NIO once again to simplify transport
 * implementations.
 *  
 * @author Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoProcessor {
    /**
     * Adds the specified {@code session} to the I/O processor so that
     * the I/O processor starts to perform any I/O operations related
     * with the {@code session}.
     */
    void add(IoSession session);

    /**
     * Flushes the internal write request queue of the specified
     * {@code session}.
     * 
     * @param writeRequest the write request added right now
     */
    void flush(IoSession session, WriteRequest writeRequest);

    /**
     * Controls the traffic of the specified {@code session} as specified
     * in {@link IoSession#getTrafficMask()}.
     */
    void updateTrafficMask(IoSession session);
    
    /**
     * Removes and closes the specified {@code session} from the I/O
     * processor so that the I/O processor closes the connection
     * associated with the {@code session} and releases any other related
     * resources.
     */
    void remove(IoSession session);
}
