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
package org.apache.mina.core.service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.mina.core.IoUtil;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChainBuilder;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.session.IoSessionDataStructureFactory;

/**
 * Base interface for all {@link IoAcceptor}s and {@link IoConnector}s
 * that provide I/O service and manage {@link IoSession}s.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoService {
    /**
     * Returns the {@link TransportMetadata} that this service runs on.
     */
    TransportMetadata getTransportMetadata();

    /**
     * Adds an {@link IoServiceListener} that listens any events related with
     * this service.
     */
    void addListener(IoServiceListener listener);

    /**
     * Removed an existing {@link IoServiceListener} that listens any events
     * related with this service.
     */
    void removeListener(IoServiceListener listener);

    /**
     * Returns <tt>true</tt> if and if only {@link #dispose()} method has
     * been called.  Please note that this method will return <tt>true</tt>
     * even after all the related resources are released.
     */
    boolean isDisposing();

    /**
     * Returns <tt>true</tt> if and if only all resources of this processor
     * have been disposed.
     */
    boolean isDisposed();

    /**
     * Releases any resources allocated by this service.  Please note that
     * this method might block as long as there are any sessions managed by
     * this service.
     */
    void dispose();

    /**
     * Returns the handler which will handle all connections managed by this service.
     */
    IoHandler getHandler();

    /**
     * Sets the handler which will handle all connections managed by this service.
     */
    void setHandler(IoHandler handler);

    /**
     * Returns the map of all sessions which are currently managed by this
     * service.  The key of map is the {@link IoSession#getId() ID} of the
     * session.
     *
     * @return the sessions. An empty collection if there's no session.
     */
    Map<Long, IoSession> getManagedSessions();

    /**
     * Returns the number of all sessions which are currently managed by this
     * service.
     */
    int getManagedSessionCount();

    /**
     * Returns the maximum number of sessions which were being managed at the
     * same time.
     */
    int getLargestManagedSessionCount();

    /**
     * Returns the cumulative number of sessions which were managed (or are
     * being managed) by this service, which means 'currently managed session
     * count + closed session count'.
     */
    long getCumulativeManagedSessionCount();

    /**
     * Returns the default configuration of the new {@link IoSession}s
     * created by this service.
     */
    IoSessionConfig getSessionConfig();

    /**
     * Returns the {@link IoFilterChainBuilder} which will build the
     * {@link IoFilterChain} of all {@link IoSession}s which is created
     * by this service.
     * The default value is an empty {@link DefaultIoFilterChainBuilder}.
     */
    IoFilterChainBuilder getFilterChainBuilder();

    /**
     * Sets the {@link IoFilterChainBuilder} which will build the
     * {@link IoFilterChain} of all {@link IoSession}s which is created
     * by this service.
     * If you specify <tt>null</tt> this property will be set to
     * an empty {@link DefaultIoFilterChainBuilder}.
     */
    void setFilterChainBuilder(IoFilterChainBuilder builder);

    /**
     * A shortcut for <tt>( ( DefaultIoFilterChainBuilder ) </tt>{@link #getFilterChainBuilder()}<tt> )</tt>.
     * Please note that the returned object is not a <b>real</b> {@link IoFilterChain}
     * but a {@link DefaultIoFilterChainBuilder}.  Modifying the returned builder
     * won't affect the existing {@link IoSession}s at all, because
     * {@link IoFilterChainBuilder}s affect only newly created {@link IoSession}s.
     *
     * @throws IllegalStateException if the current {@link IoFilterChainBuilder} is
     *                               not a {@link DefaultIoFilterChainBuilder}
     */
    DefaultIoFilterChainBuilder getFilterChain();

    /**
     * Returns a value of whether or not this service is active
     *
     * @return
     * 	whether of not the service is active.
     */
    boolean isActive();

    /**
     * Returns the time when this service was activated.  It returns the last
     * time when this service was activated if the service is not active now.
     *
     * @return
     * 	The time by using {@link System#currentTimeMillis()}
     */
    long getActivationTime();

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
     * Returns <code>true</code> if this service is idle for the specified
     * {@link IdleStatus}.
     */
    boolean isIdle(IdleStatus status);

    /**
     * Returns <code>true</code> if this service is {@link IdleStatus#READER_IDLE}.
     * @see #isIdle(IdleStatus)
     */
    boolean isReaderIdle();

    /**
     * Returns <code>true</code> if this service is {@link IdleStatus#WRITER_IDLE}.
     * @see #isIdle(IdleStatus)
     */
    boolean isWriterIdle();

    /**
     * Returns <code>true</code> if this service is {@link IdleStatus#BOTH_IDLE}.
     * @see #isIdle(IdleStatus)
     */
    boolean isBothIdle();

    /**
     * Returns the number of the fired continuous <tt>serviceIdle</tt> events
     * for the specified {@link IdleStatus}.
     * <p/>
     * If <tt>serviceIdle</tt> event is fired first after some time after I/O,
     * <tt>idleCount</tt> becomes <tt>1</tt>.  <tt>idleCount</tt> resets to
     * <tt>0</tt> if any I/O occurs again, otherwise it increases to
     * <tt>2</tt> and so on if <tt>serviceIdle</tt> event is fired again without
     * any I/O between two (or more) <tt>serviceIdle</tt> events.
     */
    int getIdleCount(IdleStatus status);

    /**
     * Returns the number of the fired continuous <tt>serviceIdle</tt> events
     * for {@link IdleStatus#READER_IDLE}.
     * @see #getIdleCount(IdleStatus)
     */
    int getReaderIdleCount();

    /**
     * Returns the number of the fired continuous <tt>serviceIdle</tt> events
     * for {@link IdleStatus#WRITER_IDLE}.
     * @see #getIdleCount(IdleStatus)
     */
    int getWriterIdleCount();

    /**
     * Returns the number of the fired continuous <tt>serviceIdle</tt> events
     * for {@link IdleStatus#BOTH_IDLE}.
     * @see #getIdleCount(IdleStatus)
     */
    int getBothIdleCount();

    /**
     * Returns the time in milliseconds when the last <tt>serviceIdle</tt> event
     * is fired for the specified {@link IdleStatus}.
     */
    long getLastIdleTime(IdleStatus status);

    /**
     * Returns the time in milliseconds when the last <tt>serviceIdle</tt> event
     * is fired for {@link IdleStatus#READER_IDLE}.
     * @see #getLastIdleTime(IdleStatus)
     */
    long getLastReaderIdleTime();

    /**
     * Returns the time in milliseconds when the last <tt>serviceIdle</tt> event
     * is fired for {@link IdleStatus#WRITER_IDLE}.
     * @see #getLastIdleTime(IdleStatus)
     */
    long getLastWriterIdleTime();

    /**
     * Returns the time in milliseconds when the last <tt>serviceIdle</tt> event
     * is fired for {@link IdleStatus#BOTH_IDLE}.
     * @see #getLastIdleTime(IdleStatus)
     */
    long getLastBothIdleTime();

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
     * Returns idle time for {@link IdleStatus#READER_IDLE} in seconds.
     */
    int getReaderIdleTime();

    /**
     * Returns idle time for {@link IdleStatus#READER_IDLE} in milliseconds.
     */
    long getReaderIdleTimeInMillis();

    /**
     * Sets idle time for {@link IdleStatus#READER_IDLE} in seconds.
     */
    void setReaderIdleTime(int idleTime);

    /**
     * Returns idle time for {@link IdleStatus#WRITER_IDLE} in seconds.
     */
    int getWriterIdleTime();

    /**
     * Returns idle time for {@link IdleStatus#WRITER_IDLE} in milliseconds.
     */
    long getWriterIdleTimeInMillis();

    /**
     * Sets idle time for {@link IdleStatus#WRITER_IDLE} in seconds.
     */
    void setWriterIdleTime(int idleTime);

    /**
     * Returns idle time for {@link IdleStatus#BOTH_IDLE} in seconds.
     */
    int getBothIdleTime();

    /**
     * Returns idle time for {@link IdleStatus#BOTH_IDLE} in milliseconds.
     */
    long getBothIdleTimeInMillis();

    /**
     * Sets idle time for {@link IdleStatus#WRITER_IDLE} in seconds.
     */
    void setBothIdleTime(int idleTime);

    /**
     * Returns the number of bytes read by this service
     *
     * @return
     * 	The number of bytes this service has read
     */
    long getReadBytes();

    /**
     * Returns the number of bytes written out by this service
     *
     * @return
     * 	The number of bytes this service has written
     */
    long getWrittenBytes();

    /**
     * Returns the number of messages this services has read
     *
     * @return
     * 	The number of messages this services has read
     */
    long getReadMessages();

    /**
     * Returns the number of messages this service has written
     *
     * @return
     * 	The number of messages this service has written
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
     * Returns the maximum of the {@link #getReadBytesThroughput() readBytesThroughput}.
     */
    double getLargestReadBytesThroughput();

    /**
     * Returns the maximum of the {@link #getWrittenBytesThroughput() writtenBytesThroughput}.
     */
    double getLargestWrittenBytesThroughput();

    /**
     * Returns the maximum of the {@link #getReadMessagesThroughput() readMessagesThroughput}.
     */
    double getLargestReadMessagesThroughput();

    /**
     * Returns the maximum of the {@link #getWrittenMessagesThroughput() writtenMessagesThroughput}.
     */
    double getLargestWrittenMessagesThroughput();

    /**
     * Returns the interval (seconds) between each throughput calculation.
     * The default value is <tt>3</tt> seconds.
     */
    int getThroughputCalculationInterval();

    /**
     * Returns the interval (milliseconds) between each throughput calculation.
     * The default value is <tt>3</tt> seconds.
     */
    long getThroughputCalculationIntervalInMillis();

    /**
     * Sets the interval (seconds) between each throughput calculation.  The
     * default value is <tt>3</tt> seconds.
     */
    void setThroughputCalculationInterval(int throughputCalculationInterval);

    /**
     * Returns the number of bytes scheduled to be written
     *
     * @return
     * 	The number of bytes scheduled to be written
     */
    int getScheduledWriteBytes();

    /**
     * Returns the number of messages scheduled to be written
     *
     * @return
     * 	The number of messages scheduled to be written
     */
    int getScheduledWriteMessages();

    /**
     * Writes the specified {@code message} to all the {@link IoSession}s
     * managed by this service.  This method is a convenience shortcut for
     * {@link IoUtil#broadcast(Object, Collection)}.
     */
    Set<WriteFuture> broadcast(Object message);

    /**
     * Returns the {@link IoSessionDataStructureFactory} that provides
     * related data structures for a new session created by this service.
     */
    IoSessionDataStructureFactory getSessionDataStructureFactory();

    /**
     * Sets the {@link IoSessionDataStructureFactory} that provides
     * related data structures for a new session created by this service.
     */
    void setSessionDataStructureFactory(IoSessionDataStructureFactory sessionDataStructureFactory);
}
