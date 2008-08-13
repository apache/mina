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
package org.apache.mina.proxy.session;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.proxy.ProxyConnector;
import org.apache.mina.proxy.ProxyLogicHandler;
import org.apache.mina.proxy.event.IoSessionEventQueue;
import org.apache.mina.proxy.filter.ProxyFilter;
import org.apache.mina.proxy.handlers.ProxyRequest;
import org.apache.mina.proxy.handlers.http.HttpAuthenticationMethods;

/**
 * ProxyIoSession.java - Class that contains all informations for the current proxy 
 * authentication session.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * @since MINA 2.0.0-M3
 */
public class ProxyIoSession {

    public final static String PROXY_SESSION = ProxyConnector.class.getName()
            + ".ProxySession";

    private final static String DEFAULT_ENCODING = "ISO-8859-1";

    /**
     * The list contains the authentication methods to use. 
     * The order in the list is revelant : if first method is available then it will be used etc ...
     */
    private List<HttpAuthenticationMethods> preferedOrder;

    /**
     * The request to send to the proxy.
     */
    private ProxyRequest request;

    /**
     * The currently selected proxy handler. 
     */
    private ProxyLogicHandler handler;

    /**
     * Parent {@link ProxyFilter} handling the session.
     */
    private ProxyFilter proxyFilter;

    /**
     * The session.
     */
    private IoSession session;

    /**
     * The connector.
     */
    private ProxyConnector connector;

    /**
     * Address of the proxy server.
     */
    private InetSocketAddress proxyAddress = null;

    /**
     * A flag that indicates that proxy closed the connection before handshake is done. So
     * we need to reconnect to the proxy to continue handshaking process.  
     */
    private boolean reconnectionNeeded = false;

    /**
     * Name of the charset used for string encoding & decoding.
     */
    private String charsetName;

    /**
     * The session event queue.
     */
    private IoSessionEventQueue eventQueue;

    /**
     * Set to true when an exception has been thrown.
     */
    private boolean authenticationFailed;

    public IoSessionEventQueue getEventQueue() {
        return eventQueue;
    }

    public ProxyIoSession(InetSocketAddress proxyAddress, ProxyRequest request) {
        setProxyAddress(proxyAddress);
        setRequest(request);
    }

    public List<HttpAuthenticationMethods> getPreferedOrder() {
        return preferedOrder;
    }

    public void setPreferedOrder(List<HttpAuthenticationMethods> preferedOrder) {
        this.preferedOrder = preferedOrder;
    }

    public ProxyLogicHandler getHandler() {
        return handler;
    }

    public void setHandler(ProxyLogicHandler handler) {
        this.handler = handler;
    }

    public ProxyFilter getProxyFilter() {
        return proxyFilter;
    }

    /**
     * Note : Please do not call this method from your code it could result in an
     * unexpected behaviour.
     */
    public void setProxyFilter(ProxyFilter proxyFilter) {
        this.proxyFilter = proxyFilter;
    }

    public ProxyRequest getRequest() {
        return request;
    }

    public void setRequest(ProxyRequest request) {
        if (request == null) {
            throw new NullPointerException("request cannot be null");
        }

        this.request = request;
    }

    public IoSession getSession() {
        return session;
    }

    /**
     * Note : Please do not call this method from your code it could result in an
     * unexpected behaviour.
     */
    public void setSession(IoSession session) {
        this.session = session;
        this.eventQueue = new IoSessionEventQueue(this);
    }

    public ProxyConnector getConnector() {
        return connector;
    }

    /**
     * Note : Please do not call this method from your code it could result in an
     * unexpected behaviour.
     */
    public void setConnector(ProxyConnector connector) {
        this.connector = connector;
    }

    public InetSocketAddress getProxyAddress() {
        return proxyAddress;
    }

    public void setProxyAddress(InetSocketAddress proxyAddress) {
        if (proxyAddress == null) {
            throw new IllegalArgumentException("proxyAddress cannot be null");
        }

        if (!(proxyAddress instanceof InetSocketAddress)) {
            throw new NullPointerException("Unsupported proxyAddress type "
                    + proxyAddress.getClass().getName());
        }

        this.proxyAddress = proxyAddress;
    }

    public boolean isReconnectionNeeded() {
        return reconnectionNeeded;
    }

    /**
     * Note : Please do not call this method from your code it could result in an
     * unexpected behaviour.
     */
    public void setReconnectionNeeded(boolean reconnectionNeeded) {
        this.reconnectionNeeded = reconnectionNeeded;
    }

    public Charset getCharset() {
        return Charset.forName(getCharsetName());
    }

    public synchronized String getCharsetName() {
        if (charsetName == null) {
            charsetName = DEFAULT_ENCODING;
        }

        return charsetName;
    }

    public void setCharsetName(String charsetName) {
        this.charsetName = charsetName;
    }

    public boolean isAuthenticationFailed() {
        return authenticationFailed;
    }

    public void setAuthenticationFailed(boolean authenticationFailed) {
        this.authenticationFailed = authenticationFailed;
    }
}