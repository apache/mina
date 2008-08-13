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
package org.apache.mina.proxy.handlers.socks;

import org.apache.mina.proxy.AbstractProxyLogicHandler;
import org.apache.mina.proxy.session.ProxyIoSession;

/**
 * AbstractSocksLogicHandler.java - Base class for SOCKS {@link AbstractProxyLogicHandler} implementations.
 * 
 * @author Edouard De Oliveira <a href="mailto:doe_wanted@yahoo.fr">doe_wanted@yahoo.fr</a>
 * @version $Id: $
 */
public abstract class AbstractSocksLogicHandler extends
        AbstractProxyLogicHandler {

    /**
     * The request we will ask to the proxy.
     */
    protected final SocksProxyRequest request;

    /**
     * Creates a new {@link AbstractSocksLogicHandler}.
     */
    public AbstractSocksLogicHandler(final ProxyIoSession proxyIoSession) {
        super(proxyIoSession);
        this.request = (SocksProxyRequest) proxyIoSession.getRequest();
    }
}