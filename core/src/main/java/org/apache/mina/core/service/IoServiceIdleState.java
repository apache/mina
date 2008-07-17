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

import org.apache.mina.core.session.IdleStatus;

/**
 * Gives access to the idle state information for an IoService.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @since 2.0-M3
 */
public interface IoServiceIdleState {

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
}