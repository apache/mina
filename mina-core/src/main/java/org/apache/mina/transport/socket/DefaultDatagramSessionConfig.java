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

import java.net.DatagramSocket;

/**
 * A default implementation of {@link DatagramSessionConfig}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultDatagramSessionConfig extends AbstractDatagramSessionConfig {
    private static boolean DEFAULT_BROADCAST = false;
    private static boolean DEFAULT_REUSE_ADDRESS = false;

    /* The SO_RCVBUF parameter. Set to -1 (ie, will default to OS default) */
    private static int DEFAULT_RECEIVE_BUFFER_SIZE = -1;

    /* The SO_SNDBUF parameter. Set to -1 (ie, will default to OS default) */
    private static int DEFAULT_SEND_BUFFER_SIZE = -1;

    private static int DEFAULT_TRAFFIC_CLASS = 0;

    private boolean broadcast = DEFAULT_BROADCAST;
    private boolean reuseAddress = DEFAULT_REUSE_ADDRESS;
    private int receiveBufferSize = DEFAULT_RECEIVE_BUFFER_SIZE;
    private int sendBufferSize = DEFAULT_SEND_BUFFER_SIZE;
    private int trafficClass = DEFAULT_TRAFFIC_CLASS;
    
    /**
     * Creates a new instance.
     */
    public DefaultDatagramSessionConfig() {
        // Do nothing
    }

    /**
     * @see DatagramSocket#getBroadcast()
     */
    public boolean isBroadcast() {
        return broadcast;
    }

    /**
     * @see DatagramSocket#setBroadcast(boolean)
     */
    public void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }

    /**
     * @see DatagramSocket#getReuseAddress()
     */
    public boolean isReuseAddress() {
        return reuseAddress;
    }

    /**
     * @see DatagramSocket#setReuseAddress(boolean)
     */
    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    /**
     * @see DatagramSocket#getReceiveBufferSize()
     */
    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    /**
     * @see DatagramSocket#setReceiveBufferSize(int)
     */
    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    /**
     * @see DatagramSocket#getSendBufferSize()
     */
    public int getSendBufferSize() {
        return sendBufferSize;
    }

    /**
     * @see DatagramSocket#setSendBufferSize(int)
     */
    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    /**
     * @see DatagramSocket#getTrafficClass()
     */
    public int getTrafficClass() {
        return trafficClass;
    }

    /**
     * @see DatagramSocket#setTrafficClass(int)
     */
    public void setTrafficClass(int trafficClass) {
        this.trafficClass = trafficClass;
    }

    @Override
    protected boolean isBroadcastChanged() {
        return broadcast != DEFAULT_BROADCAST;
    }

    @Override
    protected boolean isReceiveBufferSizeChanged() {
        return receiveBufferSize != DEFAULT_RECEIVE_BUFFER_SIZE;
    }

    @Override
    protected boolean isReuseAddressChanged() {
        return reuseAddress != DEFAULT_REUSE_ADDRESS;
    }

    @Override
    protected boolean isSendBufferSizeChanged() {
        return sendBufferSize != DEFAULT_SEND_BUFFER_SIZE;
    }

    @Override
    protected boolean isTrafficClassChanged() {
        return trafficClass != DEFAULT_TRAFFIC_CLASS;
    }
    
}