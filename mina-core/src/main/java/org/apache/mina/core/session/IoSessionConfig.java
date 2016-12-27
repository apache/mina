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

/**
 * The configuration of {@link IoSession}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoSessionConfig {

    /**
     * @return the size of the read buffer that I/O processor allocates
     * per each read.  It's unusual to adjust this property because
     * it's often adjusted automatically by the I/O processor.
     */
    int getReadBufferSize();

    /**
     * Sets the size of the read buffer that I/O processor allocates
     * per each read.  It's unusual to adjust this property because
     * it's often adjusted automatically by the I/O processor.
     * 
     * @param readBufferSize The size of the read buffer
     */
    void setReadBufferSize(int readBufferSize);

    /**
     * @return the minimum size of the read buffer that I/O processor
     * allocates per each read.  I/O processor will not decrease the
     * read buffer size to the smaller value than this property value.
     */
    int getMinReadBufferSize();

    /**
     * Sets the minimum size of the read buffer that I/O processor
     * allocates per each read.  I/O processor will not decrease the
     * read buffer size to the smaller value than this property value.
     * 
     * @param minReadBufferSize The minimum size of the read buffer
     */
    void setMinReadBufferSize(int minReadBufferSize);

    /**
     * @return the maximum size of the read buffer that I/O processor
     * allocates per each read.  I/O processor will not increase the
     * read buffer size to the greater value than this property value.
     */
    int getMaxReadBufferSize();

    /**
     * Sets the maximum size of the read buffer that I/O processor
     * allocates per each read.  I/O processor will not increase the
     * read buffer size to the greater value than this property value.
     * 
     * @param maxReadBufferSize The maximum size of the read buffer
     */
    void setMaxReadBufferSize(int maxReadBufferSize);

    /**
     * @return the interval (seconds) between each throughput calculation.
     * The default value is <tt>3</tt> seconds.
     */
    int getThroughputCalculationInterval();

    /**
     * @return the interval (milliseconds) between each throughput calculation.
     * The default value is <tt>3</tt> seconds.
     */
    long getThroughputCalculationIntervalInMillis();

    /**
     * Sets the interval (seconds) between each throughput calculation.  The
     * default value is <tt>3</tt> seconds.
     * 
     * @param throughputCalculationInterval The interval
     */
    void setThroughputCalculationInterval(int throughputCalculationInterval);

    /**
     * @return idle time for the specified type of idleness in seconds.
     * 
     * @param status The status for which we want the idle time (One of READER_IDLE,
     * WRITER_IDLE or BOTH_IDLE)
     */
    int getIdleTime(IdleStatus status);

    /**
     * @return idle time for the specified type of idleness in milliseconds.
     * 
     * @param status The status for which we want the idle time (One of READER_IDLE,
     * WRITER_IDLE or BOTH_IDLE)
     */
    long getIdleTimeInMillis(IdleStatus status);

    /**
     * Sets idle time for the specified type of idleness in seconds.
     * @param status The status for which we want to set the idle time (One of READER_IDLE,
     * WRITER_IDLE or BOTH_IDLE)
     * @param idleTime The time in second to set
     */
    void setIdleTime(IdleStatus status, int idleTime);

    /**
     * @return idle time for {@link IdleStatus#READER_IDLE} in seconds.
     */
    int getReaderIdleTime();

    /**
     * @return idle time for {@link IdleStatus#READER_IDLE} in milliseconds.
     */
    long getReaderIdleTimeInMillis();

    /**
     * Sets idle time for {@link IdleStatus#READER_IDLE} in seconds.
     * 
     * @param idleTime The time to set
     */
    void setReaderIdleTime(int idleTime);

    /**
     * @return idle time for {@link IdleStatus#WRITER_IDLE} in seconds.
     */
    int getWriterIdleTime();

    /**
     * @return idle time for {@link IdleStatus#WRITER_IDLE} in milliseconds.
     */
    long getWriterIdleTimeInMillis();

    /**
     * Sets idle time for {@link IdleStatus#WRITER_IDLE} in seconds.
     * 
     * @param idleTime The time to set
     */
    void setWriterIdleTime(int idleTime);

    /**
     * @return idle time for {@link IdleStatus#BOTH_IDLE} in seconds.
     */
    int getBothIdleTime();

    /**
     * @return idle time for {@link IdleStatus#BOTH_IDLE} in milliseconds.
     */
    long getBothIdleTimeInMillis();

    /**
     * Sets idle time for {@link IdleStatus#WRITER_IDLE} in seconds.
     * 
     * @param idleTime The time to set
     */
    void setBothIdleTime(int idleTime);

    /**
     * @return write timeout in seconds.
     */
    int getWriteTimeout();

    /**
     * @return write timeout in milliseconds.
     */
    long getWriteTimeoutInMillis();

    /**
     * Sets write timeout in seconds.
     * 
     * @param writeTimeout The timeout to set
     */
    void setWriteTimeout(int writeTimeout);

    /**
     * @return <tt>true</tt> if and only if {@link IoSession#read()} operation
     * is enabled.  If enabled, all received messages are stored in an internal
     * {@link BlockingQueue} so you can read received messages in more
     * convenient way for client applications.  Enabling this option is not
     * useful to server applications and can cause unintended memory leak, and
     * therefore it's disabled by default.
     */
    boolean isUseReadOperation();

    /**
     * Enables or disabled {@link IoSession#read()} operation.  If enabled, all
     * received messages are stored in an internal {@link BlockingQueue} so you
     * can read received messages in more convenient way for client
     * applications.  Enabling this option is not useful to server applications
     * and can cause unintended memory leak, and therefore it's disabled by
     * default.
     * 
     * @param useReadOperation <tt>true</tt> if the read operation is enabled, <tt>false</tt> otherwise
     */
    void setUseReadOperation(boolean useReadOperation);

    /**
     * Sets all configuration properties retrieved from the specified
     * <tt>config</tt>.
     * 
     * @param config The configuration to use
     */
    void setAll(IoSessionConfig config);
}
