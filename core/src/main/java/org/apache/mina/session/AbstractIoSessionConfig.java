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
/**
 * 
 */
package org.apache.mina.session;

import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoSessionConfig;

/**
 * Base class for session configuration. Implements session configuration properties commons to all the different
 * transports.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractIoSessionConfig implements IoSessionConfig {
    // =====================
    // idle management
    // =====================
    /** The delay we wait for a read before we consider the session is staled */
    private long idleTimeRead = -1;

    /** The delay we wait for a write before we consider the session is staled */
    private long idleTimeWrite = -1;

    /** The SO_RCVBUF socket option. The default buffer size used for Read */
    private Integer readBufferSize = null;

    /** The SO_SNDBUF socket option. The default buffer size used for Write */
    private Integer sendBufferSize = null;

    /** The ToS value */
    private TrafficClassEnum trafficClass = TrafficClassEnum.IPTOS_DEFAULT;

    /** The SO_REUSEADDR socket option */
    private Boolean reuseAddress = null;

    /** The SO_TIMEOUT socket option */
    private Integer timeout = null;

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
    public Integer getReadBufferSize() {
        return readBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReadBufferSize(int readBufferSize) {
        if (readBufferSize <= 0) {
            throw new IllegalArgumentException("readBufferSize: " + readBufferSize + " (expected: 1+)");
        }
        this.readBufferSize = readBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getSendBufferSize() {
        return sendBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTrafficClass() {
        return trafficClass.getValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTrafficClass(TrafficClassEnum trafficClass) {
        this.trafficClass = trafficClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTrafficClass(int trafficClass) {
        this.trafficClass = TrafficClassEnum.valueOf(trafficClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isReuseAddress() {
        return reuseAddress;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getTimeout() {
        return timeout;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
