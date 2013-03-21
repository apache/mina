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

import javax.net.ssl.SSLContext;

import org.apache.mina.session.AbstractIoSessionConfig;

/**
 * Implementation for the socket session configuration.
 * 
 * Will hold the values for the service in change of configuring this session (before the session opening).
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultTcpSessionConfig extends AbstractIoSessionConfig implements TcpSessionConfig {
    /** The SSLContext instance */
    private SSLContext sslContext;

    //=====================
    // socket options
    //=====================
    /** The TCP_NODELAY socket option */
    private Boolean tcpNoDelay = null;

    /** The OOBINLINE socket option */
    private Boolean oobInline = null;

    /** The SO_KEEPALIVE socket option */
    private Boolean keepAlive = null;

    /** The SO_LINGER socket option */
    private Integer soLinger;

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isTcpNoDelay() {
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
    public Boolean isKeepAlive() {
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
    public Boolean isOobInline() {
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
    public Integer getSoLinger() {
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
     * Inject a {@link SSLContex} valid for the session. This {@link SSLContex} will be used
     * by the SSLEngine to handle secured connections.<br/>
     * The {@link SSLContex} must have been created and initialized before being injected in
     * the configuration.<br/>
     * By setting a {@link SSLContext}, the session switch to secured.
     * @param sslContext The configured {@link SSLContex}.
     */
    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    /**
     * {@inheritDoc}
     */
    public SSLContext getSslContext() {
        return sslContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecured() {
        return sslContext != null;
    }
}
