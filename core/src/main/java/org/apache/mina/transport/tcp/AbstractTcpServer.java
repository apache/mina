/*
 *  Licensed to the Apache Software Foundation () under one
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
package org.apache.mina.transport.tcp;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import org.apache.mina.api.IoSession;
import org.apache.mina.service.server.AbstractIoServer;

/**
 * Base class for TCP based Servers
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractTcpServer extends AbstractIoServer {
    /** The SSLContext instance */
    private SSLContext sslContext;

    /**
     * Create an new AbsractTcpServer instance
     */
    protected AbstractTcpServer() {
        super();
    }

    /**
     * Set the reuse address flag on the server socket
     * @param reuseAddress <code>true</code> to enable
     */
    public abstract void setReuseAddress(boolean reuseAddress);

    /**
     * Is the reuse address enabled for this server.
     * @return
     */
    public abstract boolean isReuseAddress();

    /**
     * Inject a {@link SSLContex} valid for the service. This {@link SSLContex} will be used
     * by the SSLEngine to handle secured connections.<br/>
     * The {@link SSLContex} must have been created and initialized before being injected in
     * the service.<br/>
     * By setting a {@link SSLContext}, the service switch to secured.
     * @param sslContext The configured {@link SSLContex}.
     */
    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        mode = ServiceMode.SECURED;
    }

    /**
     * @return The {@link SSLContext} instance stored in the service.
     */
    public SSLContext getSslContext() {
        return sslContext;
    }

    /**
     * {@inheritDoc}
     */
    public void initSecured(IoSession session) throws SSLException {
        if (mode == ServiceMode.SECURED) {
            session.initSecure(sslContext);
        }
    }
}