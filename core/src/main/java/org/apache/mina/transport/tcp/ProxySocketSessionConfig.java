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

import org.apache.mina.api.ConfigurationException;
import org.apache.mina.api.IdleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class proxy the inner java.net.Socket configuration with the SocketSessionConfig of the session.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ProxySocketSessionConfig implements SocketSessionConfig {

    private static final Logger LOG = LoggerFactory.getLogger(ProxySocketSessionConfig.class);

    private final Socket socket;

    public ProxySocketSessionConfig(Socket socket) {
        this.socket = socket;
    }

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

    @Override
    public Boolean isTcpNoDelay() {
        try {
            return socket.getTcpNoDelay();
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public void setTcpNoDelay(boolean tcpNoDelay) {
        try {
            socket.setTcpNoDelay(tcpNoDelay);
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public Boolean isReuseAddress() {
        try {
            return socket.getReuseAddress();
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public void setReuseAddress(boolean reuseAddress) {
        try {
            socket.setReuseAddress(reuseAddress);
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public Integer getReceiveBufferSize() {
        try {
            return socket.getReceiveBufferSize();
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public void setReceiveBufferSize(int receiveBufferSize) {
        try {
            socket.setReceiveBufferSize(receiveBufferSize);
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public Integer getSendBufferSize() {
        try {
            return socket.getSendBufferSize();
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public void setSendBufferSize(int sendBufferSize) {
        try {
            socket.setSendBufferSize(sendBufferSize);
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public Integer getTrafficClass() {
        try {
            return socket.getTrafficClass();
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public void setTrafficClass(int trafficClass) {
        try {
            socket.setTrafficClass(trafficClass);
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public Boolean isKeepAlive() {
        try {
            return socket.getKeepAlive();
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public void setKeepAlive(boolean keepAlive) {
        try {
            socket.setKeepAlive(keepAlive);
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public Boolean isOobInline() {
        try {
            return socket.getOOBInline();
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public void setOobInline(boolean oobInline) {
        try {
            socket.setOOBInline(oobInline);
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public Integer getSoLinger() {
        try {
            return socket.getSoLinger();
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public void setSoLinger(int soLinger) {
        try {
            socket.setSoLinger(soLinger > 0, soLinger);
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

}
