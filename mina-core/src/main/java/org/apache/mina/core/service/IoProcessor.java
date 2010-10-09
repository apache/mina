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
package org.apache.mina.core.service;

import org.apache.mina.core.session.IoSession;

/**
 * An internal interface to represent an 'I/O processor' that performs
 * actual I/O operations for {@link IoSession}s.  It abstracts existing
 * reactor frameworks such as Java NIO once again to simplify transport
 * implementations.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 * @param <S> the type of the {@link IoSession} this processor can handle
 */
public interface IoProcessor<S extends IoSession> {

    /**
     * Returns <tt>true</tt> if and if only {@link #dispose()} method has
     * been called.  Please note that this method will return <tt>true</tt>
     * even after all the related resources are released.
     */
    boolean isDisposing();
    
    /**
     * Returns <tt>true</tt> if and if only all resources of this processor
     * have been disposed.
     */
    boolean isDisposed();
    
    /**
     * Releases any resources allocated by this processor.  Please note that 
     * the resources might not be released as long as there are any sessions
     * managed by this processor.  Most implementations will close all sessions
     * immediately and release the related resources.
     */
    void dispose();
    
    /**
     * Adds the specified {@code session} to the I/O processor so that
     * the I/O processor starts to perform any I/O operations related
     * with the {@code session}.
     */
    void add(S session);

    /**
     * Flushes the internal write request queue of the specified
     * {@code session}.
     */
    void flush(S session);

    /**
     * Controls the traffic of the specified {@code session} depending of the
     * {@link IoSession#isReadSuspended()} and {@link IoSession#isWriteSuspended()}
     * flags
     */
    void updateTrafficControl(S session);

    /**
     * Removes and closes the specified {@code session} from the I/O
     * processor so that the I/O processor closes the connection
     * associated with the {@code session} and releases any other related
     * resources.
     */
    void remove(S session);
}
