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
 * An SSL filter that encrypts and decrypts the data exchanged in the session.
 * Adding this filter triggers SSL handshake procedure immediately by sending a
 * SSL 'hello' message, so you don't need to call {@link #startSsl(IoSession)}
 * manually unless you are implementing StartTLS (see below). If you don't want
 * the handshake procedure to start immediately, please specify {@code false} as
 * {@code autoStart} parameter in the constructor.
 * <p>
 * This filter uses an {@link SSLEngine} which was introduced in Java 5, so Java
 * version 5 or above is mandatory to use this filter. And please note that this
 * filter only works for TCP/IP connections.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SSL2Filter extends IoFilterAdapter {
	/**
	 * The logger
	 */
	protected static final Logger LOGGER = LoggerFactory.getLogger(SSL2Filter.class);

	protected static final Executor EXECUTOR = new ThreadPoolExecutor(2, 2, 100, TimeUnit.MILLISECONDS,
			new LinkedBlockingDeque<Runnable>(), new BasicThreadFactory("ssl-exec", true));

	protected static final AttributeKey SSL_HANDLER = new AttributeKey(SSL2Filter.class, "handler");

	protected final SSLContext mContext;

	protected boolean mNeedClientAuth;

	protected boolean mWantClientAuth;

	protected String[] mEnabledCipherSuites;

	protected String[] mEnabledProtocols;

	/**
	 * Creates a new SSL filter using the specified {@link SSLContext}.
	 * 
	 * @param sslContext The SSLContext to use
	 */
	public SSL2Filter(SSLContext sslContext) {
		if (sslContext == null) {
			throw new IllegalArgumentException("SSLContext is null");
		}

		this.mContext = sslContext;
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

	/**
	 * Executed just before the filter is added into the chain, we do :
	 * <ul>
	 * <li>check that we don't have a SSL filter already present
	 * <li>we update the next filter
	 * <li>we create the SSL handler helper class
	 * <li>and we store it into the session's Attributes
	 * </ul>
	 */
	@Override
	public void onPreAdd(IoFilterChain parent, String name, NextFilter next) throws Exception {
		// Check that we don't have a SSL filter already present in the chain
		if (parent.contains(SSL2Filter.class)) {
			String msg = "Only one SSL filter is permitted in a chain.";
			LOGGER.error(msg);
			throw new IllegalStateException(msg);
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Adding the SSL Filter {} to the chain", name);
		}
	}

	@Override
	public void onPreRemove(IoFilterChain parent, String name, NextFilter next) throws Exception {
		IoSession session = parent.getSession();
		session.removeAttribute(SSL_HANDLER);
	}

	@Override
	public void sessionOpened(NextFilter next, IoSession session) throws Exception {

		LOGGER.debug("session openend {}", session);

		SSL2Handler x = SSL2Handler.class.cast(session.getAttribute(SSL_HANDLER));

		if (x == null) {
			SSLEngine e = mContext.createSSLEngine();

			e.setNeedClientAuth(mNeedClientAuth);
			e.setWantClientAuth(mWantClientAuth);
			e.setEnabledCipherSuites(mEnabledCipherSuites);
			e.setEnabledProtocols(mEnabledProtocols);
			e.setUseClientMode(!session.isServer());

			x = new SSL2HandlerG0(e, EXECUTOR, session);

			session.setAttribute(SSL_HANDLER, x);
		}

		x.open(next);
	}

	@Override
	public void messageReceived(NextFilter next, IoSession session, Object message) throws Exception {
		SSL2Handler x = SSL2Handler.class.cast(session.getAttribute(SSL_HANDLER));
		x.receive(next, IoBuffer.class.cast(message));
	}

	@Override
	public void messageSent(NextFilter next, IoSession session, WriteRequest writeRequest) throws Exception {
		if (writeRequest instanceof EncryptedWriteRequest) {
			EncryptedWriteRequest e = EncryptedWriteRequest.class.cast(writeRequest);
			SSL2Handler x = SSL2Handler.class.cast(session.getAttribute(SSL_HANDLER));
			x.ack(next, writeRequest);
			if (e.getParentRequest() != null) {
				next.messageSent(session, e.getParentRequest());
			}
		} else {
			super.messageSent(next, session, writeRequest);
		}
	}

	@Override
	public void filterWrite(NextFilter next, IoSession session, WriteRequest writeRequest) throws Exception {

		LOGGER.debug("session write {}", session);

		if (writeRequest instanceof EncryptedWriteRequest) {
			super.filterWrite(next, session, writeRequest);
		} else {
			SSL2Handler x = SSL2Handler.class.cast(session.getAttribute(SSL_HANDLER));
			x.write(next, writeRequest);
		}
	}
}
