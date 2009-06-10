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

import java.util.concurrent.BlockingQueue;


/**
 * The configuration of {@link IoSession}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoSessionConfig {

    /**
     * Returns the size of the read buffer that I/O processor allocates
     * per each read.  It's unusual to adjust this property because
     * it's often adjusted automatically by the I/O processor.
     */
    int getReadBufferSize();

    /**
     * Sets the size of the read buffer that I/O processor allocates
     * per each read.  It's unusual to adjust this property because
     * it's often adjusted automatically by the I/O processor.
     */
    void setReadBufferSize(int readBufferSize);

    /**
     * Returns the minimum size of the read buffer that I/O processor
     * allocates per each read.  I/O processor will not decrease the
     * read buffer size to the smaller value than this property value.
     */
    int getMinReadBufferSize();

    /**
     * Sets the minimum size of the read buffer that I/O processor
     * allocates per each read.  I/O processor will not decrease the
     * read buffer size to the smaller value than this property value.
     */
    void setMinReadBufferSize(int minReadBufferSize);

    /**
     * Returns the maximum size of the read buffer that I/O processor
     * allocates per each read.  I/O processor will not increase the
     * read buffer size to the greater value than this property value.
     */
    int getMaxReadBufferSize();

    /**
     * Sets the maximum size of the read buffer that I/O processor
     * allocates per each read.  I/O processor will not increase the
     * read buffer size to the greater value than this property value.
     */
    void setMaxReadBufferSize(int maxReadBufferSize);
    
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
     * Returns write timeout in seconds.
     */
    int getWriteTimeout();

    /**
     * Returns write timeout in milliseconds.
     */
    long getWriteTimeoutInMillis();

    /**
     * Sets write timeout in seconds.
     */
    void setWriteTimeout(int writeTimeout);
    
    /**
     * Returns <tt>true</tt> if and only if {@link IoSession#read()} operation
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
     */
    void setUseReadOperation(boolean useReadOperation);

    /**
     * Sets all configuration properties retrieved from the specified
     * <tt>config</tt>.
     */
    void setAll(IoSessionConfig config);
}
