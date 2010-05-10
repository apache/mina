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

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoEventType;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;

/**
 * An {@link IoFilter} that sends a keep-alive request on
 * {@link IoEventType#SESSION_IDLE} and sends back the response for the
 * sent keep-alive request.
 *
 * <h2>Interference with {@link IoSessionConfig#setIdleTime(IdleStatus, int)}</h2>
 *
 * This filter adjusts <tt>idleTime</tt> of the {@link IdleStatus}s that
 * this filter is interested in automatically (e.g. {@link IdleStatus#READER_IDLE}
 * and {@link IdleStatus#WRITER_IDLE}.)  Changing the <tt>idleTime</tt>
 * of the {@link IdleStatus}s can lead this filter to a unexpected behavior.
 * Please also note that any {@link IoFilter} and {@link IoHandler} behind
 * {@link KeepAliveFilter} will not get any {@link IoEventType#SESSION_IDLE}
 * event.  To receive the internal {@link IoEventType#SESSION_IDLE} event,
 * you can call {@link #setForwardEvent(boolean)} with <tt>true</tt>.
 *
 * <h2>Implementing {@link KeepAliveMessageFactory}</h2>
 *
 * To use this filter, you have to provide an implementation of
 * {@link KeepAliveMessageFactory}, which determines a received or sent
 * message is a keep-alive message or not and creates a new keep-alive
 * message:
 *
 * <table border="1">
 * <tr>
 * <th>Name</th><th>Description</th><th>Implementation</th>
 * </tr>
 * <tr valign="top">
 * <td>Active</td>
 * <td>You want a keep-alive request is sent when the reader is idle.
 * Once the request is sent, the response for the request should be
 * received within <tt>keepAliveRequestTimeout</tt> seconds.  Otherwise,
 * the specified {@link KeepAliveRequestTimeoutHandler} will be invoked.
 * If a keep-alive request is received, its response also should be sent back.
 * </td>
 * <td>Both {@link KeepAliveMessageFactory#getRequest(IoSession)} and
 * {@link KeepAliveMessageFactory#getResponse(IoSession, Object)} must
 * return a non-<tt>null</tt>.</td>
 * </tr>
 * <tr valign="top">
 * <td>Semi-active</td>
 * <td>You want a keep-alive request to be sent when the reader is idle.
 * However, you don't really care if the response is received or not.
 * If a keep-alive request is received, its response should
 * also be sent back.
 * </td>
 * <td>Both {@link KeepAliveMessageFactory#getRequest(IoSession)} and
 * {@link KeepAliveMessageFactory#getResponse(IoSession, Object)} must
 * return a non-<tt>null</tt>, and the <tt>timeoutHandler</tt> property
 * should be set to {@link KeepAliveRequestTimeoutHandler#NOOP},
 * {@link KeepAliveRequestTimeoutHandler#LOG} or the custom {@link KeepAliveRequestTimeoutHandler}
 * implementation that doesn't affect the session state nor throw an exception.
 * </td>
 * </tr>
 * <tr valign="top">
 * <td>Passive</td>
 * <td>You don't want to send a keep-alive request by yourself, but the
 * response should be sent back if a keep-alive request is received.</td>
 * <td>{@link KeepAliveMessageFactory#getRequest(IoSession)} must return
 * <tt>null</tt> and {@link KeepAliveMessageFactory#getResponse(IoSession, Object)}
 * must return a non-<tt>null</tt>.</td>
 * </tr>
 * <tr valign="top">
 * <td>Deaf Speaker</td>
 * <td>You want a keep-alive request to be sent when the reader is idle, but
 * you don't want to send any response back.</td>
 * <td>{@link KeepAliveMessageFactory#getRequest(IoSession)} must return
 * a non-<tt>null</tt>,
 * {@link KeepAliveMessageFactory#getResponse(IoSession, Object)} must
 * return <tt>null</tt> and the <tt>timeoutHandler</tt> must be set to
 * {@link KeepAliveRequestTimeoutHandler#DEAF_SPEAKER}.</td>
 * </tr>
 * <tr valign="top">
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
 * <h2>Handling timeout</h2>
 *
 * {@link KeepAliveFilter} will notify its {@link KeepAliveRequestTimeoutHandler}
 * when {@link KeepAliveFilter} didn't receive the response message for a sent
 * keep-alive message.  The default handler is {@link KeepAliveRequestTimeoutHandler#CLOSE},
 * but you can use other presets such as {@link KeepAliveRequestTimeoutHandler#NOOP},
 * {@link KeepAliveRequestTimeoutHandler#LOG} or {@link KeepAliveRequestTimeoutHandler#EXCEPTION}.
 * You can even implement your own handler.
 *
 * <h3>Special handler: {@link KeepAliveRequestTimeoutHandler#DEAF_SPEAKER}</h3>
 *
 * {@link KeepAliveRequestTimeoutHandler#DEAF_SPEAKER} is a special handler which is
 * dedicated for the 'deaf speaker' mode mentioned above.  Setting the
 * <tt>timeoutHandler</tt> property to {@link KeepAliveRequestTimeoutHandler#DEAF_SPEAKER}
 * stops this filter from waiting for response messages and therefore disables
 * response timeout detection.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @org.apache.xbean.XBean
 */
public class KeepAliveFilter extends IoFilterAdapter {

    private final AttributeKey WAITING_FOR_RESPONSE = new AttributeKey(
            getClass(), "waitingForResponse");
    private final AttributeKey IGNORE_READER_IDLE_ONCE = new AttributeKey(
            getClass(), "ignoreReaderIdleOnce");

    private final KeepAliveMessageFactory messageFactory;
    private final IdleStatus interestedIdleStatus;
    private volatile KeepAliveRequestTimeoutHandler requestTimeoutHandler;
    private volatile int requestInterval;
    private volatile int requestTimeout;
    private volatile boolean forwardEvent;

    /**
     * Creates a new instance with the default properties.
     * The default property values are:
     * <ul>
     * <li><tt>interestedIdleStatus</tt> - {@link IdleStatus#READER_IDLE}</li>
     * <li><tt>policy</tt> = {@link KeepAliveRequestTimeoutHandler#CLOSE}</li>
     * <li><tt>keepAliveRequestInterval</tt> - 60 (seconds)</li>
     * <li><tt>keepAliveRequestTimeout</tt> - 30 (seconds)</li>
     * </ul>
     */
    public KeepAliveFilter(KeepAliveMessageFactory messageFactory) {
        this(messageFactory, IdleStatus.READER_IDLE, KeepAliveRequestTimeoutHandler.CLOSE);
    }

    /**
     * Creates a new instance with the default properties.
     * The default property values are:
     * <ul>
     * <li><tt>policy</tt> = {@link KeepAliveRequestTimeoutHandler#CLOSE}</li>
     * <li><tt>keepAliveRequestInterval</tt> - 60 (seconds)</li>
     * <li><tt>keepAliveRequestTimeout</tt> - 30 (seconds)</li>
     * </ul>
     */
    public KeepAliveFilter(
            KeepAliveMessageFactory messageFactory,
            IdleStatus interestedIdleStatus) {
        this(messageFactory, interestedIdleStatus, KeepAliveRequestTimeoutHandler.CLOSE, 60, 30);
    }

    /**
     * Creates a new instance with the default properties.
     * The default property values are:
     * <ul>
     * <li><tt>interestedIdleStatus</tt> - {@link IdleStatus#READER_IDLE}</li>
     * <li><tt>keepAliveRequestInterval</tt> - 60 (seconds)</li>
     * <li><tt>keepAliveRequestTimeout</tt> - 30 (seconds)</li>
     * </ul>
     */
    public KeepAliveFilter(
            KeepAliveMessageFactory messageFactory, KeepAliveRequestTimeoutHandler policy) {
        this(messageFactory, IdleStatus.READER_IDLE, policy, 60, 30);
    }

    /**
     * Creates a new instance with the default properties.
     * The default property values are:
     * <ul>
     * <li><tt>keepAliveRequestInterval</tt> - 60 (seconds)</li>
     * <li><tt>keepAliveRequestTimeout</tt> - 30 (seconds)</li>
     * </ul>
     */
    public KeepAliveFilter(
            KeepAliveMessageFactory messageFactory,
            IdleStatus interestedIdleStatus, KeepAliveRequestTimeoutHandler policy) {
        this(messageFactory, interestedIdleStatus, policy, 60, 30);
    }

    /**
     * Creates a new instance.
     */
    public KeepAliveFilter(
            KeepAliveMessageFactory messageFactory,
            IdleStatus interestedIdleStatus, KeepAliveRequestTimeoutHandler policy,
            int keepAliveRequestInterval, int keepAliveRequestTimeout) {
        if (messageFactory == null) {
            throw new IllegalArgumentException("messageFactory");
        }
        if (interestedIdleStatus == null) {
            throw new IllegalArgumentException("interestedIdleStatus");
        }
        if (policy == null) {
            throw new IllegalArgumentException("policy");
        }

        this.messageFactory = messageFactory;
        this.interestedIdleStatus = interestedIdleStatus;
        requestTimeoutHandler = policy;

        setRequestInterval(keepAliveRequestInterval);
        setRequestTimeout(keepAliveRequestTimeout);
    }

    public IdleStatus getInterestedIdleStatus() {
        return interestedIdleStatus;
    }

    public KeepAliveRequestTimeoutHandler getRequestTimeoutHandler() {
        return requestTimeoutHandler;
    }

    public void setRequestTimeoutHandler(KeepAliveRequestTimeoutHandler timeoutHandler) {
        if (timeoutHandler == null) {
            throw new IllegalArgumentException("timeoutHandler");
        }
        requestTimeoutHandler = timeoutHandler;
    }

    public int getRequestInterval() {
        return requestInterval;
    }

    public void setRequestInterval(int keepAliveRequestInterval) {
        if (keepAliveRequestInterval <= 0) {
            throw new IllegalArgumentException(
                    "keepAliveRequestInterval must be a positive integer: " +
                    keepAliveRequestInterval);
        }
        requestInterval = keepAliveRequestInterval;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(int keepAliveRequestTimeout) {
        if (keepAliveRequestTimeout <= 0) {
            throw new IllegalArgumentException(
                    "keepAliveRequestTimeout must be a positive integer: " +
                    keepAliveRequestTimeout);
        }
        requestTimeout = keepAliveRequestTimeout;
    }

    public KeepAliveMessageFactory getMessageFactory() {
        return messageFactory;
    }

    /**
     * Returns <tt>true</tt> if and only if this filter forwards
     * a {@link IoEventType#SESSION_IDLE} event to the next filter.
     * By default, the value of this property is <tt>false</tt>.
     */
    public boolean isForwardEvent() {
        return forwardEvent;
    }

    /**
     * Sets if this filter needs to forward a
     * {@link IoEventType#SESSION_IDLE} event to the next filter.
     * By default, the value of this property is <tt>false</tt>.
     */
    public void setForwardEvent(boolean forwardEvent) {
        this.forwardEvent = forwardEvent;
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
        if (status == interestedIdleStatus) {
            if (!session.containsAttribute(WAITING_FOR_RESPONSE)) {
                Object pingMessage = messageFactory.getRequest(session);
                if (pingMessage != null) {
                    nextFilter.filterWrite(
                            session,
                            new DefaultWriteRequest(pingMessage));

                    // If policy is OFF, there's no need to wait for
                    // the response.
                    if (getRequestTimeoutHandler() != KeepAliveRequestTimeoutHandler.DEAF_SPEAKER) {
                        markStatus(session);
                        if (interestedIdleStatus == IdleStatus.BOTH_IDLE) {
                            session.setAttribute(IGNORE_READER_IDLE_ONCE);
                        }
                    } else {
                        resetStatus(session);
                    }
                }
            } else {
                handlePingTimeout(session);
            }
        } else if (status == IdleStatus.READER_IDLE) {
            if (session.removeAttribute(IGNORE_READER_IDLE_ONCE) == null) {
                if (session.containsAttribute(WAITING_FOR_RESPONSE)) {
                    handlePingTimeout(session);
                }
            }
        }

        if (forwardEvent) {
            nextFilter.sessionIdle(session, status);
        }
    }

    private void handlePingTimeout(IoSession session) throws Exception {
        resetStatus(session);
        KeepAliveRequestTimeoutHandler handler = getRequestTimeoutHandler();
        if (handler == KeepAliveRequestTimeoutHandler.DEAF_SPEAKER) {
            return;
        }

        handler.keepAliveRequestTimedOut(this, session);
    }

    private void markStatus(IoSession session) {
        session.getConfig().setIdleTime(interestedIdleStatus, 0);
        session.getConfig().setReaderIdleTime(getRequestTimeout());
        session.setAttribute(WAITING_FOR_RESPONSE);
    }

    private void resetStatus(IoSession session) {
        session.getConfig().setReaderIdleTime(0);
        session.getConfig().setWriterIdleTime(0);
        session.getConfig().setIdleTime(
                interestedIdleStatus, getRequestInterval());
        session.removeAttribute(WAITING_FOR_RESPONSE);
    }

    private boolean isKeepAliveMessage(IoSession session, Object message) {
        return messageFactory.isRequest(session, message) ||
        messageFactory.isResponse(session, message);
    }
}
