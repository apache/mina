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
package org.apache.mina.common;

import java.net.SocketAddress;
import java.util.Set;

/**
 * A handle which represents connection between two endpoints regardless of
 * transport types.
 * <p>
 * {@link IoSession} provides user-defined attributes.  User-defined attributes
 * are application-specific data which is associated with a session.
 * It often contains objects that represents the state of a higher-level protocol
 * and becomes a way to exchange data between filters and handlers.
 *
 * <h3>Adjusting Transport Type Specific Properties</h3>
 * <p>
 * You can simply downcast the session to an appropriate subclass.
 * </p>
 *
 * <h3>Thread Safety</h3>
 * <p>
 * {@link IoSession} is thread-safe.  But please note that performing
 * more than one {@link #write(Object)} calls at the same time will
 * cause the {@link IoFilter#filterWrite(IoFilter.NextFilter, IoSession, IoFilter.WriteRequest)}
 * is executed simnutaneously, and therefore you have to make sure the
 * {@link IoFilter} implementations you're using are thread-safe, too.
 * </p>
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoSession {

    /**
     * Returns the {@link IoService} which provides I/O service to this session.
     */
    IoService getService();

    /**
     * Returns the {@link IoServiceConfig} of this session.
     */
    IoServiceConfig getServiceConfig();

    /**
     * Returns the {@link IoHandler} which handles this session.
     */
    IoHandler getHandler();

    /**
     * Returns the configuration of this session.
     */
    IoSessionConfig getConfig();

    /**
     * Returns the filter chain that only affects this session.
     */
    IoFilterChain getFilterChain();

    /**
     * Writes the specified <code>message</code> to remote peer.  This
     * operation is asynchronous; {@link IoHandler#messageSent(IoSession, Object)}
     * will be invoked when the message is actually sent to remote peer.
     * You can also wait for the returned {@link WriteFuture} if you want
     * to wait for the message actually written.
     */
    WriteFuture write(Object message);

    /**
     * Closes this session immediately.  This operation is asynthronous.
     * Wait for the returned {@link CloseFuture} if you want to wait for
     * the session actually closed.
     */
    CloseFuture close();

    /**
     * Returns an attachment of this session.
     * This method is identical with <tt>getAttribute( "" )</tt>.
     */
    Object getAttachment();

    /**
     * Sets an attachment of this session.
     * This method is identical with <tt>setAttribute( "", attachment )</tt>.
     *
     * @return Old attachment.  <tt>null</tt> if it is new.
     */
    Object setAttachment(Object attachment);

    /**
     * Returns the value of user-defined attribute of this session.
     *
     * @param key the key of the attribute
     * @return <tt>null</tt> if there is no attribute with the specified key
     */
    Object getAttribute(String key);

    /**
     * Sets a user-defined attribute.
     *
     * @param key the key of the attribute
     * @param value the value of the attribute
     * @return The old value of the attribute.  <tt>null</tt> if it is new.
     */
    Object setAttribute(String key, Object value);

    /**
     * Sets a user defined attribute without a value.  This is useful when
     * you just want to put a 'mark' attribute.  Its value is set to
     * {@link Boolean#TRUE}.
     *
     * @param key the key of the attribute
     * @return The old value of the attribute.  <tt>null</tt> if it is new.
     */
    Object setAttribute(String key);

    /**
     * Removes a user-defined attribute with the specified key.
     *
     * @return The old value of the attribute.  <tt>null</tt> if not found.
     */
    Object removeAttribute(String key);

    /**
     * Returns <tt>true</tt> if this session contains the attribute with
     * the specified <tt>key</tt>.
     */
    boolean containsAttribute(String key);

    /**
     * Returns the set of keys of all user-defined attributes.
     */
    Set<String> getAttributeKeys();

    /**
     * Returns transport type of this session.
     */
    TransportType getTransportType();

    /**
     * Returns <code>true</code> if this session is connected with remote peer.
     */
    boolean isConnected();

    /**
     * Returns <code>true</tt> if and only if this session is being closed
     * (but not disconnected yet) or is closed.
     */
    boolean isClosing();

    /**
     * Returns the {@link CloseFuture} of this session.  This method returns
     * the same instance whenever user calls it.
     */
    CloseFuture getCloseFuture();

    /**
     * Returns the socket address of remote peer.
     */
    SocketAddress getRemoteAddress();

    /**
     * Returns the socket address of local machine which is associated with this
     * session.
     */
    SocketAddress getLocalAddress();

    /**
     * Returns the socket address of the {@link IoService} listens to to manage
     * this session.  If this session is managed by {@link IoAcceptor}, it
     * returns the {@link SocketAddress} which is specified as a parameter of
     * {@link IoAcceptor#bind(SocketAddress, IoHandler)}.  If this session is
     * managed by {@link IoConnector}, this method returns the same address with
     * that of {@link #getRemoteAddress()}.
     */
    SocketAddress getServiceAddress();

    /**
     * Returns idle time for the specified type of idleness in seconds.
     */
    int getIdleTime(IdleStatus status);

    /**
     * Returns idle time for the specified type of idleness in milliseconds.
     */
    long getIdleTimeInMillis(IdleStatus status);

    /**
     * Sets idle time for the specified type of idleness in seconds.
     */
    void setIdleTime(IdleStatus status, int idleTime);

    /**
     * Returns write timeout in seconds.
     */
    int getWriteTimeout();

    /**
     * Returns write timeout in milliseconds.
     */
    long getWriteTimeoutInMillis();

    /**
     * Sets write timeout in seconds.
     */
    void setWriteTimeout(int writeTimeout);

    /**
     * Returns the current {@link TrafficMask} of this session.
     */
    TrafficMask getTrafficMask();

    /**
     * Sets the {@link TrafficMask} of this session which will result
     * the parent {@link IoService} to start to control the traffic
     * of this session immediately.
     */
    void setTrafficMask(TrafficMask trafficMask);

    /**
     * A shortcut method for {@link #setTrafficMask(TrafficMask)} that
     * suspends read operations for this session.
     */
    void suspendRead();

    /**
     * A shortcut method for {@link #setTrafficMask(TrafficMask)} that
     * suspends write operations for this session.
     */
    void suspendWrite();

    /**
     * A shortcut method for {@link #setTrafficMask(TrafficMask)} that
     * resumes read operations for this session.
     */
    void resumeRead();

    /**
     * A shortcut method for {@link #setTrafficMask(TrafficMask)} that
     * resumes write operations for this session.
     */
    void resumeWrite();

    /**
     * Returns the total number of bytes which were read from this session.
     */
    long getReadBytes();

    /**
     * Returns the total number of bytes which were written to this session.
     */
    long getWrittenBytes();

    /**
     * Returns the total number of messages which were read and decoded from this session.
     */
    long getReadMessages();

    /**
     * Returns the total number of messages which were written and encoded by this session.
     */
    long getWrittenMessages();

    /**
     * Returns the total number of write requests which were written to this session.
     */
    long getWrittenWriteRequests();

    /**
     * Returns the number of write requests which are scheduled to be written
     * to this session.
     */
    int getScheduledWriteRequests();

    /**
     * Returns the number of bytes which are scheduled to be written to this
     * session.
     */
    int getScheduledWriteBytes();

    /**
     * Returns the time in millis when this session is created.
     */
    long getCreationTime();

    /**
     * Returns the time in millis when I/O occurred lastly.
     */
    long getLastIoTime();

    /**
     * Returns the time in millis when read operation occurred lastly.
     */
    long getLastReadTime();

    /**
     * Returns the time in millis when write operation occurred lastly.
     */
    long getLastWriteTime();

    /**
     * Returns <code>true</code> if this session is idle for the specified
     * {@link IdleStatus}.
     */
    boolean isIdle(IdleStatus status);

    /**
     * Returns the number of the fired continuous <tt>sessionIdle</tt> events
     * for the specified {@link IdleStatus}.
     * <p>
     * If <tt>sessionIdle</tt> event is fired first after some time after I/O,
     * <tt>idleCount</tt> becomes <tt>1</tt>.  <tt>idleCount</tt> resets to
     * <tt>0</tt> if any I/O occurs again, otherwise it increases to
     * <tt>2</tt> and so on if <tt>sessionIdle</tt> event is fired again without
     * any I/O between two (or more) <tt>sessionIdle</tt> events.
     */
    int getIdleCount(IdleStatus status);

    /**
     * Returns the time in millis when the last <tt>sessionIdle</tt> event
     * is fired for the specified {@link IdleStatus}.
     */
    long getLastIdleTime(IdleStatus status);
}
