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
package org.apache.mina.api;

import java.net.DatagramSocket;
import java.net.Socket;

import org.apache.mina.session.TrafficClassEnum;

/**
 * The configuration of {@link IoSession}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoSessionConfig {

    /**
     * Returns the size of the read buffer that I/O processor allocates
     * per each read.  It's unusual to adjust this property because
     * it's often adjusted automatically by the I/O processor.
     * 
     * @return The buffer size, or null if its not set
     */
    Integer getReadBufferSize();

    /**
     * Sets the size of the read buffer that I/O processor allocates
     * per each read.  It's unusual to adjust this property because
     * it's often adjusted automatically by the I/O processor.
     * 
     * @param readBufferSize The buffer size used to read data from the socket
     */
    void setReadBufferSize(int readBufferSize);

    /**
     * @see DatagramSocket#getSendBufferSize()
     */
    Integer getSendBufferSize();

    /**
     * Sets the size of the buffer that I/O processor allocates
     * per each write.  It's unusual to adjust this property because
     * it's often adjusted automatically by the I/O processor.
     * 
     * @param sendBufferSize The buffer size used to send data into the socket
     */
    void setSendBufferSize(int sendBufferSize);

    /**
     * Returns idle time for the specified type of idleness in milli-seconds.
     * @see IdleStatus
     * @return the idle time in ms or <code>-1</code> if no idle time configured for this status
     */
    long getIdleTimeInMillis(IdleStatus status);

    /**
     * Set the delay before an {@link IoSession} is considered idle for a given
     * operation type (read/write/both) @see IdleStatus
     *
     * @param status          the type of idle (read/write/both) timeout to set
     * @param ildeTimeInMilli the timeout in milliseconds (<code>-1</code> for no idle detection on this status)
     */
    void setIdleTimeInMillis(IdleStatus status, long ildeTimeInMilli);

    /**
     * @see Socket#getTrafficClass()
     * @return The ToS set for this session
     */
    int getTrafficClass();

    /**
     * Set the ToS flag for this session
     * @see Socket#setTrafficClass(int)
     * @param trafficClass The ToS to set
     */
    void setTrafficClass(TrafficClassEnum trafficClass);

    /**
     * Set the ToS flag for this session
     * @see Socket#setTrafficClass(int)
     * @param trafficClass The ToS to set
     */
    void setTrafficClass(int trafficClass);

    /**
     * @see Socket#getReuseAddress()
     */
    Boolean isReuseAddress();

    /**
     * @see Socket#setReuseAddress(boolean)
     * @see DatagramSocket#setReuseAddress(boolean)
     * return <code>null</code> if the default system value is used 
     */
    void setReuseAddress(boolean reuseAddress);

    /**
     * Get the SO_TIMEOUT set for this socket
     * @see Socket#getSoTimeout()
     * @see DatagramSocket#getSoTimeout()
     * 
     * @return The Timeout, in milliseconds. 0 means infinite.
     */
    Integer getTimeout();

    /**
     * Sets the SO_TIMEOUT option for this socket
     * @see Socket#setSoTimeout(int)
     * @see DatagramSocket#setSoTimeout(int)
     * @param timeOut The timeout to set, in milliseconds. 0 means infinite
     */
    void setTimeout(int timeOut);
}
