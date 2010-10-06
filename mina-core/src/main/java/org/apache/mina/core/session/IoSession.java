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
package org.apache.mina.core.session;

import java.net.SocketAddress;
import java.util.Set;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;

/**
 * A handle which represents connection between two end-points regardless of
 * transport types.
 * <p/>
 * {@link IoSession} provides user-defined attributes.  User-defined attributes
 * are application-specific data which are associated with a session.
 * It often contains objects that represents the state of a higher-level protocol
 * and becomes a way to exchange data between filters and handlers.
 * <p/>
 * <h3>Adjusting Transport Type Specific Properties</h3>
 * <p/>
 * You can simply downcast the session to an appropriate subclass.
 * </p>
 * <p/>
 * <h3>Thread Safety</h3>
 * <p/>
 * {@link IoSession} is thread-safe.  But please note that performing
 * more than one {@link #write(Object)} calls at the same time will
 * cause the {@link IoFilter#filterWrite(IoFilter.NextFilter,IoSession,WriteRequest)}
 * to be executed simultaneously, and therefore you have to make sure the
 * {@link IoFilter} implementations you're using are thread-safe, too.
 * </p>
 * <p/>
 * <h3>Equality of Sessions</h3>
 * TODO : The getId() method is totally wrong. We can't base
 * a method which is designed to create a unique ID on the hashCode method.
 * {@link #equals(Object)} and {@link #hashCode()} shall not be overriden
 * to the default behavior that is defined in {@link Object}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoSession {
    /**
     * @return a unique identifier for this session.  Every session has its own
     * ID which is different from each other.
     * 
     * TODO : The way it's implemented does not guarantee that the contract is
     * respected. It uses the HashCode() method which don't guarantee the key
     * unicity.
     */
    long getId();

    /**
     * @return the {@link IoService} which provides I/O service to this session.
     */
    IoService getService();

    /**
     * @return the {@link IoHandler} which handles this session.
     */
    IoHandler getHandler();

    /**
     * @return the configuration of this session.
     */
    IoSessionConfig getConfig();

    /**
     * @return the filter chain that only affects this session.
     */
    IoFilterChain getFilterChain();

    
    /**
     * TODO Add method documentation
     */
    WriteRequestQueue getWriteRequestQueue();

    /**
     * @return the {@link TransportMetadata} that this session runs on.
     */
    TransportMetadata getTransportMetadata();

    /**
     * TODO This javadoc is wrong. The return tag should be short.
     * 
     * @return a {@link ReadFuture} which is notified when a new message is
     * received, the connection is closed or an exception is caught.  This
     * operation is especially useful when you implement a client application.
     * TODO : Describe here how we enable this feature.
     * However, please note that this operation is disabled by default and
     * throw {@link IllegalStateException} because all received events must be
     * queued somewhere to support this operation, possibly leading to memory
     * leak.  This means you have to keep calling {@link #read()} once you
     * enabled this operation.  To enable this operation, please call
     * {@link IoSessionConfig#setUseReadOperation(boolean)} with <tt>true</tt>.
     *
     * @throws IllegalStateException if
     * {@link IoSessionConfig#setUseReadOperation(boolean) useReadOperation}
     * option has not been enabled.
     */
    ReadFuture read();

    /**
     * Writes the specified <code>message</code> to remote peer.  This
     * operation is asynchronous; {@link IoHandler#messageSent(IoSession,Object)}
     * will be invoked when the message is actually sent to remote peer.
     * You can also wait for the returned {@link WriteFuture} if you want
     * to wait for the message actually written.
     */
    WriteFuture write(Object message);

    /**
     * (Optional) Writes the specified <tt>message</tt> to the specified <tt>destination</tt>.
     * This operation is asynchronous; {@link IoHandler#messageSent(IoSession, Object)}
     * will be invoked when the message is actually sent to remote peer. You can
     * also wait for the returned {@link WriteFuture} if you want to wait for
     * the message actually written.
     * <p>
     * When you implement a client that receives a broadcast message from a server
     * such as DHCP server, the client might need to send a response message for the
     * broadcast message the server sent.  Because the remote address of the session
     * is not the address of the server in case of broadcasting, there should be a
     * way to specify the destination when you write the response message.
     * This interface provides {@link #write(Object, SocketAddress)} method so you
     * can specify the destination.
     *
     * @param destination <tt>null</tt> if you want the message sent to the
     *                    default remote address
     *
     * @throws UnsupportedOperationException if this operation is not supported
     */
    WriteFuture write(Object message, SocketAddress destination);

    /**
     * Closes this session immediately or after all queued write requests
     * are flushed.  This operation is asynchronous.  Wait for the returned
     * {@link CloseFuture} if you want to wait for the session actually closed.
     *
     * @param immediately {@code true} to close this session immediately
     *                    (i.e. {@link #close()}). The pending write requests
     *                    will simply be discarded.
     *                    {@code false} to close this session after all queued
     *                    write requests are flushed (i.e. {@link #closeOnFlush()}).
     */
    CloseFuture close(boolean immediately);
    
    /**
     * Closes this session after all queued write requests
     * are flushed. This operation is asynchronous.  Wait for the returned
     * {@link CloseFuture} if you want to wait for the session actually closed.
     * @deprecated use {@link IoSession#close(boolean)}
     */
    @Deprecated CloseFuture close();

    /**
     * Returns an attachment of this session.
     * This method is identical with <tt>getAttribute( "" )</tt>.
     *
     * @deprecated Use {@link #getAttribute(Object)} instead.
     */
    @Deprecated Object getAttachment();

    /**
     * Sets an attachment of this session.
     * This method is identical with <tt>setAttribute( "", attachment )</tt>.
     *
     * @return Old attachment.  <tt>null</tt> if it is new.
     * @deprecated Use {@link #setAttribute(Object, Object)} instead.
     */
    @Deprecated Object setAttachment(Object attachment);

    /**
     * Returns the value of the user-defined attribute of this session.
     *
     * @param key the key of the attribute
     * @return <tt>null</tt> if there is no attribute with the specified key
     */
    Object getAttribute(Object key);

    /**
     * Returns the value of user defined attribute associated with the
     * specified key.  If there's no such attribute, the specified default
     * value is associated with the specified key, and the default value is
     * returned.  This method is same with the following code except that the
     * operation is performed atomically.
     * <pre>
     * if (containsAttribute(key)) {
     *     return getAttribute(key);
     * } else {
     *     setAttribute(key, defaultValue);
     *     return defaultValue;
     * }
     * </pre>
     */
    Object getAttribute(Object key, Object defaultValue);

    /**
     * Sets a user-defined attribute.
     *
     * @param key   the key of the attribute
     * @param value the value of the attribute
     * @return The old value of the attribute.  <tt>null</tt> if it is new.
     */
    Object setAttribute(Object key, Object value);

    /**
     * Sets a user defined attribute without a value.  This is useful when
     * you just want to put a 'mark' attribute.  Its value is set to
     * {@link Boolean#TRUE}.
     *
     * @param key the key of the attribute
     * @return The old value of the attribute.  <tt>null</tt> if it is new.
     */
    Object setAttribute(Object key);

    /**
     * Sets a user defined attribute if the attribute with the specified key
     * is not set yet.  This method is same with the following code except
     * that the operation is performed atomically.
     * <pre>
     * if (containsAttribute(key)) {
     *     return getAttribute(key);
     * } else {
     *     return setAttribute(key, value);
     * }
     * </pre>
     */
    Object setAttributeIfAbsent(Object key, Object value);

    /**
     * Sets a user defined attribute without a value if the attribute with
     * the specified key is not set yet.  This is useful when you just want to
     * put a 'mark' attribute.  Its value is set to {@link Boolean#TRUE}.
     * This method is same with the following code except that the operation
     * is performed atomically.
     * <pre>
     * if (containsAttribute(key)) {
     *     return getAttribute(key);  // might not always be Boolean.TRUE.
     * } else {
     *     return setAttribute(key);
     * }
     * </pre>
     */
    Object setAttributeIfAbsent(Object key);

    /**
     * Removes a user-defined attribute with the specified key.
     *
     * @return The old value of the attribute.  <tt>null</tt> if not found.
     */
    Object removeAttribute(Object key);

    /**
     * Removes a user defined attribute with the specified key if the current
     * attribute value is equal to the specified value.  This method is same
     * with the following code except that the operation is performed
     * atomically.
     * <pre>
     * if (containsAttribute(key) && getAttribute(key).equals(value)) {
     *     removeAttribute(key);
     *     return true;
     * } else {
     *     return false;
     * }
     * </pre>
     */
    boolean removeAttribute(Object key, Object value);

    /**
     * Replaces a user defined attribute with the specified key if the
     * value of the attribute is equals to the specified old value.
     * This method is same with the following code except that the operation
     * is performed atomically.
     * <pre>
     * if (containsAttribute(key) && getAttribute(key).equals(oldValue)) {
     *     setAttribute(key, newValue);
     *     return true;
     * } else {
     *     return false;
     * }
     * </pre>
     */
    boolean replaceAttribute(Object key, Object oldValue, Object newValue);

    /**
     * Returns <tt>true</tt> if this session contains the attribute with
     * the specified <tt>key</tt>.
     */
    boolean containsAttribute(Object key);

    /**
     * Returns the set of keys of all user-defined attributes.
     */
    Set<Object> getAttributeKeys();

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
     * {@link IoAcceptor#bind()}.  If this session is managed by
     * {@link IoConnector}, this method returns the same address with
     * that of {@link #getRemoteAddress()}.
     */
    SocketAddress getServiceAddress();

    /**
     * 
     * TODO setWriteRequestQueue.
     *
     * @param writeRequestQueue
     */
    void setCurrentWriteRequest(WriteRequest currentWriteRequest);
    
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
     * @return <code>true</code> if suspended
     */
    boolean isReadSuspended();
    
    /**
     * Is write operation is suspended for this session.
     * @return <code>true</code> if suspended
     */
    boolean isWriteSuspended();
    
    /**
     * Update all statistical properties related with throughput assuming
     * the specified time is the current time.  By default this method returns
     * silently without updating the throughput properties if they were
     * calculated already within last
     * {@link IoSessionConfig#getThroughputCalculationInterval() calculation interval}.
     * If, however, <tt>force</tt> is specified as <tt>true</tt>, this method
     * updates the throughput properties immediately.

     * @param currentTime the current time in milliseconds
     */
    void updateThroughput(long currentTime, boolean force);
    
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
     * Returns the number of read bytes per second.
     */
    double getReadBytesThroughput();

    /**
     * Returns the number of written bytes per second.
     */
    double getWrittenBytesThroughput();

    /**
     * Returns the number of read messages per second.
     */
    double getReadMessagesThroughput();

    /**
     * Returns the number of written messages per second.
     */
    double getWrittenMessagesThroughput();

    /**
     * Returns the number of messages which are scheduled to be written to this session.
     */
    int getScheduledWriteMessages();

    /**
     * Returns the number of bytes which are scheduled to be written to this
     * session.
     */
    long getScheduledWriteBytes();

    /**
     * Returns the message which is being written by {@link IoService}.
     * @return <tt>null</tt> if and if only no message is being written
     */
    Object getCurrentWriteMessage();

    /**
     * Returns the {@link WriteRequest} which is being processed by
     * {@link IoService}.
     *
     * @return <tt>null</tt> if and if only no message is being written
     */
    WriteRequest getCurrentWriteRequest();

    /**
     * @return the session's creation time in milliseconds
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
     * Returns <code>true</code> if this session is {@link IdleStatus#READER_IDLE}.
     * @see #isIdle(IdleStatus)
     */
    boolean isReaderIdle();

    /**
     * Returns <code>true</code> if this session is {@link IdleStatus#WRITER_IDLE}.
     * @see #isIdle(IdleStatus)
     */
    boolean isWriterIdle();

    /**
     * Returns <code>true</code> if this session is {@link IdleStatus#BOTH_IDLE}.
     * @see #isIdle(IdleStatus)
     */
    boolean isBothIdle();

    /**
     * Returns the number of the fired continuous <tt>sessionIdle</tt> events
     * for the specified {@link IdleStatus}.
     * <p/>
     * If <tt>sessionIdle</tt> event is fired first after some time after I/O,
     * <tt>idleCount</tt> becomes <tt>1</tt>.  <tt>idleCount</tt> resets to
     * <tt>0</tt> if any I/O occurs again, otherwise it increases to
     * <tt>2</tt> and so on if <tt>sessionIdle</tt> event is fired again without
     * any I/O between two (or more) <tt>sessionIdle</tt> events.
     */
    int getIdleCount(IdleStatus status);

    /**
     * Returns the number of the fired continuous <tt>sessionIdle</tt> events
     * for {@link IdleStatus#READER_IDLE}.
     * @see #getIdleCount(IdleStatus)
     */
    int getReaderIdleCount();

    /**
     * Returns the number of the fired continuous <tt>sessionIdle</tt> events
     * for {@link IdleStatus#WRITER_IDLE}.
     * @see #getIdleCount(IdleStatus)
     */
    int getWriterIdleCount();

    /**
     * Returns the number of the fired continuous <tt>sessionIdle</tt> events
     * for {@link IdleStatus#BOTH_IDLE}.
     * @see #getIdleCount(IdleStatus)
     */
    int getBothIdleCount();

    /**
     * Returns the time in milliseconds when the last <tt>sessionIdle</tt> event
     * is fired for the specified {@link IdleStatus}.
     */
    long getLastIdleTime(IdleStatus status);


    /**
     * Returns the time in milliseconds when the last <tt>sessionIdle</tt> event
     * is fired for {@link IdleStatus#READER_IDLE}.
     * @see #getLastIdleTime(IdleStatus)
     */
    long getLastReaderIdleTime();

    /**
     * Returns the time in milliseconds when the last <tt>sessionIdle</tt> event
     * is fired for {@link IdleStatus#WRITER_IDLE}.
     * @see #getLastIdleTime(IdleStatus)
     */
    long getLastWriterIdleTime();

    /**
     * Returns the time in milliseconds when the last <tt>sessionIdle</tt> event
     * is fired for {@link IdleStatus#BOTH_IDLE}.
     * @see #getLastIdleTime(IdleStatus)
     */
    long getLastBothIdleTime();
}
