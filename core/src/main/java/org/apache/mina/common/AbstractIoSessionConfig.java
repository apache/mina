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
 * A base implementation of {@link IoSessionConfig}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractIoSessionConfig implements IoSessionConfig {

    private int minReadBufferSize = 64;
    private int readBufferSize = 2048;
    private int maxReadBufferSize = 65536;
    private int idleTimeForRead;
    private int idleTimeForWrite;
    private int idleTimeForBoth;
    private int writeTimeout;

    protected AbstractIoSessionConfig() {
    }

    public final void setAll(IoSessionConfig config) {
        if (config == null) {
            throw new NullPointerException("config");
        }

        setReadBufferSize(config.getReadBufferSize());
        setMinReadBufferSize(config.getMinReadBufferSize());
        setMaxReadBufferSize(config.getMaxReadBufferSize());
        setIdleTime(IdleStatus.BOTH_IDLE, config.getIdleTime(IdleStatus.BOTH_IDLE));
        setIdleTime(IdleStatus.READER_IDLE, config.getIdleTime(IdleStatus.READER_IDLE));
        setIdleTime(IdleStatus.WRITER_IDLE, config.getIdleTime(IdleStatus.WRITER_IDLE));
        setWriteTimeout(config.getWriteTimeout());

        doSetAll(config);
    }

    /**
     * Implement this method to set all transport-specific configuration
     * properties retrieved from the specified <tt>config</tt>.
     */
    protected abstract void doSetAll(IoSessionConfig config);

    public int getReadBufferSize() {
        return readBufferSize;
    }

    public void setReadBufferSize(int readBufferSize) {
        if (readBufferSize <= 0) {
            throw new IllegalArgumentException("readBufferSize: " + readBufferSize + " (expected: 1+)");
        }
        this.readBufferSize = readBufferSize;
    }

    public int getMinReadBufferSize() {
        return minReadBufferSize;
    }

    public void setMinReadBufferSize(int minReadBufferSize) {
        if (minReadBufferSize <= 0) {
            throw new IllegalArgumentException("minReadBufferSize: " + minReadBufferSize + " (expected: 1+)");
        }
        if (minReadBufferSize > maxReadBufferSize ) {
            throw new IllegalArgumentException("minReadBufferSize: " + minReadBufferSize + " (expected: smaller than " + maxReadBufferSize + ')');

        }
        this.minReadBufferSize = minReadBufferSize;
    }

    public int getMaxReadBufferSize() {
        return maxReadBufferSize;
    }

    public void setMaxReadBufferSize(int maxReadBufferSize) {
        if (maxReadBufferSize <= 0) {
            throw new IllegalArgumentException("maxReadBufferSize: " + maxReadBufferSize + " (expected: 1+)");
        }

        if (maxReadBufferSize < minReadBufferSize) {
            throw new IllegalArgumentException("maxReadBufferSize: " + maxReadBufferSize + " (expected: greater than " + minReadBufferSize + ')');

        }
        this.maxReadBufferSize = maxReadBufferSize;
    }

    public int getIdleTime(IdleStatus status) {
        if (status == IdleStatus.BOTH_IDLE) {
            return idleTimeForBoth;
        }

        if (status == IdleStatus.READER_IDLE) {
            return idleTimeForRead;
        }

        if (status == IdleStatus.WRITER_IDLE) {
            return idleTimeForWrite;
        }

        throw new IllegalArgumentException("Unknown idle status: " + status);
    }

    public long getIdleTimeInMillis(IdleStatus status) {
        return getIdleTime(status) * 1000L;
    }

    public void setIdleTime(IdleStatus status, int idleTime) {
        if (idleTime < 0) {
            throw new IllegalArgumentException("Illegal idle time: " + idleTime);
        }

        if (status == IdleStatus.BOTH_IDLE) {
            idleTimeForBoth = idleTime;
        } else if (status == IdleStatus.READER_IDLE) {
            idleTimeForRead = idleTime;
        } else if (status == IdleStatus.WRITER_IDLE) {
            idleTimeForWrite = idleTime;
        } else {
            throw new IllegalArgumentException("Unknown idle status: " + status);
        }
    }

    public int getWriteTimeout() {
        return writeTimeout;
    }

    public long getWriteTimeoutInMillis() {
        return writeTimeout * 1000L;
    }

    public void setWriteTimeout(int writeTimeout) {
        if (writeTimeout < 0) {
            throw new IllegalArgumentException("Illegal write timeout: "
                    + writeTimeout);
        }
        this.writeTimeout = writeTimeout;
    }
}
