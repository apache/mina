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
package org.apache.mina.api;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.apache.mina.session.AttributeKey;
import org.apache.mina.session.WriteRequest;
import org.apache.mina.transport.nio.SelectorLoop;
import org.apache.mina.transport.nio.SslHelper;

/**
 * A handle which represents a connection between two end-points regardless of transport types.
 * <p/>
 * {@link IoSession} provides user-defined attributes. User-defined attributes are application-specific data which are
 * associated with a session. It often contains objects that represents the state of a higher-level protocol and becomes
 * a way to exchange data between filters and handlers.
 * <p/>
 * <h3>Adjusting Transport Type Specific Properties</h3>
 * <p/>
 * You can simply downcast the session to an appropriate subclass.
 * </p>
 * <p/>
 * <h3>Thread Safety</h3>
 * <p/>
 * {@link IoSession} is thread-safe. But please note that performing more than one {@link #write(Object)} calls at the
 * same time will cause the {@link IoFilter#filterWrite(IoFilter.NextFilter,IoSession,WriteRequest)} to be executed
 * simultaneously, and therefore you have to make sure the {@link IoFilter} implementations you're using are
 * thread-safe, too.
 * </p>
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoSession {
    /** The SslHelper instance name, stored in the session's attributes */
    AttributeKey<SslHelper> SSL_HELPER = new AttributeKey<SslHelper>(SslHelper.class, "internal_sslHelper");

    /**
     * The unique identifier of this session.
     * 
     * @return the session's unique identifier
     */
    long getId();

    /* ADDRESSES */

    /**
     * Returns the socket address of remote peer.
     * 
     * @return the remote socket address
     */
    SocketAddress getRemoteAddress();

    /**
     * Gets the local address of the local peer.
     * 
     * @return the socket address of local machine which is associated with this session.
     */
    SocketAddress getLocalAddress();

    /**
     * Gets the service this session is attached to.
     * 
     * @return the {@link IoService} which provides {@link IoSession} to this session.
     */
    IoService getService();

    /* READ / WRITE / CLOSE */

    /**
     * Tells if the session is currently closed.
     * 
     * @return <code>true</code> if this session is disconnected with remote peer.
     */
    boolean isClosed();

    /**
     * Tells if the session is being closed.
     * 
     * @return <code>true</code> if this session is in the process of being closed.
     */
    boolean isClosing();

    /**
     * Tells if the session is currently connected and able to process incoming requests and to send outgoing responses.
     * 
     * @return <code>true</code> if this session is connected with remote peer.
     */
    boolean isConnected();

    /**
     * Tells if the session is created.
     * 
     * @return <code>true</code> if this session is created.
     */
    boolean isCreated();

    /**
     * Tells if the session is processing a SSL/TLS handshake.
     * 
     * @return <code>true</tt> if and only if this session is processing a SSL/TLS handshake.
     */
    boolean isSecuring();

    /**
     * Tells if the session is belonging to a secured connection.
     * 
     * @return <code>true</tt> if and only if this session is belonging a secured connection.
     */
    boolean isSecured();

    /**
     * Changes the session's state from the current state to a new state. Not all the transition are allowed. Here is
     * the list of all the possible transitions :<br/>
     * <ul>
     * <li>CREATED -> CONNECTED</li>
     * <li>CREATED -> SECURING</li>
     * <li>CREATED -> CLOSING</li>
     * <li>CONNECTED -> SECURING</li>
     * <li>CONNECTED -> CLOSING</li>
     * <li>SECURING -> SECURED</li>
     * <li>SECURING -> CLOSING</li>
     * <li>SECURED -> CONNECTED</li>
     * <li>SECURED -> SECURING</li>
     * <li>SECURED -> CLOSING</li>
     * <li>CLOSING -> CLOSED</li>
     * </ul>
     * 
     * @param newState The final SessionState
     */
    void changeState(SessionState newState);

    /**
     * Initializes the SSL/TLS environment for this session.
     * 
     * @param sslContext The SLLCOntext instance to use.
     */
    void initSecure(SSLContext sslContext);

    /**
     * Tells if the session is using SSL/TLS.
     * 
     * @return <code>true</tt> if and only if this session is exchanging data over a SSL/TLS connection
     */
    boolean isConnectedSecured();

    /**
     * Closes this session immediately or after all queued write requests are flushed. This operation is asynchronous.
     * Wait for the returned {@link IoFuture} if you want to wait for the session actually closed. Once this method has
     * been called, no incoming request will be accepted.
     * 
     * @param immediately {@code true} to close this session immediately. {@code false} to close this session after all
     *        queued write requests are flushed.
     * @return A {@link IoFuture} that will contains the session's state
     */
    IoFuture<Void> close(boolean immediately);

    /* READ/WRITE PAUSE MANAGEMENT */
    /**
     * Suspends read operations for this session.
     */
    void suspendRead();

    /**
     * Suspends write operations for this session.
     */
    void suspendWrite();

    /**
     * Resumes read operations for this session.
     */
    void resumeRead();

    /**
     * Resumes write operations for this session.
     */
    void resumeWrite();

    /**
     * Is read operation is suspended for this session.
     * 
     * @return <code>true</code> if suspended
     */
    boolean isReadSuspended();

    /**
     * Is write operation is suspended for this session.
     * 
     * @return <code>true</code> if suspended
     */
    boolean isWriteSuspended();

    /* BASIC STATS */
    /**
     * Gets the total number of bytes read for this session since it was created.
     * 
     * Returns the total number of bytes which were read from this session.
     */
    long getReadBytes();

    /**
     * Gets the total number of bytes written for this session since it was created.
     * 
     * @return the total number of bytes which were written to this session.
     */
    long getWrittenBytes();

    /* IDLE management */
    /**
     * Gets the session configuration, it where the idle timeout are set and other transport specific configuration.
     * 
     * @return the session's configuration
     */
    IoSessionConfig getConfig();

    /**
     * The session's creation time.
     * 
     * @return the session's creation time in milliseconds
     */
    long getCreationTime();

    /**
     * Returns the time in millisecond when I/O occurred lastly (either read or write).
     * 
     * @return the time of the last read or write done for this session
     */
    long getLastIoTime();

    /**
     * Returns the time in millisecond when the last I/O read occurred.
     * 
     * Returns the time in millisecond when read operation occurred lastly.
     */
    long getLastReadTime();

    /**
     * Returns the time in millisecond when the last I/O write occurred.
     * 
     * Returns the time in millisecond when write operation occurred lastly.
     */
    long getLastWriteTime();

    /* Session context management */

    /**
     * Returns the value of the user-defined attribute for the given <code>key</code>.If the there is no attribute with
     * the specified key the <tt>defaultValue</tt> will be returned.
     * 
     * @param key the attribute's key, must not be <code>null</code>
     * @return <tt>defaultValue</tt> if there is no attribute with the specified key
     * @exception IllegalArgumentException if <code>key==null</code>
     * @see #setAttribute(AttributeKey, Object)
     */
    <T> T getAttribute(AttributeKey<T> key, T defaultValue);

    /**
     * Returns the value of the user-defined attribute for the given <code>key</code>.If the there is no attribute with
     * the specified key <code>null</code> will be returned.
     * 
     * @param key the attribute's key, must not be <code>null</code>
     * @return <code>null</code> if there is no attribute with the specified key
     * @exception IllegalArgumentException if <code>key==null</code>
     * @see #setAttribute(AttributeKey, Object)
     */
    <T> T getAttribute(AttributeKey<T> key);

    /**
     * Sets a user-defined attribute. If the <code>value</code> is <code>null</code> the attribute will be removed from
     * this {@link IoSession}.
     * 
     * @param key the attribute's key, must not be <code>null</code>
     * @param value the attribute's value, <code>null</code> to remove the attribute
     * @return the old attribute's value or <code>null</code> if there is no previous value
     * @exception IllegalArgumentException <ul>
     *            <li>if <code>key==null</code>
     *            <li>if <code>value</code> is not <code>null</code> and not an instance of type that is specified in by
     *            the given <code>key</code> (see {@link AttributeKey#getType()})
     * 
     *            </ul>
     * 
     * @see #getAttribute(AttributeKey)
     */
    <T> T setAttribute(AttributeKey<? extends T> key, T value);

    /**
     * Returns an unmodifiable {@link Set} of all Keys of this {@link IoSession}. If this {@link IoSession} contains no
     * attributes an empty {@link Set} will be returned.
     * 
     * @return all Keys, never <code>null</code>
     * @see Collections#unmodifiableSet(Set)
     */
    Set<AttributeKey<?>> getAttributeKeys();

    /**
     * Removes the specified Attribute from this container. The old value will be returned, <code>null</code> will be
     * rutrnen if there is no such attribute in this container.<br>
     * <br>
     * This method is equivalent to <code>setAttribute(key,null)</code>.
     * 
     * @param key of the attribute to be removed,must not be <code>null</code>
     * @return the removed value, <code>null</code> if this container doesn't contain the specified attribute
     * @exception IllegalArgumentException if <code>key==null</code>
     */
    <T> T removeAttribute(AttributeKey<T> key);

    /**
     * State of a {@link IoSession}
     * 
     * @author <a href="http://mina.apache.org">Apache MINA Project</a>
     * 
     */
    enum SessionState {
        CREATED, CONNECTED, CLOSING, CLOSED, SECURING, SECURED
    }

    /* SESSION WRITING */
    /**
     * Enqueue a message for writing. This method wont block ! The message will by asynchronously processed by the
     * filter chain and wrote to socket by the {@link SelectorLoop}
     * 
     */
    void write(Object message);

    /**
     * Same as {@link IoSession#write(Object)}, but provide a {@link IoFuture} for tracking the completion of this
     * write.
     * 
     * @param message the message to be processed and written
     * @return the {@link IoFuture} for tracking this asynchronous operation
     */
    IoFuture<Void> writeWithFuture(Object message);

    /**
     * Internal method for enqueue write request after filter chain processing
     * 
     * @param writeRequest the message to put in the write request
     * @return the created write request
     */
    WriteRequest enqueueWriteRequest(WriteRequest writeRequest);

}