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
package org.apache.mina.filter.keepalive;

import org.apache.mina.common.AttributeKey;
import org.apache.mina.common.DefaultWriteRequest;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.IoSessionLogger;
import org.apache.mina.common.WriteRequest;
import org.slf4j.Logger;

/**
 * An {@link IoFilter} that sends a keep-alive request on
 * <tt>sessionIdle</tt> event with {@link IdleStatus#READER_IDLE} and sends
 * back  the response for the keep-alive request.
 * 
 * <h2>Interference with {@link IoSessionConfig#setIdleTime(IdleStatus, int)}</h2>
 * 
 * This filter adjusts <tt>idleTime</tt> property for
 * {@link IdleStatus#READER_IDLE} automatically.  Changing the <tt>idleTime</tt>
 * property for {@link IdleStatus#READER_IDLE} will lead this filter to a
 * unexpected behavior.
 * 
 * <h2>Implementing {@link KeepAliveMessageFactory}</h2>
 * 
 * To use this filter, you have to provide an implementation of
 * {@link KeepAliveMessageFactory}, which determines a received or sent
 * message is a keep-alive message or not and creates a new keep-alive
 * message:
 * 
 * <table>
 * <tr>
 * <th>Name</th><th>Description</th><th>Implementation</th>
 * </tr>
 * <tr>
 * <td>Active</td>
 * <td>You want a keep-alive request is sent when the reader is idle.
 * Once the request is sent, the response for the request should be
 * received within <tt>keepAliveTimeout</tt> seconds.  Otherwise, the specified
 * {@link KeepAlivePolicy} should be enforced.  If a keep-alive request
 * is received, its response also should be sent back.</td>
 * <td>Both {@link KeepAliveMessageFactory#getRequest(IoSession)} and
 * {@link KeepAliveMessageFactory#getResponse(IoSession, Object)} must
 * return a non-<tt>null</tt>.</td>
 * </tr>
 * <tr>
 * <td>Semi-active</td>
 * <td>You want a keep-alive request to be sent when the reader is idle.
 * However, you don't really care if the response is received or not.
 * If a keep-alive request is received, its response should
 * also be sent back.
 * </td>
 * <td>Both {@link KeepAliveMessageFactory#getRequest(IoSession)} and
 * {@link KeepAliveMessageFactory#getResponse(IoSession, Object)} must
 * return a non-<tt>null</tt>, and the <tt>policy</tt> property
 * should be set to {@link KeepAlivePolicy#OFF} or {@link KeepAlivePolicy#LOG}.
 * </td>
 * </tr>
 * <tr>
 * <td>Passive</td>
 * <td>You don't want to send a keep-alive request by yourself, but the
 * response should be sent back if a keep-alive request is received.</td>
 * <td>{@link KeepAliveMessageFactory#getRequest(IoSession)} must return
 * <tt>null</tt> and {@link KeepAliveMessageFactory#getResponse(IoSession, Object)}
 * must return a non-<tt>null</tt>.</td>
 * </tr>
 * <tr>
 * <td>Deaf Speaker</td>
 * <td>You want a keep-alive request to be sent when the reader is idle, but
 * you don't want to send any response back.</td> 
 * <td>{@link KeepAliveMessageFactory#getRequest(IoSession)} must return
 * a non-<tt>null</tt> and
 * {@link KeepAliveMessageFactory#getResponse(IoSession, Object)} must
 * return <tt>null</tt>.</td>
 * </tr>
 * <tr>
 * <td>Silent Listener</td>
 * <td>You don't want to send a keep-alive request by yourself nor send any
 * response back.</td> 
 * <td>Both {@link KeepAliveMessageFactory#getRequest(IoSession)} and
 * {@link KeepAliveMessageFactory#getResponse(IoSession, Object)} must
 * return <tt>null</tt>.</td>
 * </tr>
 * </table>
 * Please note that you must implement
 * {@link KeepAliveMessageFactory#isRequest(IoSession, Object)} and
 * {@link KeepAliveMessageFactory#isResponse(IoSession, Object)} properly
 * whatever case you chose.
 * 
 * <h2>Enforcing a policy</h2>
 * 
 * You can enforce a predefined policy by specifying a {@link KeepAlivePolicy}.
 * The default policy is {@link KeepAlivePolicy#CLOSE}.  Setting the policy
 * to {@link KeepAlivePolicy#OFF} stops this filter from waiting for response
 * messages and therefore disables <tt>keepAliveRequestTimeout</tt> property.
 *  
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class KeepAliveFilter extends IoFilterAdapter {
    
    private final AttributeKey WAITING_FOR_RESPONSE = new AttributeKey(
            getClass(), "waitingForResponse");

    private final KeepAliveMessageFactory messageFactory;
    private volatile KeepAlivePolicy policy;
    private volatile int keepAliveRequestInterval;
    private volatile int keepAliveRequestTimeout;
    
    /**
     * Creates a new instance with the default {@link KeepAlivePolicy} and
     * the default timeout values (<tt>policy</tt> =
     * {@link KeepAlivePolicy#CLOSE}, <tt>keepAliveRequestInterval = 60</tt>
     * and <tt>keepAliveRequestTimeout = 30</tt>).
     */
    public KeepAliveFilter(KeepAliveMessageFactory messageFactory) {
        this(messageFactory, KeepAlivePolicy.CLOSE);
    }
    
    /**
     * Creates a new instance with the default timeout values
     * (<tt>keepAliveRequestInterval = 60</tt> and
     * <tt>keepAliveRequestTimeout = 30</tt>).
     */
    public KeepAliveFilter(
            KeepAliveMessageFactory messageFactory, KeepAlivePolicy policy) {
        this(messageFactory, policy, 60, 30);
    }

    /**
     * Creates a new instance.
     */
    public KeepAliveFilter(
            KeepAliveMessageFactory messageFactory, KeepAlivePolicy policy,
            int keepAliveRequestInterval, int keepAliveRequestTimeout) {
        if (messageFactory == null) {
            throw new NullPointerException("messageFactory");
        }
        if (policy == null) {
            throw new NullPointerException("policy");
        }
        
        this.messageFactory = messageFactory;
        this.policy = policy;
        
        setKeepAliveRequestInterval(keepAliveRequestInterval);
        setKeepAliveRequestTimeout(keepAliveRequestTimeout);
    }

    public KeepAlivePolicy getPolicy() {
        return policy;
    }

    public void setPolicy(KeepAlivePolicy policy) {
        if (policy == null) {
            throw new NullPointerException("policy");
        }
        this.policy = policy;
    }

    public int getKeepAliveRequestInterval() {
        return keepAliveRequestInterval;
    }

    public void setKeepAliveRequestInterval(int keepAliveRequestInterval) {
        if (keepAliveRequestInterval <= 0) {
            throw new IllegalArgumentException(
                    "keepAliveRequestInterval must be a positive integer: " +
                    keepAliveRequestInterval);
        }
        this.keepAliveRequestInterval = keepAliveRequestInterval;
    }

    public int getKeepAliveRequestTimeout() {
        return keepAliveRequestTimeout;
    }

    public void setKeepAliveRequestTimeout(int keepAliveRequestTimeout) {
        if (keepAliveRequestTimeout <= 0) {
            throw new IllegalArgumentException(
                    "keepAliveRequestTimeout must be a positive integer: " +
                    keepAliveRequestTimeout);
        }
        this.keepAliveRequestTimeout = keepAliveRequestTimeout;
    }

    public KeepAliveMessageFactory getMessageFactory() {
        return messageFactory;
    }

    @Override
    public void onPreAdd(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
        if (parent.contains(this)) {
            throw new IllegalArgumentException(
                    "You can't add the same filter instance more than once. " +
                    "Create another instance and add it.");
        }
    }

    @Override
    public void onPostAdd(
            IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
        resetStatus(parent.getSession());
    }

    @Override
    public void onPostRemove(
            IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
        resetStatus(parent.getSession());
    }

    @Override
    public void messageReceived(
            NextFilter nextFilter, IoSession session, Object message) throws Exception {
        try {
            if (messageFactory.isRequest(session, message)) {
                Object pongMessage =
                    messageFactory.getResponse(session, message);
                
                if (pongMessage != null) {
                    nextFilter.filterWrite(
                            session, new DefaultWriteRequest(pongMessage));
                }
            }
            
            if (messageFactory.isResponse(session, message)) {
                resetStatus(session);
            }
        } finally {
            if (!isKeepAliveMessage(session, message)) {
                nextFilter.messageReceived(session, message);
            }
        }
    }

    @Override
    public void messageSent(
            NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        Object message = writeRequest.getMessage();
        if (!isKeepAliveMessage(session, message)) {
            nextFilter.messageSent(session, writeRequest);
        }
    }

    @Override
    public void sessionIdle(
            NextFilter nextFilter, IoSession session, IdleStatus status) throws Exception {
        try {
            if (status == IdleStatus.READER_IDLE) {
                if (!session.containsAttribute(WAITING_FOR_RESPONSE)) {
                    Object pingMessage = messageFactory.getRequest(session);
                    if (pingMessage != null) {
                        nextFilter.filterWrite(
                                session,
                                new DefaultWriteRequest(pingMessage));
                        
                        // If policy is OFF, there's no need to wait for
                        // the response.
                        if (getPolicy() != KeepAlivePolicy.OFF) {
                            markStatus(session);
                        } else {
                            resetStatus(session);
                        }
                    }
                } else {
                    resetStatus(session);
                    switch (getPolicy()) {
                    case OFF:
                        break;
                    case LOG:
                        logTimeout(session);
                        break;
                    case EXCEPTION:
                        throw new KeepAliveTimeoutException(
                                getTimeoutMessage());
                    case CLOSE:
                        logTimeout(session);
                        session.close();
                        break;
                    default:
                        throw new InternalError();
                    }
                }
            }
        } finally {
            nextFilter.sessionIdle(session, status);
        }
    }

    private void logTimeout(IoSession session) {
        Logger logger = IoSessionLogger.getLogger(session, getClass());
        if (logger.isWarnEnabled()) {
            logger.warn(getTimeoutMessage());
        }
    }

    private String getTimeoutMessage() {
        return "Keep-alive response message was not received within " + 
               getKeepAliveRequestTimeout() + " second(s).";
    }

    private void markStatus(IoSession session) {
        session.getConfig().setIdleTime(
                IdleStatus.READER_IDLE, getKeepAliveRequestTimeout());
        session.setAttribute(WAITING_FOR_RESPONSE);
    }

    private void resetStatus(IoSession session) {
        session.getConfig().setIdleTime(
                IdleStatus.READER_IDLE, getKeepAliveRequestInterval());
        session.removeAttribute(WAITING_FOR_RESPONSE);
    }
    
    private boolean isKeepAliveMessage(IoSession session, Object message) {
        return messageFactory.isRequest(session, message) ||
               messageFactory.isResponse(session, message);
    }
}
