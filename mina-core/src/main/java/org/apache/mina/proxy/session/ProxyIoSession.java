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
import org.apache.mina.proxy.handlers.http.HttpSmartProxyHandler;

/**
 * ProxyIoSession.java - Class that contains all informations for the current proxy 
 * authentication session.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class ProxyIoSession {

    public final static String PROXY_SESSION = ProxyConnector.class.getName()
            + ".ProxySession";

    private final static String DEFAULT_ENCODING = "ISO-8859-1";

    /**
     * The list contains the authentication methods to use. 
     * The order in the list is revelant : if first method is available 
     * then it will be used etc ...
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
     * The proxy connector.
     */
    private ProxyConnector connector;

    /**
     * Address of the proxy server.
     */
    private InetSocketAddress proxyAddress = null;

    /**
     * A flag that indicates that the proxy closed the connection before handshake 
     * is done. So we need to reconnect to the proxy to continue the handshaking 
     * process.  
     */
    private boolean reconnectionNeeded = false;

    /**
     * Name of the charset used for string encoding & decoding.
     */
    private String charsetName;

    /**
     * The session event queue.
     */
    private IoSessionEventQueue eventQueue = new IoSessionEventQueue(this);

    /**
     * Set to true when an exception has been thrown or if authentication failed.
     */
    private boolean authenticationFailed;

    /**
     * Constructor.
     * 
     * @param proxyAddress the IP address of the proxy server
     * @param request the proxy request
     */
    public ProxyIoSession(InetSocketAddress proxyAddress, ProxyRequest request) {
        setProxyAddress(proxyAddress);
        setRequest(request);
    }

    /**
     * Returns the pending event queue.
     */
    public IoSessionEventQueue getEventQueue() {
        return eventQueue;
    }
    
    /**
     * Returns the list of the prefered order for the authentication methods.
     * This list is used by the {@link HttpSmartProxyHandler} to determine
     * which authentication mechanism to use first between those accepted by the
     * proxy server. This list is only used when connecting to an http proxy.
     */
    public List<HttpAuthenticationMethods> getPreferedOrder() {
        return preferedOrder;
    }

    /**
     * Sets the ordered list of prefered authentication mechanisms.
     * 
     * @param preferedOrder the ordered list
     */
    public void setPreferedOrder(List<HttpAuthenticationMethods> preferedOrder) {
        this.preferedOrder = preferedOrder;
    }

    /**
     * Returns the {@link ProxyLogicHandler} currently in use.
     */
    public ProxyLogicHandler getHandler() {
        return handler;
    }

    /**
     * Sets the {@link ProxyLogicHandler} to use.
     * 
     * @param handler the {@link ProxyLogicHandler} instance
     */
    public void setHandler(ProxyLogicHandler handler) {
        this.handler = handler;
    }

    /**
     * Returns the {@link ProxyFilter}.
     */
    public ProxyFilter getProxyFilter() {
        return proxyFilter;
    }

    /**
     * Sets the {@link ProxyFilter}.
     * Note : Please do not call this method from your code it could result 
     * in an unexpected behaviour.
     * 
     * @param proxyFilter the filter
     */
    public void setProxyFilter(ProxyFilter proxyFilter) {
        this.proxyFilter = proxyFilter;
    }

    /**
     * Returns the proxy request.
     */
    public ProxyRequest getRequest() {
        return request;
    }

    /**
     * Sets the proxy request.
     * 
     * @param request the proxy request
     */
    private void setRequest(ProxyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }

        this.request = request;
    }

    /**
     * Returns the current {@link IoSession}.
     */
    public IoSession getSession() {
        return session;
    }

    /**
     * Sets the {@link IoSession} in use.
     * Note : Please do not call this method from your code it could result in an
     * unexpected behaviour.
     * 
     * @param session the current io session
     */
    public void setSession(IoSession session) {
        this.session = session;
    }

    /**
     * Returns the proxy connector.
     */
    public ProxyConnector getConnector() {
        return connector;
    }

    /**
     * Sets the connector reference of this proxy session.
     * Note : Please do not call this method from your code it could result in an
     * unexpected behaviour.
     * 
     * @param connector the proxy connector
     */
    public void setConnector(ProxyConnector connector) {
        this.connector = connector;
    }

    /**
     * Returns the IP address of the proxy server.
     */
    public InetSocketAddress getProxyAddress() {
        return proxyAddress;
    }

    /**
     * Sets the IP address of the proxy server.
     * 
     * @param proxyAddress the IP address of the proxy server
     */
    private void setProxyAddress(InetSocketAddress proxyAddress) {
        if (proxyAddress == null) {
            throw new IllegalArgumentException("proxyAddress object cannot be null");
        }

        this.proxyAddress = proxyAddress;
    }

    /**
     * Returns true if the current authentication process is not finished
     * but the server has closed the connection.
     */
    public boolean isReconnectionNeeded() {
        return reconnectionNeeded;
    }

    /**
     * Sets the reconnection needed flag. If set to true, it means that an
     * authentication process is currently running but the proxy server did not
     * kept the connection alive. So we need to reconnect to the server to complete
     * the process.
     * Note : Please do not call this method from your code it could result in an
     * unexpected behaviour.
     * 
     * @param reconnectionNeeded the value to set the flag to
     */
    public void setReconnectionNeeded(boolean reconnectionNeeded) {
        this.reconnectionNeeded = reconnectionNeeded;
    }

    /**
     * Returns a charset instance of the in use charset name.
     */
    public Charset getCharset() {
        return Charset.forName(getCharsetName());
    }

    /**
     * Returns the used charset name or {@link #DEFAULT_ENCODING} if null.
     */
    public String getCharsetName() {
        if (charsetName == null) {
            charsetName = DEFAULT_ENCODING;
        }

        return charsetName;
    }

    /**
     * Sets the charset to use.
     * 
     * @param charsetName the charset name
     */
    public void setCharsetName(String charsetName) {
        this.charsetName = charsetName;
    }

    /**
     * Returns true if authentication failed.
     */
    public boolean isAuthenticationFailed() {
        return authenticationFailed;
    }

    /**
     * Sets the authentication failed flag.
     * 
     * @param authenticationFailed the value to set the flag to
     */
    public void setAuthenticationFailed(boolean authenticationFailed) {
        this.authenticationFailed = authenticationFailed;
    }
}