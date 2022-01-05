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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides usage statistics for an {@link AbstractIoService} instance.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since 2.0.0-M3
 */
public class IoServiceStatistics {

    private final IoService service;

    /** The number of bytes read per second */
    private double readBytesThroughput;

    /** The number of bytes written per second */
    private double writtenBytesThroughput;

    /** The number of messages read per second */
    private double readMessagesThroughput;

    /** The number of messages written per second */
    private double writtenMessagesThroughput;

    /** The biggest number of bytes read per second */
    private double largestReadBytesThroughput;

    /** The biggest number of bytes written per second */
    private double largestWrittenBytesThroughput;

    /** The biggest number of messages read per second */
    private double largestReadMessagesThroughput;

    /** The biggest number of messages written per second */
    private double largestWrittenMessagesThroughput;

    /** The number of read bytes since the service has been started */
    private long readBytes;

    /** The number of written bytes since the service has been started */
    private long writtenBytes;

    /** The number of read messages since the service has been started */
    private long readMessages;

    /** The number of written messages since the service has been started */
    private long writtenMessages;

    /** The time the last read operation occurred */
    private long lastReadTime;

    /** The time the last write operation occurred */
    private long lastWriteTime;

    private long lastReadBytes;

    private long lastWrittenBytes;

    private long lastReadMessages;

    private long lastWrittenMessages;

    private long lastThroughputCalculationTime;

    private int scheduledWriteBytes;

    private int scheduledWriteMessages;

    private final Lock throughputCalculationLock = new ReentrantLock();

    private final Config config = new Config();

    /**
     * Creates a new IoServiceStatistics instance
     * 
     * @param service The {@link IoService} for which we want statistics
     */
    public IoServiceStatistics(IoService service) {
        this.service = service;
    }

    /**
     * @return The maximum number of sessions which were being managed at the
     *         same time.
     */
    public final int getLargestManagedSessionCount() {
        return ((AbstractIoService)service).getListeners().getLargestManagedSessionCount();
    }

    /**
     * @return The cumulative number of sessions which were managed (or are
     *         being managed) by this service, which means 'currently managed
     *         session count + closed session count'.
     */
    public final long getCumulativeManagedSessionCount() {
        return ((AbstractIoService)service).getListeners().getCumulativeManagedSessionCount();
    }

    /**
     * @return the time in millis when the last I/O operation (read or write)
     *         occurred.
     */
    public final long getLastIoTime() {
        if (!config.isStatisticsCalcEnabled) {
            return 0;
        }

        if (!config.isLastReadTimeCalcEnabled || !config.isLastWriteTimeCalcEnabled) {
            return 0;
        }

        throughputCalculationLock.lock();

        try {
            return Math.max(lastReadTime, lastWriteTime);
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The time in millis when the last read operation occurred.
     */
    public final long getLastReadTime() {

        if (!config.isStatisticsCalcEnabled || !config.isLastReadTimeCalcEnabled) {
            return 0;
        }

        throughputCalculationLock.lock();

        try {
            return lastReadTime;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The time in millis when the last write operation occurred.
     */
    public final long getLastWriteTime() {
        if (!config.isStatisticsCalcEnabled || !config.isLastWriteTimeCalcEnabled) {
            return 0;
        }

        throughputCalculationLock.lock();

        try {
            return lastWriteTime;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The number of bytes this service has read so far
     */
    public final long getReadBytes() {
        if (!config.isStatisticsCalcEnabled || !config.isReadBytesCalcEnabled) {
            return 0;
        }

        throughputCalculationLock.lock();

        try {
            return readBytes;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The number of bytes this service has written so far
     */
    public final long getWrittenBytes() {
        if (!config.isStatisticsCalcEnabled || !config.isWrittenBytesCalcEnabled) {
            return 0;
        }

        throughputCalculationLock.lock();

        try {
            return writtenBytes;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The number of messages this services has read so far
     */
    public final long getReadMessages() {
        if (!config.isStatisticsCalcEnabled || !config.isReadMessagesCalcEnabled) {
            return 0;
        }

        throughputCalculationLock.lock();

        try {
            return readMessages;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The number of messages this service has written so far
     */
    public final long getWrittenMessages() {
        if (!config.isStatisticsCalcEnabled || !config.isWrittenMessagesCalcEnabled) {
            return 0;
        }

        throughputCalculationLock.lock();

        try {
            return writtenMessages;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The number of read bytes per second.
     */
    public final double getReadBytesThroughput() {
        if (!config.isStatisticsCalcEnabled || !(config.isReadBytesCalcEnabled)) {
            return 0;
        }

        throughputCalculationLock.lock();

        try {
            resetThroughput();
            return readBytesThroughput;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The number of written bytes per second.
     */
    public final double getWrittenBytesThroughput() {
        if (!config.isStatisticsCalcEnabled || !config.isWrittenBytesCalcEnabled) {
            return 0;
        }

        throughputCalculationLock.lock();

        try {
            resetThroughput();
            return writtenBytesThroughput;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The number of read messages per second.
     */
    public final double getReadMessagesThroughput() {
        if (!config.isStatisticsCalcEnabled || !config.isReadMessagesCalcEnabled) {
            return 0;
        }

        throughputCalculationLock.lock();

        try {
            resetThroughput();
            return readMessagesThroughput;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The number of written messages per second.
     */
    public final double getWrittenMessagesThroughput() {
        if (!config.isStatisticsCalcEnabled || !config.isWrittenMessagesCalcEnabled) {
            return 0;
        }

        throughputCalculationLock.lock();

        try {
            resetThroughput();
            return writtenMessagesThroughput;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The maximum number of bytes read per second since the service has
     *         been started.
     */
    public final double getLargestReadBytesThroughput() {
        if (!config.isStatisticsCalcEnabled || !config.isReadBytesCalcEnabled) {
            return 0;
        }

        throughputCalculationLock.lock();

        try {
            return largestReadBytesThroughput;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The maximum number of bytes written per second since the service
     *         has been started.
     */
    public final double getLargestWrittenBytesThroughput() {
        if (!config.isStatisticsCalcEnabled || !config.isWrittenBytesCalcEnabled) {
            return 0;
        }

        throughputCalculationLock.lock();

        try {
            return largestWrittenBytesThroughput;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The maximum number of messages read per second since the service
     *         has been started.
     */
    public final double getLargestReadMessagesThroughput() {
        if (!config.isStatisticsCalcEnabled || !config.isReadMessagesCalcEnabled) {
            return 0;
        }

        throughputCalculationLock.lock();

        try {
            return largestReadMessagesThroughput;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The maximum number of messages written per second since the
     *         service has been started.
     */
    public final double getLargestWrittenMessagesThroughput() {
        if (!config.isStatisticsCalcEnabled || !config.isWrittenMessagesCalcEnabled) {
            return 0;
        }

        throughputCalculationLock.lock();

        try {
            return largestWrittenMessagesThroughput;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return the interval (seconds) between each throughput calculation. The
     *         default value is <tt>3</tt> seconds.
     */
    public final int getThroughputCalculationInterval() {
        return config.getThroughputCalculationInterval();
    }

    /**
     * @return the interval (milliseconds) between each throughput calculation.
     * The default value is <tt>3</tt> seconds.
     */
    public final long getThroughputCalculationIntervalInMillis() {
        return config.getThroughputCalculationIntervalInMillis();
    }

    /**
     * Sets the interval (seconds) between each throughput calculation.  The
     * default value is <tt>3</tt> seconds.
     * 
     * @param throughputCalculationInterval The interval between two calculation
     */
    public final void setThroughputCalculationInterval(int throughputCalculationInterval) {
        config.setThroughputCalculationInterval(throughputCalculationInterval);
    }

    /**
     * Sets last time at which a read occurred on the service.
     * 
     * @param lastReadTime
     *            The last time a read has occurred
     */
    protected final void setLastReadTime(long lastReadTime) {
        if (!config.isStatisticsCalcEnabled || !config.isLastReadTimeCalcEnabled) {
            return;
        }

        throughputCalculationLock.lock();

        try {
            this.lastReadTime = lastReadTime;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * Sets last time at which a write occurred on the service.
     * 
     * @param lastWriteTime
     *            The last time a write has occurred
     */
    protected final void setLastWriteTime(long lastWriteTime) {
        if (!config.isStatisticsCalcEnabled || !config.isLastWriteTimeCalcEnabled) {
            return;
        }

        throughputCalculationLock.lock();

        try {
            this.lastWriteTime = lastWriteTime;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * Resets the throughput counters of the service if no session is currently
     * managed.
     */
    private void resetThroughput() {
        if (service.getManagedSessionCount() == 0) {
            readBytesThroughput = 0;
            writtenBytesThroughput = 0;
            readMessagesThroughput = 0;
            writtenMessagesThroughput = 0;
        }
    }

    /**
     * Updates the throughput counters.
     * 
     * @param currentTime The current time
     */
    public void updateThroughput(long currentTime) {
        if (!config.isStatisticsCalcEnabled) {
            return;
        }

        long minInterval = config.getThroughputCalculationIntervalInMillis();

        if (minInterval == 0) {
            return;
        }

        throughputCalculationLock.lock();

        try {
            int interval = (int) (currentTime - lastThroughputCalculationTime);

            if (interval < minInterval) {
                return;
            }

            readBytesThroughput = (readBytes - lastReadBytes) * 1000.0 / interval;
            writtenBytesThroughput = (writtenBytes - lastWrittenBytes) * 1000.0 / interval;
            readMessagesThroughput = (readMessages - lastReadMessages) * 1000.0 / interval;
            writtenMessagesThroughput = (writtenMessages - lastWrittenMessages) * 1000.0 / interval;

            if (readBytesThroughput > largestReadBytesThroughput) {
                largestReadBytesThroughput = readBytesThroughput;
            }

            if (writtenBytesThroughput > largestWrittenBytesThroughput) {
                largestWrittenBytesThroughput = writtenBytesThroughput;
            }

            if (readMessagesThroughput > largestReadMessagesThroughput) {
                largestReadMessagesThroughput = readMessagesThroughput;
            }

            if (writtenMessagesThroughput > largestWrittenMessagesThroughput) {
                largestWrittenMessagesThroughput = writtenMessagesThroughput;
            }

            lastReadBytes = readBytes;
            lastWrittenBytes = writtenBytes;
            lastReadMessages = readMessages;
            lastWrittenMessages = writtenMessages;

            lastThroughputCalculationTime = currentTime;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * Increases the count of read bytes by <code>nbBytesRead</code> and sets
     * the last read time to <code>currentTime</code>.
     * 
     * @param nbBytesRead
     *            The number of bytes read
     * @param currentTime
     *            The date those bytes were read
     */
    public final void increaseReadBytes(long nbBytesRead, long currentTime) {
        if (!config.isStatisticsCalcEnabled) {
            return;
        }

        if (!config.isReadBytesCalcEnabled && !config.isLastReadTimeCalcEnabled) {
            return;
        }

        throughputCalculationLock.lock();

        try {
            readBytes += nbBytesRead;
            lastReadTime = currentTime;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * Increases the count of read messages by 1 and sets the last read time to
     * <code>currentTime</code>.
     * 
     * @param currentTime
     *            The time the message has been read
     */
    public final void increaseReadMessages(long currentTime) {
        if (!config.isStatisticsCalcEnabled) {
            return;
        }

        if (!config.isReadMessagesCalcEnabled && !config.isLastReadTimeCalcEnabled) {
            return;
        }

        throughputCalculationLock.lock();

        try {
            readMessages++;
            lastReadTime = currentTime;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * Increases the count of written bytes by <code>nbBytesWritten</code> and
     * sets the last write time to <code>currentTime</code>.
     * 
     * @param nbBytesWritten
     *            The number of bytes written
     * @param currentTime
     *            The date those bytes were written
     */
    public final void increaseWrittenBytes(int nbBytesWritten, long currentTime) {
        if (!config.isStatisticsCalcEnabled) {
            return;
        }

        if (!config.isWrittenBytesCalcEnabled && !config.isLastWriteTimeCalcEnabled) {
            return;
        }

        throughputCalculationLock.lock();

        try {
            writtenBytes += nbBytesWritten;
            lastWriteTime = currentTime;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * Increases the count of written messages by 1 and sets the last write time
     * to <code>currentTime</code>.
     * 
     * @param currentTime
     *            The date the message were written
     */
    public final void increaseWrittenMessages(long currentTime) {
        if (!config.isStatisticsCalcEnabled) {
            return;
        }

        if (!config.isWrittenMessagesCalcEnabled && !config.isLastWriteTimeCalcEnabled) {
            return;
        }

        throughputCalculationLock.lock();

        try {
            writtenMessages++;
            lastWriteTime = currentTime;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The count of bytes scheduled for write.
     */
    public final int getScheduledWriteBytes() {
        if (!config.isStatisticsCalcEnabled || !config.isScheduledWriteBytesCalcEnabled) {
            return 0;
        }

        throughputCalculationLock.lock();

        try {
            return scheduledWriteBytes;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * Increments by <code>increment</code> the count of bytes scheduled for write.
     * 
     * @param increment The number of added bytes fro write
     */
    public final void increaseScheduledWriteBytes(int increment) {
        if (!config.isStatisticsCalcEnabled || !config.isScheduledWriteBytesCalcEnabled) {
            return;
        }

        throughputCalculationLock.lock();

        try {
            scheduledWriteBytes += increment;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return the count of messages scheduled for write.
     */
    public final int getScheduledWriteMessages() {
        if (!config.isStatisticsCalcEnabled || !config.isScheduledWriteMessagesCalcEnabled) {
            return 0;
        }

        throughputCalculationLock.lock();

        try {
            return scheduledWriteMessages;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * Increments the count of messages scheduled for write.
     */
    public final void increaseScheduledWriteMessages() {
        if (!config.isStatisticsCalcEnabled || !config.isScheduledWriteMessagesCalcEnabled) {
            return;
        }

        throughputCalculationLock.lock();

        try {
            scheduledWriteMessages++;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * Decrements the count of messages scheduled for write.
     */
    public final void decreaseScheduledWriteMessages() {
        if (!config.isStatisticsCalcEnabled || !config.isScheduledWriteMessagesCalcEnabled) {
            return;
        }

        throughputCalculationLock.lock();
        try {
            scheduledWriteMessages--;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * Sets the time at which throughput counters where updated.
     * 
     * @param lastThroughputCalculationTime The time at which throughput counters where updated.
     */
    protected void setLastThroughputCalculationTime(long lastThroughputCalculationTime) {
        if (!config.isStatisticsCalcEnabled) {
            return;
        }

        if (config.getThroughputCalculationInterval() == 0) {
            return;
        }

        throughputCalculationLock.lock();

        try {
            this.lastThroughputCalculationTime = lastThroughputCalculationTime;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The configuration of IoServiceStatistics
     */
    public final Config getConfig() {
        return config;
    }

    /**
     * This is a configuration for IoServiceStatistics. It allows configuring which statistics should be calculated.
     * Disabling statistics calculation improves performance as each operation of IoServiceStatistics is blocking.
     */
    public final static class Config {

        private volatile boolean isReadBytesCalcEnabled = true;
        private volatile boolean isWrittenBytesCalcEnabled = true;
        private volatile boolean isReadMessagesCalcEnabled = true;
        private volatile boolean isWrittenMessagesCalcEnabled = true;
        private volatile boolean isLastReadTimeCalcEnabled = true;
        private volatile boolean isLastWriteTimeCalcEnabled = true;
        private volatile boolean isScheduledWriteBytesCalcEnabled = true;
        private volatile boolean isScheduledWriteMessagesCalcEnabled = true;

        /** The time (in second) between the computation of the service's statistics */
        private final AtomicInteger throughputCalculationInterval = new AtomicInteger(3);

        private volatile boolean isStatisticsCalcEnabled = true;

        /**
         * @return Is IoServiceStatistics calculations enabled
         */
        public boolean isStatisticsCalcEnabled() {
            return isStatisticsCalcEnabled;
        }

        /**
         * Enable/disable IoServiceStatistics calculations for all parameters
         *
         * @param statisticsCalcEnabled Enabled/disabled boolean value
         */
        public void setStatisticsCalcEnabled(boolean statisticsCalcEnabled) {
            isStatisticsCalcEnabled = statisticsCalcEnabled;
        }

        /**
         * @return Is the number of read bytes calculation enabled
         */
        public boolean isReadBytesCalcEnabled() {
            return isReadBytesCalcEnabled;
        }

        /**
         * Enable/disable the number of read bytes calculation
         *
         * @param readBytesCalcEnabled Enabled/disabled boolean value
         */
        public void setReadBytesCalcEnabled(boolean readBytesCalcEnabled) {
            isReadBytesCalcEnabled = readBytesCalcEnabled;
        }

        /**
         * @return Is the number of written bytes calculation enabled
         */
        public boolean isWrittenBytesCalcEnabled() {
            return isWrittenBytesCalcEnabled;
        }

        /**
         * Enable/disable the number of written bytes calculation
         *
         * @param writtenBytesCalcEnabled Enabled/disabled boolean value
         */
        public void setWrittenBytesCalcEnabled(boolean writtenBytesCalcEnabled) {
            isWrittenBytesCalcEnabled = writtenBytesCalcEnabled;
        }

        /**
         * @return Is the number of read messages calculation enabled
         */
        public boolean isReadMessagesCalcEnabled() {
            return isReadMessagesCalcEnabled;
        }

        /**
         * Enable/disable the number of read messages calculation
         *
         * @param readMessagesCalcEnabled Enabled/disabled boolean value
         */
        public void setReadMessagesCalcEnabled(boolean readMessagesCalcEnabled) {
            isReadMessagesCalcEnabled = readMessagesCalcEnabled;
        }

        /**
         * @return Is the number of written messages calculation enabled
         */
        public boolean isWrittenMessagesCalcEnabled() {
            return isWrittenMessagesCalcEnabled;
        }

        /**
         * Enable/disable the number of written messages calculation
         *
         * @param writtenMessagesCalcEnabled Enabled/disabled boolean value
         */
        public void setWrittenMessagesCalcEnabled(boolean writtenMessagesCalcEnabled) {
            isWrittenMessagesCalcEnabled = writtenMessagesCalcEnabled;
        }

        /**
         * @return Is the last read time calculation enabled
         */
        public boolean isLastReadTimeCalcEnabled() {
            return isLastReadTimeCalcEnabled;
        }

        /**
         * Enable/disable the last read time calculation
         *
         * @param lastReadTimeCalcEnabled Enabled/disabled boolean value
         */
        public void setLastReadTimeCalcEnabled(boolean lastReadTimeCalcEnabled) {
            isLastReadTimeCalcEnabled = lastReadTimeCalcEnabled;
        }

        /**
         *
         * @return Is the last write time calculation enabled
         */
        public boolean isLastWriteTimeCalcEnabled() {
            return isLastWriteTimeCalcEnabled;
        }

        /**
         * Enable/disable the last write time calculation
         *
         * @param lastWriteTimeCalcEnabled Enabled/disabled boolean value
         */
        public void setLastWriteTimeCalcEnabled(boolean lastWriteTimeCalcEnabled) {
            isLastWriteTimeCalcEnabled = lastWriteTimeCalcEnabled;
        }

        /**
         * @return Is scheduled for write the number of bytes calculation enabled
         */
        public boolean isScheduledWriteBytesCalcEnabled() {
            return isScheduledWriteBytesCalcEnabled;
        }

        /**
         * Enable/disable scheduled for write the number of bytes calculation
         *
         * @param scheduledWriteBytesCalcEnabled Enabled/disabled boolean value
         */
        public void setScheduledWriteBytesCalcEnabled(boolean scheduledWriteBytesCalcEnabled) {
            isScheduledWriteBytesCalcEnabled = scheduledWriteBytesCalcEnabled;
        }

        /**
         * @return Is scheduled for write the number of messages calculation enabled
         */
        public boolean isScheduledWriteMessagesCalcEnabled() {
            return isScheduledWriteMessagesCalcEnabled;
        }

        /**
         * Enable/disable scheduled for write messages calculation
         *
         * @param scheduledWriteMessagesCalcEnabled Enabled/disabled boolean value
         */
        public void setScheduledWriteMessagesCalcEnabled(boolean scheduledWriteMessagesCalcEnabled) {
            isScheduledWriteMessagesCalcEnabled = scheduledWriteMessagesCalcEnabled;
        }

        /**
         * @return the interval (seconds) between each throughput calculation. The
         *         default value is <tt>3</tt> seconds.
         */
        public int getThroughputCalculationInterval() {
            return throughputCalculationInterval.get();
        }

        /**
         * @return the interval (milliseconds) between each throughput calculation.
         * The default value is <tt>3</tt> seconds.
         */
        public long getThroughputCalculationIntervalInMillis() {
            return throughputCalculationInterval.get() * 1000L;
        }

        /**
         * Sets the interval (seconds) between each throughput calculation.  The
         * default value is <tt>3</tt> seconds.
         *
         * @param throughputCalculationInterval The interval between two calculation
         */
        public void setThroughputCalculationInterval(int throughputCalculationInterval) {
            if (throughputCalculationInterval < 0) {
                throw new IllegalArgumentException("throughputCalculationInterval: " + throughputCalculationInterval);
            }

            this.throughputCalculationInterval.set(throughputCalculationInterval);
        }

    }
}
