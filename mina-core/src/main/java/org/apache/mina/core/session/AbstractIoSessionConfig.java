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
 * A base implementation of {@link IoSessionConfig}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractIoSessionConfig implements IoSessionConfig {
    /** The minimum size of the buffer used to read incoming data */
    private int minReadBufferSize = 64;
    
    /** The default size of the buffer used to read incoming data */
    private int readBufferSize = 2048;

    /** The maximum size of the buffer used to read incoming data */
    private int maxReadBufferSize = 65536;

    /** The delay before we notify a session that it has been idle on read. Default to infinite */
    private int idleTimeForRead;

    /** The delay before we notify a session that it has been idle on write. Default to infinite */
    private int idleTimeForWrite;

    /** 
     * The delay before we notify a session that it has been idle on read and write. 
     * Default to infinite 
     **/
    private int idleTimeForBoth;

    /** The delay to wait for a write operation to complete before bailing out */
    private int writeTimeout = 60;

    /** A flag set to true when weallow the application to do a session.read(). Default to false */
    private boolean useReadOperation;

    private int throughputCalculationInterval = 3;

    protected AbstractIoSessionConfig() {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAll(IoSessionConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config");
        }

        setReadBufferSize(config.getReadBufferSize());
        setMaxReadBufferSize(config.getMaxReadBufferSize());
        setMinReadBufferSize(config.getMinReadBufferSize());
        setIdleTime(IdleStatus.BOTH_IDLE, config.getIdleTime(IdleStatus.BOTH_IDLE));
        setIdleTime(IdleStatus.READER_IDLE, config.getIdleTime(IdleStatus.READER_IDLE));
        setIdleTime(IdleStatus.WRITER_IDLE, config.getIdleTime(IdleStatus.WRITER_IDLE));
        setWriteTimeout(config.getWriteTimeout());
        setUseReadOperation(config.isUseReadOperation());
        setThroughputCalculationInterval(config.getThroughputCalculationInterval());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getReadBufferSize() {
        return readBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReadBufferSize(int readBufferSize) {
        if (readBufferSize <= 0) {
            throw new IllegalArgumentException("readBufferSize: " + readBufferSize + " (expected: 1+)");
        }
        this.readBufferSize = readBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMinReadBufferSize() {
        return minReadBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMinReadBufferSize(int minReadBufferSize) {
        if (minReadBufferSize <= 0) {
            throw new IllegalArgumentException("minReadBufferSize: " + minReadBufferSize + " (expected: 1+)");
        }
        if (minReadBufferSize > maxReadBufferSize) {
            throw new IllegalArgumentException("minReadBufferSize: " + minReadBufferSize + " (expected: smaller than "
                    + maxReadBufferSize + ')');

        }
        this.minReadBufferSize = minReadBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxReadBufferSize() {
        return maxReadBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMaxReadBufferSize(int maxReadBufferSize) {
        if (maxReadBufferSize <= 0) {
            throw new IllegalArgumentException("maxReadBufferSize: " + maxReadBufferSize + " (expected: 1+)");
        }

        if (maxReadBufferSize < minReadBufferSize) {
            throw new IllegalArgumentException("maxReadBufferSize: " + maxReadBufferSize + " (expected: greater than "
                    + minReadBufferSize + ')');

        }
        this.maxReadBufferSize = maxReadBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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

    /**
     * {@inheritDoc}
     */
    @Override
    public long getIdleTimeInMillis(IdleStatus status) {
        return getIdleTime(status) * 1000L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getBothIdleTime() {
        return getIdleTime(IdleStatus.BOTH_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final long getBothIdleTimeInMillis() {
        return getIdleTimeInMillis(IdleStatus.BOTH_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getReaderIdleTime() {
        return getIdleTime(IdleStatus.READER_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final long getReaderIdleTimeInMillis() {
        return getIdleTimeInMillis(IdleStatus.READER_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getWriterIdleTime() {
        return getIdleTime(IdleStatus.WRITER_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final long getWriterIdleTimeInMillis() {
        return getIdleTimeInMillis(IdleStatus.WRITER_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBothIdleTime(int idleTime) {
        setIdleTime(IdleStatus.BOTH_IDLE, idleTime);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReaderIdleTime(int idleTime) {
        setIdleTime(IdleStatus.READER_IDLE, idleTime);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWriterIdleTime(int idleTime) {
        setIdleTime(IdleStatus.WRITER_IDLE, idleTime);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWriteTimeout() {
        return writeTimeout;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getWriteTimeoutInMillis() {
        return writeTimeout * 1000L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWriteTimeout(int writeTimeout) {
        if (writeTimeout < 0) {
            throw new IllegalArgumentException("Illegal write timeout: " + writeTimeout);
        }
        this.writeTimeout = writeTimeout;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUseReadOperation() {
        return useReadOperation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUseReadOperation(boolean useReadOperation) {
        this.useReadOperation = useReadOperation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getThroughputCalculationInterval() {
        return throughputCalculationInterval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setThroughputCalculationInterval(int throughputCalculationInterval) {
        if (throughputCalculationInterval < 0) {
            throw new IllegalArgumentException("throughputCalculationInterval: " + throughputCalculationInterval);
        }

        this.throughputCalculationInterval = throughputCalculationInterval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getThroughputCalculationIntervalInMillis() {
        return throughputCalculationInterval * 1000L;
    }
}
