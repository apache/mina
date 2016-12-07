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

    private IoService service;

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

    /** The time (in second) between the computation of the service's statistics */
    private final AtomicInteger throughputCalculationInterval = new AtomicInteger(3);

    private final Lock throughputCalculationLock = new ReentrantLock();

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
        return throughputCalculationInterval.get();
    }

    /**
     * @return the interval (milliseconds) between each throughput calculation.
     * The default value is <tt>3</tt> seconds.
     */
    public final long getThroughputCalculationIntervalInMillis() {
        return throughputCalculationInterval.get() * 1000L;
    }

    /**
     * Sets the interval (seconds) between each throughput calculation.  The
     * default value is <tt>3</tt> seconds.
     * 
     * @param throughputCalculationInterval The interval between two calculation
     */
    public final void setThroughputCalculationInterval(int throughputCalculationInterval) {
        if (throughputCalculationInterval < 0) {
            throw new IllegalArgumentException("throughputCalculationInterval: " + throughputCalculationInterval);
        }

        this.throughputCalculationInterval.set(throughputCalculationInterval);
    }

    /**
     * Sets last time at which a read occurred on the service.
     * 
     * @param lastReadTime
     *            The last time a read has occurred
     */
    protected final void setLastReadTime(long lastReadTime) {
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
        throughputCalculationLock.lock();

        try {
            int interval = (int) (currentTime - lastThroughputCalculationTime);
            long minInterval = getThroughputCalculationIntervalInMillis();

            if ((minInterval == 0) || (interval < minInterval)) {
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
        throughputCalculationLock.lock();

        try {
            this.lastThroughputCalculationTime = lastThroughputCalculationTime;
        } finally {
            throughputCalculationLock.unlock();
        }
    }
}
