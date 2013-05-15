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

import javax.net.ssl.SSLContext;

import org.apache.mina.api.ConfigurationException;
import org.apache.mina.api.IdleStatus;
import org.apache.mina.session.TrafficClassEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class proxy the inner java.net.Socket configuration with the SocketSessionConfig of the session.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ProxyTcpSessionConfig implements TcpSessionConfig {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyTcpSessionConfig.class);

    private final Socket socket;

    public ProxyTcpSessionConfig(Socket socket) {
        this.socket = socket;
    }

    private long idleTimeRead = -1;

    private long idleTimeWrite = -1;

    /**
     * {@inheritDoc}
     */
    @Override
    public long getIdleTimeInMillis(IdleStatus status) {
        switch (status) {
        case READ_IDLE:
            return idleTimeRead;
        case WRITE_IDLE:
            return idleTimeWrite;
        default:
            throw new IllegalStateException("unexpected excetion, unknown idle status : " + status);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIdleTimeInMillis(IdleStatus status, long ildeTimeInMilli) {
        switch (status) {
        case READ_IDLE:
            this.idleTimeRead = ildeTimeInMilli;
            break;
        case WRITE_IDLE:
            this.idleTimeWrite = ildeTimeInMilli;
            break;
        default:
            throw new IllegalStateException("unexpected excetion, unknown idle status : " + status);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isTcpNoDelay() {
        try {
            return socket.getTcpNoDelay();
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTcpNoDelay(boolean tcpNoDelay) {
        LOG.debug("set TCP no delay '{}' for session '{}'", tcpNoDelay, this);
        try {
            socket.setTcpNoDelay(tcpNoDelay);
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isReuseAddress() {
        try {
            return socket.getReuseAddress();
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReuseAddress(boolean reuseAddress) {
        LOG.debug("set reuse address '{}' for session '{}'", reuseAddress, this);
        try {
            socket.setReuseAddress(reuseAddress);
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getReadBufferSize() {
        try {
            return socket.getReceiveBufferSize();
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReadBufferSize(int receiveBufferSize) {
        LOG.debug("set receive buffer size '{}' for session '{}'", receiveBufferSize, this);
        try {
            socket.setReceiveBufferSize(receiveBufferSize);
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getSendBufferSize() {
        try {
            return socket.getSendBufferSize();
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSendBufferSize(int sendBufferSize) {
        LOG.debug("set send buffer size '{}' for session '{}'", sendBufferSize, this);
        try {
            socket.setSendBufferSize(sendBufferSize);
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTrafficClass() {
        try {
            return socket.getTrafficClass();
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTrafficClass(int trafficClass) {
        LOG.debug("set traffic class '{}' for session '{}'", trafficClass, this);
        try {
            socket.setTrafficClass(trafficClass);
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTrafficClass(TrafficClassEnum trafficClass) {
        LOG.debug("set traffic class '{}' for session '{}'", trafficClass, this);
        try {
            socket.setTrafficClass(trafficClass.getValue());
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isKeepAlive() {
        try {
            return socket.getKeepAlive();
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setKeepAlive(boolean keepAlive) {
        LOG.debug("set keep alive '{}' for session '{}'", keepAlive, this);
        try {
            socket.setKeepAlive(keepAlive);
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isOobInline() {
        try {
            return socket.getOOBInline();
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOobInline(boolean oobInline) {
        LOG.debug("set oob inline '{}' for session '{}'", oobInline, this);
        try {
            socket.setOOBInline(oobInline);
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getSoLinger() {
        try {
            return socket.getSoLinger();
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSoLinger(int soLinger) {
        LOG.debug("set so linger '{}' for session '{}'", soLinger, this);
        try {
            socket.setSoLinger(soLinger > 0, soLinger);
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecured() {
        return false;
    }

    @Override
    public SSLContext getSslContext() {
        return null;
    }

    @Override
    public void setSslContext(SSLContext sslContext) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getTimeout() {
        try {
            return socket.getSoTimeout();
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimeout(int timeout) {
        try {
            socket.setSoTimeout(timeout);
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }
}
