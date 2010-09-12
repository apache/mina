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

import java.util.Map;
import java.util.Set;

import org.apache.mina.core.IoUtil;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChainBuilder;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.session.IoSessionDataStructureFactory;

/**
 * Base interface for all {@link IoAcceptor}s and {@link IoConnector}s
 * that provide I/O service and manage {@link IoSession}s.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
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
   * Releases any resources allocated by this service.  Please note that
   * this method might block as long as there are any sessions managed by this service.
   *
   * Warning : calling this method from a IoFutureListener with <code>awaitTermination</code> = true
   * will probably lead to a deadlock.
   *
   * @param awaitTermination When true this method will block until the underlying ExecutorService is terminated
   */
    void dispose(boolean awaitTermination);

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
     * @return whether of not the service is active.
     */
    boolean isActive();

    /**
     * Returns the time when this service was activated.  It returns the last
     * time when this service was activated if the service is not active now.
     *
     * @return The time by using {@link System#currentTimeMillis()}
     */
    long getActivationTime();

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

    /**
     * Returns the number of bytes scheduled to be written
     *
     * @return The number of bytes scheduled to be written
     */
    int getScheduledWriteBytes();

    /**
     * Returns the number of messages scheduled to be written
     *
     * @return The number of messages scheduled to be written
     */
    int getScheduledWriteMessages();

    /**
     * Returns the IoServiceStatistics object for this service.
     * 
     * @return The statistics object for this service.
     */
    IoServiceStatistics getStatistics();
}
