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
package org.apache.mina.proxy.handlers.http;

import org.apache.mina.proxy.ProxyAuthException;
import org.apache.mina.proxy.handlers.http.basic.HttpBasicAuthLogicHandler;
import org.apache.mina.proxy.handlers.http.basic.HttpNoAuthLogicHandler;
import org.apache.mina.proxy.handlers.http.digest.HttpDigestAuthLogicHandler;
import org.apache.mina.proxy.handlers.http.ntlm.HttpNTLMAuthLogicHandler;
import org.apache.mina.proxy.session.ProxyIoSession;

/**
 * HttpAuthenticationMethods.java - Enumerates all known http authentication methods.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * @since MINA 2.0.0-M3
 */
public enum HttpAuthenticationMethods {

    NO_AUTH(1), BASIC(2), NTLM(3), DIGEST(4);
    
    private final int id;
    
    private HttpAuthenticationMethods(int id) {
    	this.id = id;
    }

    /**
     * Returns the authentication mechanism id.
     * @return the id
     */
    public int getId() {
    	return id;
    }

    /**
     * Creates an {@link AbstractAuthLogicHandler} to handle the authentication mechanism.
     * 
     * @param proxyIoSession the proxy session object
     * @return a new logic handler 
     */
    public AbstractAuthLogicHandler getNewHandler(ProxyIoSession proxyIoSession)
            throws ProxyAuthException {
        switch (this) {
            case BASIC:
                return new HttpBasicAuthLogicHandler(proxyIoSession);
    
            case DIGEST:
                return new HttpDigestAuthLogicHandler(proxyIoSession);
    
            case NTLM:
                return new HttpNTLMAuthLogicHandler(proxyIoSession);
    
            case NO_AUTH:
                return new HttpNoAuthLogicHandler(proxyIoSession);
    
            default:
                return null;
        }
    }
}
