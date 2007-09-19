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
package org.apache.mina.transport.apr;

import org.apache.mina.common.IoSession;

/**
 * Default configuration for {@link APRSession}
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
class DefaultAPRSessionConfig extends AbstractAPRSessionConfig implements
        APRSessionConfig {

    private static boolean SET_RECEIVE_BUFFER_SIZE_AVAILABLE = false;

    private static boolean SET_SEND_BUFFER_SIZE_AVAILABLE = false;

    private static boolean GET_TRAFFIC_CLASS_AVAILABLE = false;

    private static boolean SET_TRAFFIC_CLASS_AVAILABLE = false;

    private static boolean DEFAULT_REUSE_ADDRESS = false;

    private static int DEFAULT_RECEIVE_BUFFER_SIZE = 1024;

    private static int DEFAULT_SEND_BUFFER_SIZE = 1024;

    private static int DEFAULT_TRAFFIC_CLASS = 0;

    private static boolean DEFAULT_KEEP_ALIVE = false;

    private static boolean DEFAULT_OOB_INLINE = false;

    private static int DEFAULT_SO_LINGER = -1;

    private static boolean DEFAULT_TCP_NO_DELAY = false;

    private boolean reuseAddress = DEFAULT_REUSE_ADDRESS;

    private int receiveBufferSize = DEFAULT_RECEIVE_BUFFER_SIZE;

    private int sendBufferSize = DEFAULT_SEND_BUFFER_SIZE;

    private int trafficClass = DEFAULT_TRAFFIC_CLASS;

    private boolean keepAlive = DEFAULT_KEEP_ALIVE;

    private boolean oobInline = DEFAULT_OOB_INLINE;

    private int soLinger = DEFAULT_SO_LINGER;

    private boolean tcpNoDelay = DEFAULT_TCP_NO_DELAY;

    /**
     * Creates a new instance.
     */
    DefaultAPRSessionConfig() {
    }

    public boolean isReuseAddress() {
        return reuseAddress;
    }

    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    public int getSendBufferSize() {
        return sendBufferSize;
    }

    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    public int getTrafficClass() {
        return trafficClass;
    }

    public void setTrafficClass(int trafficClass) {
        this.trafficClass = trafficClass;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public boolean isOobInline() {
        return oobInline;
    }

    public void setOobInline(boolean oobInline) {
        this.oobInline = oobInline;
    }

    public int getSoLinger() {
        return soLinger;
    }

    public void setSoLinger(int soLinger) {
        this.soLinger = soLinger;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }
}
