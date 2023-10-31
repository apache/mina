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
package org.apache.mina.filter.ssl;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.util.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A SSL processor which performs flow control of encrypted information on the
 * filter-chain.
 * <p>
 * The initial handshake is automatically enabled for "client" sessions once the
 * filter is added to the filter-chain and the session is connected.
 *
 * @author Jonathan Valliere
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SslFilter extends IoFilterAdapter {
    /**
     * SSLSession object when the session is secured, otherwise null.
     */
    static public final AttributeKey SSL_SECURED = new AttributeKey(SslFilter.class, "status");

    /**
     * Returns the SSL2Handler object
     */
    static protected final AttributeKey SSL_HANDLER = new AttributeKey(SslFilter.class, "handler");

    /**
     * The logger
     */
    static protected final Logger LOGGER = LoggerFactory.getLogger(SslFilter.class);

    /**
     * Task executor for processing handshakes
     */
    static protected final Executor EXECUTOR = new ThreadPoolExecutor(2, 2, 100, TimeUnit.MILLISECONDS,
            new LinkedBlockingDeque<>(), new BasicThreadFactory("ssl-exec", true));

    protected final SSLContext sslContext;

    /** A flag used to tell the filter to start the handshake immediately (in onPostAdd method)
     *  alternatively handshake will be started after session is connected (in sessionOpened method)
     *  default value is true
     * */
    private final boolean autoStart;

    /** A flag set if client authentication is required */ 
    protected boolean needClientAuth = false;

    /** A flag set if client authentication is requested */ 
    protected boolean wantClientAuth = false;
    
    /** The enabled Ciphers. */
    protected String[] enabledCipherSuites;
    
    /** 
     * The list of enabled SSL/TLS protocols. Must be an array of String, containing:
     * <ul>
     *   <li><b>SSLv2Hello</b></li>
     *   <li><b>SSLv3</b></li>
     *   <li><b>TLSv1.1</b> or <b>TLSv1</b></li>
     *   <li><b>TLSv1.2</b></li>
     *   <li><b>TLSv1.3</b></li>
     *   <li><b>NONE</b></li>
     * </ul> 
     * 
     * If null, we will use the default <em>SSLEngine</em> configurtation.
     **/
    protected String[] enabledProtocols;

    /**
     * EndPoint identification algorithms
     */
    private String identificationAlgorithm;

    /**
     * Creates a new SSL filter using the specified {@link SSLContext}.
     * 
     * @param sslContext The SSLContext to use
     */
    public SslFilter(SSLContext sslContext) {
        this(sslContext, true);
    }

    /**
     * Creates a new SSL filter using the specified {@link SSLContext}.
     * If the <code>autostart</code> flag is set to <code>true</code>, the
     * handshake will start immediately after the filter has been added
     * to the chain.
     *
     * @param sslContext The SSLContext to use
     * @param autoStart The flag used to tell the filter to start the handshake immediately
     */
    public SslFilter(SSLContext sslContext, boolean autoStart) {
        Objects.requireNonNull(sslContext, "ssl must not be null");

        this.sslContext = sslContext;
        this.autoStart = autoStart;
    }

    /**
     * @return <code>true</code> if the engine will <em>require</em> client
     *         authentication. This option is only useful to engines in the server
     *         mode.
     */
    public boolean isNeedClientAuth() {
        return needClientAuth;
    }

    /**
     * Configures the engine to <em>require</em> client authentication. This option
     * is only useful for engines in the server mode.
     * 
     * @param needClientAuth A flag set when client authentication is required
     */
    public void setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
    }

    /**
     * @return <code>true</code> if the engine will <em>request</em> client
     *         authentication. This option is only useful to engines in the server
     *         mode.
     */
    public boolean isWantClientAuth() {
        return wantClientAuth;
    }

    /**
     * Configures the engine to <em>request</em> client authentication. This option
     * is only useful for engines in the server mode.
     * 
     * @param wantClientAuth A flag set when client authentication is requested
     */
    public void setWantClientAuth(boolean wantClientAuth) {
        this.wantClientAuth = wantClientAuth;
    }

    /**
     * @return the list of cipher suites to be enabled when {@link SSLEngine} is
     *         initialized. <code>null</code> means 'use {@link SSLEngine}'s default.'
     */
    public String[] getEnabledCipherSuites() {
        return enabledCipherSuites;
    }

    /**
     * Sets the list of cipher suites to be enabled when {@link SSLEngine} is
     * initialized.
     *
     * @param enabledCipherSuites The list of enabled Cipher.
     *                            <code>null</code> means 'use {@link SSLEngine}'s default.'
     */
    public void setEnabledCipherSuites(String... enabledCipherSuites) {
        this.enabledCipherSuites = enabledCipherSuites;
    }

    /**
     * @return the endpoint identification algorithm to be used when {@link SSLEngine}
     * is initialized. <code>null</code> means 'use {@link SSLEngine}'s default.'
     */
    public String getEndpointIdentificationAlgorithm() {
        return identificationAlgorithm;
    }

    /**
     * Sets the endpoint identification algorithm to be used when {@link SSLEngine}
     * is initialized.
     *
     * @param identificationAlgorithm <code>null</code> means 'use {@link SSLEngine}'s default.'
     */
    public void setEndpointIdentificationAlgorithm(String identificationAlgorithm) {
        this.identificationAlgorithm = identificationAlgorithm;
    }


    /**
     * @return the list of protocols to be enabled when {@link SSLEngine} is
     *         initialized. <code>null</code> means 'use {@link SSLEngine}'s default.'
     */
    public String[] getEnabledProtocols() {
        return enabledProtocols;
    }

    /**
     * Gets the given session's SslHandler.
     *
     * @param session An IoSession to query.
     * @return the given session's SslHandler.
     */
    private SslHandler getSslHandler(IoSession session) {
        return SslHandler.class.cast(session.getAttribute(SSL_HANDLER));
    }

    /**
     * Sets the list of protocols to be enabled when {@link SSLEngine} is
     * initialized.
     *
     * @param enabledProtocols The list of enabled SSL/TLS protocols.
     *                  <code>null</code> means 'use {@link SSLEngine}'s default.'
     */
    public void setEnabledProtocols(String... enabledProtocols) {
        this.enabledProtocols = enabledProtocols;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPreAdd(IoFilterChain parent, String name, NextFilter next) throws Exception {
        // Check that we don't have a SSL filter already present in the chain
        if (parent.contains(SslFilter.class)) {
            throw new IllegalStateException("Only one SSL filter is permitted in a chain");
        }

        if (LOGGER.isDebugEnabled()) {
            if (parent.getSession().isServer()) {
                LOGGER.debug("SERVER: Adding the SSL Filter '{}' to the chain", name);
            } else {
                LOGGER.debug("CLIENT: Adding the SSL Filter '{}' to the chain", name);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPostAdd(IoFilterChain parent, String name, NextFilter next) throws Exception {
        IoSession session = parent.getSession();

        if (session.isConnected() && autoStart) {
            onConnected(next, session);
        }

        super.onPostAdd(parent, name, next);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPreRemove(IoFilterChain parent, String name, NextFilter next) throws Exception {
        IoSession session = parent.getSession();
        onClose(next, session, false);
    }

    /**
     * Internal method for performing post-connect operations; this can be triggered
     * during normal connect event or after the filter is added to the chain.
     * 
     * @param next The nextFilter to call in the chain
     * @param session The session instance
     * @throws SSLException Any exception thrown by the SslHandler closing
     */
    synchronized protected void onConnected(NextFilter next, IoSession session) throws SSLException {
        SslHandler sslHandler = getSslHandler(session);

        if (sslHandler == null) {
            InetSocketAddress s = InetSocketAddress.class.cast(session.getRemoteAddress());
            SSLEngine sslEngine = createEngine(session, s);
            sslHandler = new SSLHandlerG0(sslEngine, EXECUTOR, session);
            session.setAttribute(SSL_HANDLER, sslHandler);
        }

        sslHandler.open(next);
    }

    /**
     * Called when the session is going to be closed. We must shutdown the SslHandler instance.
     * 
     * @param next The nextFilter to call in the chain
     * @param session The session instance
     * @param linger if true, write any queued messages before closing
     * @throws SSLException Any exception thrown by the SslHandler closing
     */
    synchronized protected void onClose(NextFilter next, IoSession session, boolean linger) throws SSLException {
        session.removeAttribute(SSL_SECURED);
        SslHandler sslHandler = SslHandler.class.cast(session.removeAttribute(SSL_HANDLER));
        
        if (sslHandler != null) {
            sslHandler.close(next, linger);
        }
    }

    /**
     * Customization handler for creating the engine
     * 
     * @param session source session
     * @param addr    socket address used for fast reconnect
     * @return an SSLEngine
     */
    protected SSLEngine createEngine(IoSession session, InetSocketAddress addr) {
        SSLEngine sslEngine;
        
        if (addr != null) { 
            sslEngine = sslContext.createSSLEngine(addr.getHostName(), addr.getPort());
        } else {
            sslEngine =  sslContext.createSSLEngine();
        }
        
        // Always start with WANT, which will be squashed by NEED if NEED is true.
        // Actually, it makes not a lot of sense to select NEED and WANT. NEED >> WANT...
        if (wantClientAuth) {
            sslEngine.setWantClientAuth(true);
        }

        if (needClientAuth) {
            sslEngine.setNeedClientAuth(true);
        }
        
        if (enabledCipherSuites != null) {
            sslEngine.setEnabledCipherSuites(enabledCipherSuites);
        }
        
        if (enabledProtocols != null) {
            sslEngine.setEnabledProtocols(enabledProtocols);
        }

        // Set the endpoint identification algorithm
        if (getEndpointIdentificationAlgorithm() != null) {
            SSLParameters sslParameters = sslEngine.getSSLParameters();
            sslParameters.setEndpointIdentificationAlgorithm(getEndpointIdentificationAlgorithm());
            sslEngine.setSSLParameters(sslParameters);
        }
        
        sslEngine.setUseClientMode(!session.isServer());
        
        return sslEngine;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionOpened(NextFilter next, IoSession session) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            if (session.isServer()) {
                LOGGER.debug("SERVER: Session {} openend", session);
            } else {
                LOGGER.debug("CLIENT: Session {} openend", session);
            }
        }

        onConnected(next, session);
        super.sessionOpened(next, session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionClosed(NextFilter next, IoSession session) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            if (session.isServer()) {
                LOGGER.debug("SERVER: Session {} closed", session);
            } else {
                LOGGER.debug("CLIENT: Session {} closed", session);
            }
        }

        onClose(next, session, false);
        super.sessionClosed(next, session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageReceived(NextFilter next, IoSession session, Object message) throws Exception {
        //if (session.isServer()) {
            //System.out.println( ">>> Server messageReceived" );
        //} else {
            //System.out.println( ">>> Client messageReceived" );
        //}

        //System.out.println( message );
        
        if (LOGGER.isDebugEnabled()) {
            if (session.isServer()) {
                LOGGER.debug("SERVER: Session {} received {}", session, message);
            } else {
                LOGGER.debug("CLIENT: Session {} received {}", session, message);
            }
        }
        
        SslHandler sslHandler = getSslHandler(session);
        sslHandler.receive(next, IoBuffer.class.cast(message));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageSent(NextFilter next, IoSession session, WriteRequest request) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            if (session.isServer()) {
                LOGGER.debug("SERVER: Session {} ack {}", session, request);
            } else {
                LOGGER.debug("CLIENT: Session {} ack {}", session, request);
            }
        }

        if (request instanceof EncryptedWriteRequest) {
            EncryptedWriteRequest encryptedWriteRequest = EncryptedWriteRequest.class.cast(request);
            SslHandler sslHandler = getSslHandler(session);
            sslHandler.ack(next, request);
            
            if (encryptedWriteRequest.getOriginalRequest() != encryptedWriteRequest) {
                next.messageSent(session, encryptedWriteRequest.getOriginalRequest());
            }
        } else {
            super.messageSent(next, session, request);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void filterWrite(NextFilter next, IoSession session, WriteRequest request) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            if (session.isServer()) {
                LOGGER.debug("SERVER: Session {} write {}", session, request);
            } else {
                LOGGER.debug("CLIENT: Session {} write {}", session, request);
            }
        }

        if (request instanceof EncryptedWriteRequest || request instanceof DisableEncryptWriteRequest) {
            super.filterWrite(next, session, request);
        } else {
            SslHandler sslHandler = getSslHandler(session);
            sslHandler.write(next, request);
        }
    }
}
