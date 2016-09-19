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

import org.apache.mina.core.service.IoService;

/**
 * A default implementation of {@link SocketSessionConfig}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultSocketSessionConfig extends AbstractSocketSessionConfig {
    private static final boolean DEFAULT_REUSE_ADDRESS = false;

    private static final int DEFAULT_TRAFFIC_CLASS = 0;

    private static final boolean DEFAULT_KEEP_ALIVE = false;

    private static final boolean DEFAULT_OOB_INLINE = false;

    private static final int DEFAULT_SO_LINGER = -1;

    private static final boolean DEFAULT_TCP_NO_DELAY = false;

    protected IoService parent;

    private boolean defaultReuseAddress;

    private boolean reuseAddress;

    /* The SO_RCVBUF parameter. Set to -1 (ie, will default to OS default) */
    private int receiveBufferSize = -1;

    /* The SO_SNDBUF parameter. Set to -1 (ie, will default to OS default) */
    private int sendBufferSize = -1;

    private int trafficClass = DEFAULT_TRAFFIC_CLASS;

    private boolean keepAlive = DEFAULT_KEEP_ALIVE;

    private boolean oobInline = DEFAULT_OOB_INLINE;

    private int soLinger = DEFAULT_SO_LINGER;

    private boolean tcpNoDelay = DEFAULT_TCP_NO_DELAY;

    /**
     * Creates a new instance.
     */
    public DefaultSocketSessionConfig() {
        // Do nothing
    }

    /**
     * Initialize this configuration.
     * 
     * @param parent The parent IoService.
     */
    public void init(IoService parent) {
        this.parent = parent;

        if (parent instanceof SocketAcceptor) {
            defaultReuseAddress = true;
        } else {
            defaultReuseAddress = DEFAULT_REUSE_ADDRESS;
        }

        reuseAddress = defaultReuseAddress;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReuseAddress() {
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
    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSendBufferSize() {
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
        return trafficClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTrafficClass(int trafficClass) {
        this.trafficClass = trafficClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isKeepAlive() {
        return keepAlive;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOobInline() {
        return oobInline;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOobInline(boolean oobInline) {
        this.oobInline = oobInline;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSoLinger() {
        return soLinger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSoLinger(int soLinger) {
        this.soLinger = soLinger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isKeepAliveChanged() {
        return keepAlive != DEFAULT_KEEP_ALIVE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isOobInlineChanged() {
        return oobInline != DEFAULT_OOB_INLINE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isReceiveBufferSizeChanged() {
        return receiveBufferSize != -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isReuseAddressChanged() {
        return reuseAddress != defaultReuseAddress;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isSendBufferSizeChanged() {
        return sendBufferSize != -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isSoLingerChanged() {
        return soLinger != DEFAULT_SO_LINGER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isTcpNoDelayChanged() {
        return tcpNoDelay != DEFAULT_TCP_NO_DELAY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isTrafficClassChanged() {
        return trafficClass != DEFAULT_TRAFFIC_CLASS;
    }
}
