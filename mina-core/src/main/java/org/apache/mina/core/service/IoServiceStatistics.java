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
import java.util.concurrent.atomic.AtomicLong;


/**
 * Provides usage statistics for an {@link AbstractIoService} instance.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since 2.0.0-M3
 */
public class IoServiceStatistics {
    
    private AbstractIoService service;
    
    private double readBytesThroughput;
    private double writtenBytesThroughput;
    private double readMessagesThroughput;
    private double writtenMessagesThroughput;
    private double largestReadBytesThroughput;
    private double largestWrittenBytesThroughput;
    private double largestReadMessagesThroughput;
    private double largestWrittenMessagesThroughput;    
    
    private final AtomicLong readBytes = new AtomicLong();
    private final AtomicLong writtenBytes = new AtomicLong();
    private final AtomicLong readMessages = new AtomicLong();
    private final AtomicLong writtenMessages = new AtomicLong();    
    private long lastReadTime;
    private long lastWriteTime;
    
    private long lastReadBytes;
    private long lastWrittenBytes;
    private long lastReadMessages;
    private long lastWrittenMessages;
    private long lastThroughputCalculationTime;

    private final AtomicInteger scheduledWriteBytes = new AtomicInteger();
    private final AtomicInteger scheduledWriteMessages = new AtomicInteger();
    
    private int throughputCalculationInterval = 3;
    
    private final Object throughputCalculationLock = new Object();
    
    public IoServiceStatistics(AbstractIoService service) {
        this.service = service;
    }
    
    /**
     * Returns the maximum number of sessions which were being managed at the
     * same time.
     */
    public final int getLargestManagedSessionCount() {
        return service.getListeners().getLargestManagedSessionCount();
    }

    /**
     * Returns the cumulative number of sessions which were managed (or are
     * being managed) by this service, which means 'currently managed session
     * count + closed session count'.
     */
    public final long getCumulativeManagedSessionCount() {
        return service.getListeners().getCumulativeManagedSessionCount();
    }
    
    /**
     * Returns the time in millis when I/O occurred lastly.
     */
    public final long getLastIoTime() {
        return Math.max(lastReadTime, lastWriteTime);
    }

    /**
     * Returns the time in millis when read operation occurred lastly.
     */
    public final long getLastReadTime() {
        return lastReadTime;
    }

    /**
     * Returns the time in millis when write operation occurred lastly.
     */
    public final long getLastWriteTime() {
        return lastWriteTime;
    }
    
    /**
     * Returns the number of bytes read by this service
     *
     * @return
     *     The number of bytes this service has read
     */
    public final long getReadBytes() {
        return readBytes.get();
    }

    /**
     * Returns the number of bytes written out by this service
     *
     * @return
     *     The number of bytes this service has written
     */
    public final long getWrittenBytes() {
        return writtenBytes.get();
    }

    /**
     * Returns the number of messages this services has read
     *
     * @return
     *     The number of messages this services has read
     */
    public final long getReadMessages() {
        return readMessages.get();
    }

    /**
     * Returns the number of messages this service has written
     *
     * @return
     *     The number of messages this service has written
     */
    public final long getWrittenMessages() {
        return writtenMessages.get();
    }

    /**
     * Returns the number of read bytes per second.
     */
    public final double getReadBytesThroughput() {
        resetThroughput();
        return readBytesThroughput;
    }

    /**
     * Returns the number of written bytes per second.
     */
    public final double getWrittenBytesThroughput() {
        resetThroughput();
        return writtenBytesThroughput;
    }

    /**
     * Returns the number of read messages per second.
     */
    public final double getReadMessagesThroughput() {
        resetThroughput();
        return readMessagesThroughput;
    }

    /**
     * Returns the number of written messages per second.
     */
    public final double getWrittenMessagesThroughput() {
        resetThroughput();
        return writtenMessagesThroughput;
    }

    /**
     * Returns the maximum of the {@link #getReadBytesThroughput() readBytesThroughput}.
     */
    public final double getLargestReadBytesThroughput() {
        return largestReadBytesThroughput;
    }

    /**
     * Returns the maximum of the {@link #getWrittenBytesThroughput() writtenBytesThroughput}.
     */
    public final double getLargestWrittenBytesThroughput() {
        return largestWrittenBytesThroughput;
    }

    /**
     * Returns the maximum of the {@link #getReadMessagesThroughput() readMessagesThroughput}.
     */
    public final double getLargestReadMessagesThroughput() {
        return largestReadMessagesThroughput;
    }

    /**
     * Returns the maximum of the {@link #getWrittenMessagesThroughput() writtenMessagesThroughput}.
     */
    public final double getLargestWrittenMessagesThroughput() {
        return largestWrittenMessagesThroughput;
    }

    /**
     * Returns the interval (seconds) between each throughput calculation.
     * The default value is <tt>3</tt> seconds.
     */
    public final int getThroughputCalculationInterval() {
        return throughputCalculationInterval;
    }

    /**
     * Returns the interval (milliseconds) between each throughput calculation.
     * The default value is <tt>3</tt> seconds.
     */
    public final long getThroughputCalculationIntervalInMillis() {
        return throughputCalculationInterval * 1000L;
    }

    /**
     * Sets the interval (seconds) between each throughput calculation.  The
     * default value is <tt>3</tt> seconds.
     */
    public final void setThroughputCalculationInterval(
            int throughputCalculationInterval) {
        if (throughputCalculationInterval < 0) {
            throw new IllegalArgumentException(
                    "throughputCalculationInterval: "
                            + throughputCalculationInterval);
        }

        this.throughputCalculationInterval = throughputCalculationInterval;
    }

    /**
     * Sets last time at which a read occurred on the service.
     */
    protected final void setLastReadTime(long lastReadTime) {
        this.lastReadTime = lastReadTime;
    }

    /**
     * Sets last time at which a write occurred on the service.
     */
    protected final void setLastWriteTime(long lastWriteTime) {
        this.lastWriteTime = lastWriteTime;
    }
    
    /**
     * Resets the throughput counters of the service if none session 
     * is currently managed. 
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
     */    
    public void updateThroughput(long currentTime) {
        synchronized (throughputCalculationLock) {
            int interval = (int) (currentTime - lastThroughputCalculationTime);
            long minInterval = getThroughputCalculationIntervalInMillis();
            if (minInterval == 0 || interval < minInterval) {
                return;
            }

            long readBytes = this.readBytes.get();
            long writtenBytes = this.writtenBytes.get();
            long readMessages = this.readMessages.get();
            long writtenMessages = this.writtenMessages.get();

            readBytesThroughput = (readBytes - lastReadBytes) * 1000.0
                    / interval;
            writtenBytesThroughput = (writtenBytes - lastWrittenBytes) * 1000.0
                    / interval;
            readMessagesThroughput = (readMessages - lastReadMessages) * 1000.0
                    / interval;
            writtenMessagesThroughput = (writtenMessages - lastWrittenMessages)
                    * 1000.0 / interval;

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
        }
    }
    
    /**
     * Increases the count of read bytes by <code>increment</code> and sets 
     * the last read time to <code>currentTime</code>.
     */ 
    public final void increaseReadBytes(long increment, long currentTime) {
        readBytes.addAndGet(increment);
        lastReadTime = currentTime;
    }

    /**
     * Increases the count of read messages by 1 and sets the last read time to 
     * <code>currentTime</code>.
     */ 
    public final void increaseReadMessages(long currentTime) {
        readMessages.incrementAndGet();
        lastReadTime = currentTime;
    }
    
    /**
     * Increases the count of written bytes by <code>increment</code> and sets 
     * the last write time to <code>currentTime</code>.
     */ 
    public final void increaseWrittenBytes(int increment, long currentTime) {
        writtenBytes.addAndGet(increment);
        lastWriteTime = currentTime;
    }

    /**
     * Increases the count of written messages by 1 and sets the last write time to 
     * <code>currentTime</code>.
     */   
    public final void increaseWrittenMessages(long currentTime) {
        writtenMessages.incrementAndGet();
        lastWriteTime = currentTime;
    }
    
    /**
     * Returns the count of bytes scheduled for write.
     */
    public final int getScheduledWriteBytes() {
        return scheduledWriteBytes.get();
    }

    /**
     * Increments by <code>increment</code> the count of bytes scheduled for write.
     */
    public final void increaseScheduledWriteBytes(int increment) {
        scheduledWriteBytes.addAndGet(increment);
    }

    /**
     * Returns the count of messages scheduled for write.
     */
    public final int getScheduledWriteMessages() {
        return scheduledWriteMessages.get();
    }

    /**
     * Increments by 1 the count of messages scheduled for write.
     */    
    public final void increaseScheduledWriteMessages() {
        scheduledWriteMessages.incrementAndGet();
    }

    /**
     * Decrements by 1 the count of messages scheduled for write.
     */    
    public final void decreaseScheduledWriteMessages() {
        scheduledWriteMessages.decrementAndGet();
    }

    /**
     * Sets the time at which throughtput counters where updated.
     */        
    protected void setLastThroughputCalculationTime(
            long lastThroughputCalculationTime) {
        this.lastThroughputCalculationTime = lastThroughputCalculationTime;
    }    
}
