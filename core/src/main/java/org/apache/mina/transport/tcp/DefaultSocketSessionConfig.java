/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mina.transport.tcp;

import java.net.Socket;
import java.net.SocketException;

import org.apache.mina.api.IdleStatus;

/**
 * Implementation for the socket session configuration.
 * 
 * Will hold the values for the service in change of configuring this session (before the session opening).
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultSocketSessionConfig implements SocketSessionConfig {

    /**
     * Create a socket configuration with the default value of the given {@link Socket}.
     * Will help to create
     * 
     * @param socket
     * @throws SocketException 
     */
    public DefaultSocketSessionConfig(Socket socket) throws SocketException {
        this.receiveBufferSize = socket.getReceiveBufferSize();
        this.sendBufferSize = socket.getSendBufferSize();
        this.tcpNoDelay = socket.getTcpNoDelay();
        this.reuseAddress = socket.getReuseAddress();
        this.trafficClass = socket.getTrafficClass();
        this.keepAlive = socket.getKeepAlive();
        this.oobInline = socket.getOOBInline();
        this.soLinger = socket.getSoLinger();
        this.readBufferSize = 1024; // FIXME : dumb default value
    }

    //=====================
    // idle management
    //=====================    

    private long idleTimeRead = -1;

    private long idleTimeWrite = -1;

    private long idleTimeReadWrite = -1;

    @Override
    public long getIdleTimeInMillis(IdleStatus status) {
        switch (status) {
        case READ_IDLE:
            return idleTimeRead;

        case WRITE_IDLE:
            return idleTimeWrite;
        case READ_WRITE_IDLE:
            return idleTimeReadWrite;
        default:
            throw new RuntimeException("unexpected excetion, unknown idle status : " + status);
        }
    }

    @Override
    public void setIdleTimeInMillis(IdleStatus status, long ildeTimeInMilli) {
        switch (status) {
        case READ_IDLE:
            this.idleTimeRead = ildeTimeInMilli;
            break;
        case WRITE_IDLE:
            this.idleTimeWrite = ildeTimeInMilli;
            break;
        case READ_WRITE_IDLE:
            this.idleTimeReadWrite = ildeTimeInMilli;
            break;
        default:
            throw new RuntimeException("unexpected excetion, unknown idle status : " + status);
        }
    }

    //=====================
    // buffers
    //=====================

    private int readBufferSize;

    @Override
    public int getReadBufferSize() {
        return readBufferSize;
    }

    @Override
    public void setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
    }

    private int receiveBufferSize;

    @Override
    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    @Override
    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    private int sendBufferSize;

    @Override
    public int getSendBufferSize() {
        return sendBufferSize;
    }

    @Override
    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    //=====================
    // socket options
    //=====================

    private boolean tcpNoDelay;

    @Override
    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    @Override
    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    private boolean reuseAddress;

    @Override
    public boolean isReuseAddress() {
        return reuseAddress;
    }

    @Override
    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    private int trafficClass;

    @Override
    public int getTrafficClass() {
        return trafficClass;
    }

    @Override
    public void setTrafficClass(int trafficClass) {
        this.trafficClass = trafficClass;
    }

    private boolean keepAlive;

    @Override
    public boolean isKeepAlive() {
        return keepAlive;
    }

    @Override
    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    private boolean oobInline;

    @Override
    public boolean isOobInline() {
        return oobInline;
    }

    @Override
    public void setOobInline(boolean oobInline) {
        this.oobInline = oobInline;

    }

    private int soLinger;

    @Override
    public int getSoLinger() {
        return soLinger;
    }

    @Override
    public void setSoLinger(int soLinger) {
        this.soLinger = soLinger;
    }
}
