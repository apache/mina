/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.mina.common;

import org.apache.mina.io.IoSession;
import org.apache.mina.io.WriteTimeoutException;
import org.apache.mina.io.datagram.DatagramSessionConfig;
import org.apache.mina.io.socket.SocketSessionConfig;
import org.apache.mina.protocol.ProtocolSession;

/**
 * Provides general or {@link TransportType}-specific configuration.
 * <p>
 * <ul>
 *   <li><code>idleTime</code> (secs) - <code>sessionIdle</code> event is
 *       enabled if this value is greater than 0.
 *   <li><code>writeTimeout</code> (secs) - {@link WriteTimeoutException} is
 *       thrown when the write buffer of session is full for the specified
 *       time.</li>
 * </ul>
 * <p>
 * Please refer to {@link SocketSessionConfig} and {@link DatagramSessionConfig}
 * for {@link TransportType}-specific configurations.
 * <p>
 * {@link SessionConfig} can be obtained by {@link IoSession#getConfig()} and
 * by {@link ProtocolSession#getConfig()}.  To adjust
 * {@link TransportType}-specific settings, please downcast it:
 * <pre>
 * public void sessionOpened( IoSession s )
 * {
 *     ( ( SocketSessionConfig ) s.getConfig() ).setReuseAddress( true );
 * }
 * </pre>
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public interface SessionConfig
{
    /**
     * Returns idle time for the specified type of idleness in seconds.
     */
    int getIdleTime( IdleStatus status );

    /**
     * Returnd idle time for the specified type of idleness in milliseconds.
     */
    long getIdleTimeInMillis( IdleStatus status );

    /**
     * Sets idle time for the specified type of idleness in seconds.
     */
    void setIdleTime( IdleStatus status, int idleTime );

    /**
     * Returns write timeout in seconds.
     */
    int getWriteTimeout();

    /**
     * Returns write timeout in milliseconds.
     */
    long getWriteTimeoutInMillis();

    /**
     * Sets write timeout in seconds.
     */
    void setWriteTimeout( int writeTimeout );
}