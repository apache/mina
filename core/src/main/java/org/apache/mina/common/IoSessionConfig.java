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
package org.apache.mina.common;

/**
 * The configuration of {@link IoSession}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
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
     * Sets all configuration properties retrieved from the specified
     * <tt>config</tt>.
     */
    void setAll(IoSessionConfig config);
}
