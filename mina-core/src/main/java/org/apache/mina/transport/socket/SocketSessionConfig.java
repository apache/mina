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
package org.apache.mina.transport.socket;

import java.net.Socket;

import org.apache.mina.core.session.IoSessionConfig;

/**
 * An {@link IoSessionConfig} for socket transport type.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface SocketSessionConfig extends IoSessionConfig {
    /**
     * @see Socket#getReuseAddress()
     * 
     * @return <tt>true</tt> if SO_REUSEADDR is enabled.
     */
    boolean isReuseAddress();

    /**
     * @see Socket#setReuseAddress(boolean)
     * 
     * @param reuseAddress Tells if SO_REUSEADDR is enabled or disabled
     */
    void setReuseAddress(boolean reuseAddress);

    /**
     * @see Socket#getReceiveBufferSize()
     * 
     * @return the size of the receive buffer
     */
    int getReceiveBufferSize();

    /**
     * @see Socket#setReceiveBufferSize(int)
     * 
     * @param receiveBufferSize The size of the receive buffer
     */
    void setReceiveBufferSize(int receiveBufferSize);

    /**
     * @see Socket#getSendBufferSize()
     * 
     * @return the size of the send buffer
     */
    int getSendBufferSize();

    /**
     * @see Socket#setSendBufferSize(int)
     * 
     * @param sendBufferSize The size of the send buffer
     */
    void setSendBufferSize(int sendBufferSize);

    /**
     * @see Socket#getTrafficClass()
     * 
     * @return the traffic class
     */
    int getTrafficClass();

    /**
     * @see Socket#setTrafficClass(int)
     * 
     * @param trafficClass The traffic class to set, one of <tt>IPTOS_LOWCOST</tt> (0x02)
     * <tt>IPTOS_RELIABILITY</tt> (0x04), <tt>IPTOS_THROUGHPUT</tt> (0x08) or <tt>IPTOS_LOWDELAY</tt> (0x10)
     */
    void setTrafficClass(int trafficClass);

    /**
     * @see Socket#getKeepAlive()
     * 
     * @return <tt>true</tt> if <tt>SO_KEEPALIVE</tt> is enabled.
     */
    boolean isKeepAlive();

    /**
     * @see Socket#setKeepAlive(boolean)
     * 
     * @param keepAlive if <tt>SO_KEEPALIVE</tt> is to be enabled
     */
    void setKeepAlive(boolean keepAlive);

    /**
     * @see Socket#getOOBInline()
     * 
     * @return <tt>true</tt> if <tt>SO_OOBINLINE</tt> is enabled.
     */
    boolean isOobInline();

    /**
     * @see Socket#setOOBInline(boolean)
     * 
     * @param oobInline if <tt>SO_OOBINLINE</tt> is to be enabled
     */
    void setOobInline(boolean oobInline);

    /**
     * Please note that enabling <tt>SO_LINGER</tt> in Java NIO can result
     * in platform-dependent behavior and unexpected blocking of I/O thread.
     *
     * @see Socket#getSoLinger()
     * @see <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6179351">Sun Bug Database</a>
     * 
     * @return The value for <tt>SO_LINGER</tt>
     */
    int getSoLinger();

    /**
     * Please note that enabling <tt>SO_LINGER</tt> in Java NIO can result
     * in platform-dependent behavior and unexpected blocking of I/O thread.
     *
     * @param soLinger Please specify a negative value to disable <tt>SO_LINGER</tt>.
     *
     * @see Socket#setSoLinger(boolean, int)
     * @see <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6179351">Sun Bug Database</a>
     */
    void setSoLinger(int soLinger);

    /**
     * @see Socket#getTcpNoDelay()
     * 
     * @return <tt>true</tt> if <tt>TCP_NODELAY</tt> is enabled.
     */
    boolean isTcpNoDelay();

    /**
     * @see Socket#setTcpNoDelay(boolean)
     * 
     * @param tcpNoDelay <tt>true</tt> if <tt>TCP_NODELAY</tt> is to be enabled
     */
    void setTcpNoDelay(boolean tcpNoDelay);
}
