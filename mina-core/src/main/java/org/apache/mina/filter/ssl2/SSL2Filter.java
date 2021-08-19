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
package org.apache.mina.filter.ssl2;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

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
 * An simple SSL processor which performs flow control of encrypted information
 * on the filter-chain.
 * <p>
 * The initial handshake is automatically enabled for "client" sessions once the
 * filter is added to the filter-chain and the session is connected.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SSL2Filter extends IoFilterAdapter {
	/**
	 * The presence of this attribute in a session indicates that the session is
	 * secured.
	 */
	static public final AttributeKey SSL_SECURED = new AttributeKey(SSL2Filter.class, "status");

	/**
	 * Returns the SSL2Handler object
	 */
	static protected final AttributeKey SSL_HANDLER = new AttributeKey(SSL2Filter.class, "handler");

	/**
	 * The logger
	 */
	static protected final Logger LOGGER = LoggerFactory.getLogger(SSL2Filter.class);

	/**
	 * Task executor for processing handshakes
	 */
	static protected final Executor EXECUTOR = new ThreadPoolExecutor(2, 2, 100, TimeUnit.MILLISECONDS,
			new LinkedBlockingDeque<Runnable>(), new BasicThreadFactory("ssl-exec", true));

	protected final SSLContext mContext;
	protected boolean mNeedClientAuth;
	protected boolean mWantClientAuth;
	protected String[] mEnabledCipherSuites;
	protected String[] mEnabledProtocols;

	/**
	 * Creates a new SSL filter using the specified {@link SSLContext}.
	 * 
	 * @param context The SSLContext to use
	 */
	public SSL2Filter(SSLContext context) {
		Objects.requireNonNull(context, "ssl must not be null");

		this.mContext = context;
	}

	/**
	 * @return <tt>true</tt> if the engine will <em>require</em> client
	 *         authentication. This option is only useful to engines in the server
	 *         mode.
	 */
	public boolean isNeedClientAuth() {
		return mNeedClientAuth;
	}

	/**
	 * Configures the engine to <em>require</em> client authentication. This option
	 * is only useful for engines in the server mode.
	 * 
	 * @param needClientAuth A flag set when we need to authenticate the client
	 */
	public void setNeedClientAuth(boolean needClientAuth) {
		this.mNeedClientAuth = needClientAuth;
	}

	/**
	 * @return <tt>true</tt> if the engine will <em>request</em> client
	 *         authentication. This option is only useful to engines in the server
	 *         mode.
	 */
	public boolean isWantClientAuth() {
		return mWantClientAuth;
	}

	/**
	 * Configures the engine to <em>request</em> client authentication. This option
	 * is only useful for engines in the server mode.
	 * 
	 * @param wantClientAuth A flag set when we want to check the client
	 *                       authentication
	 */
	public void setWantClientAuth(boolean wantClientAuth) {
		this.mWantClientAuth = wantClientAuth;
	}

	/**
	 * @return the list of cipher suites to be enabled when {@link SSLEngine} is
	 *         initialized. <tt>null</tt> means 'use {@link SSLEngine}'s default.'
	 */
	public String[] getEnabledCipherSuites() {
		return mEnabledCipherSuites;
	}

	/**
	 * Sets the list of cipher suites to be enabled when {@link SSLEngine} is
	 * initialized.
	 *
	 * @param cipherSuites <tt>null</tt> means 'use {@link SSLEngine}'s default.'
	 */
	public void setEnabledCipherSuites(String[] cipherSuites) {
		this.mEnabledCipherSuites = cipherSuites;
	}

	/**
	 * @return the list of protocols to be enabled when {@link SSLEngine} is
	 *         initialized. <tt>null</tt> means 'use {@link SSLEngine}'s default.'
	 */
	public String[] getEnabledProtocols() {
		return mEnabledProtocols;
	}

	/**
	 * Sets the list of protocols to be enabled when {@link SSLEngine} is
	 * initialized.
	 *
	 * @param protocols <tt>null</tt> means 'use {@link SSLEngine}'s default.'
	 */
	public void setEnabledProtocols(String[] protocols) {
		this.mEnabledProtocols = protocols;
	}

	@Override
	public void onPreAdd(IoFilterChain parent, String name, NextFilter next) throws Exception {
		// Check that we don't have a SSL filter already present in the chain
		if (parent.contains(SSL2Filter.class)) {
			throw new IllegalStateException("Only one SSL filter is permitted in a chain");
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Adding the SSL Filter {} to the chain", name);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPostAdd(IoFilterChain parent, String name, NextFilter next) throws Exception {
		IoSession session = parent.getSession();
		if (session.isConnected()) {
			this.onConnected(next, session);
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
	 * @param next
	 * @param session
	 * @throws Exception
	 */
	synchronized protected void onConnected(NextFilter next, IoSession session) throws Exception {
		SSL2Handler x = SSL2Handler.class.cast(session.getAttribute(SSL_HANDLER));

		if (x == null) {
			final InetSocketAddress s = InetSocketAddress.class.cast(session.getRemoteAddress());
			final SSLEngine e = this.createEngine(session, s);
			x = new SSL2HandlerG0(e, EXECUTOR, session);
			session.setAttribute(SSL_HANDLER, x);
		}

		x.open(next);
	}

	synchronized protected void onClose(NextFilter next, IoSession session, boolean linger) throws Exception {
		session.removeAttribute(SSL_SECURED);
		SSL2Handler x = SSL2Handler.class.cast(session.removeAttribute(SSL_HANDLER));
		if (x != null) {
			x.close(next, linger);
		}
	}
	
	/**
	 * Customization handler for creating the engine
	 * 
	 * @param session
	 * @param s
	 * @return an SSLEngine
	 */
	protected SSLEngine createEngine(IoSession session, InetSocketAddress s) {
		SSLEngine e = (s != null) ? mContext.createSSLEngine(s.getHostString(), s.getPort())
				: mContext.createSSLEngine();
		e.setNeedClientAuth(mNeedClientAuth);
		e.setWantClientAuth(mWantClientAuth);
		if (this.mEnabledCipherSuites != null) {
			e.setEnabledCipherSuites(this.mEnabledCipherSuites);
		}
		if (this.mEnabledProtocols != null) {
			e.setEnabledProtocols(this.mEnabledProtocols);
		}
		e.setUseClientMode(!session.isServer());
		return e;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sessionOpened(NextFilter next, IoSession session) throws Exception {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("session {} openend", session);

		this.onConnected(next, session);
		super.sessionOpened(next, session);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sessionClosed(NextFilter next, IoSession session) throws Exception {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("session {} closed", session);
		this.onClose(next, session, false);
		super.sessionClosed(next, session);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void messageReceived(NextFilter next, IoSession session, Object message) throws Exception {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("session {} received {}", session, message);
		SSL2Handler x = SSL2Handler.class.cast(session.getAttribute(SSL_HANDLER));
		x.receive(next, IoBuffer.class.cast(message));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void messageSent(NextFilter next, IoSession session, WriteRequest request) throws Exception {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("session {} ack {}", session, request);

		if (request instanceof EncryptedWriteRequest) {
			EncryptedWriteRequest e = EncryptedWriteRequest.class.cast(request);
			SSL2Handler x = SSL2Handler.class.cast(session.getAttribute(SSL_HANDLER));
			x.ack(next, request);
			if (e.getOriginalRequest() != e) {
				next.messageSent(session, e.getOriginalRequest());
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
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("session {} write {}", session, request);

		if (request instanceof EncryptedWriteRequest) {
			super.filterWrite(next, session, request);
		} else {
			SSL2Handler x = SSL2Handler.class.cast(session.getAttribute(SSL_HANDLER));
			x.write(next, request);
		}
	}
}
