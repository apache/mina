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

/**
 * Provides usage statistics for an IoService.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public interface IoServiceStatistics {
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
     * Returns the number of bytes read by this service
     *
     * @return
     *     The number of bytes this service has read
     */
    long getReadBytes();

    /**
     * Returns the number of bytes written out by this service
     *
     * @return
     *     The number of bytes this service has written
     */
    long getWrittenBytes();

    /**
     * Returns the number of messages this services has read
     *
     * @return
     *     The number of messages this services has read
     */
    long getReadMessages();

    /**
     * Returns the number of messages this service has written
     *
     * @return
     *     The number of messages this service has written
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
}
