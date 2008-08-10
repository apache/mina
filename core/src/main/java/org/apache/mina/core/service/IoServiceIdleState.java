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
 * Provides the idle state information associated with an {@link AbstractIoService}.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * @since 2.0.0-M3
 */
public class IoServiceIdleState {

    private AbstractIoService service;
    
    private int idleTimeForRead;
    private int idleTimeForWrite;
    private int idleTimeForBoth;

    private int idleCountForBoth;
    private int idleCountForRead;
    private int idleCountForWrite;

    private long lastIdleTimeForBoth;
    private long lastIdleTimeForRead;
    private long lastIdleTimeForWrite;

    private final Object idlenessCheckLock = new Object();

    public IoServiceIdleState(AbstractIoService service) {
        this.service = service;
    }

    /**
     * Returns <code>true</code> if this service is idle for the specified
     * {@link IdleStatus}.
     */
    public final boolean isIdle(IdleStatus status) {
        if (status == IdleStatus.BOTH_IDLE) {
            return idleCountForBoth > 0;
        }

        if (status == IdleStatus.READER_IDLE) {
            return idleCountForRead > 0;
        }

        if (status == IdleStatus.WRITER_IDLE) {
            return idleCountForWrite > 0;
        }

        throw new IllegalArgumentException("Unknown idle status: " + status);
    }

    /**
     * Returns <code>true</code> if this service is {@link IdleStatus#READER_IDLE}.
     * @see #isIdle(IdleStatus)
     */
    public final boolean isReaderIdle() {
        return isIdle(IdleStatus.READER_IDLE);
    }

    /**
     * Returns <code>true</code> if this service is {@link IdleStatus#WRITER_IDLE}.
     * @see #isIdle(IdleStatus)
     */
    public final boolean isWriterIdle() {
        return isIdle(IdleStatus.WRITER_IDLE);
    }

    /**
     * Returns <code>true</code> if this service is {@link IdleStatus#BOTH_IDLE}.
     * @see #isIdle(IdleStatus)
     */
    public final boolean isBothIdle() {
        return isIdle(IdleStatus.BOTH_IDLE);
    }

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
    public final int getIdleCount(IdleStatus status) {
        if (status == IdleStatus.BOTH_IDLE) {
            return idleCountForBoth;
        }

        if (status == IdleStatus.READER_IDLE) {
            return idleCountForRead;
        }

        if (status == IdleStatus.WRITER_IDLE) {
            return idleCountForWrite;
        }

        throw new IllegalArgumentException("Unknown idle status: " + status);
    }

    /**
     * Returns the number of the fired continuous <tt>serviceIdle</tt> events
     * for {@link IdleStatus#READER_IDLE}.
     * @see #getIdleCount(IdleStatus)
     */
    public final int getReaderIdleCount() {
        return getIdleCount(IdleStatus.READER_IDLE);
    }

    /**
     * Returns the number of the fired continuous <tt>serviceIdle</tt> events
     * for {@link IdleStatus#WRITER_IDLE}.
     * @see #getIdleCount(IdleStatus)
     */
    public final int getWriterIdleCount() {
        return getIdleCount(IdleStatus.WRITER_IDLE);
    }

    /**
     * Returns the number of the fired continuous <tt>serviceIdle</tt> events
     * for {@link IdleStatus#BOTH_IDLE}.
     * @see #getIdleCount(IdleStatus)
     */
    public final int getBothIdleCount() {
        return getIdleCount(IdleStatus.BOTH_IDLE);
    }

    /**
     * Returns the time in milliseconds when the last <tt>serviceIdle</tt> event
     * is fired for the specified {@link IdleStatus}.
     */
    public final long getLastIdleTime(IdleStatus status) {
        if (status == IdleStatus.BOTH_IDLE) {
            return lastIdleTimeForBoth;
        }

        if (status == IdleStatus.READER_IDLE) {
            return lastIdleTimeForRead;
        }

        if (status == IdleStatus.WRITER_IDLE) {
            return lastIdleTimeForWrite;
        }

        throw new IllegalArgumentException("Unknown idle status: " + status);
    }

    /**
     * Returns the time in milliseconds when the last <tt>serviceIdle</tt> event
     * is fired for {@link IdleStatus#READER_IDLE}.
     * @see #getLastIdleTime(IdleStatus)
     */
    public final long getLastReaderIdleTime() {
        return getLastIdleTime(IdleStatus.READER_IDLE);
    }

    /**
     * Returns the time in milliseconds when the last <tt>serviceIdle</tt> event
     * is fired for {@link IdleStatus#WRITER_IDLE}.
     * @see #getLastIdleTime(IdleStatus)
     */
    public final long getLastWriterIdleTime() {
        return getLastIdleTime(IdleStatus.WRITER_IDLE);
    }

    /**
     * Returns the time in milliseconds when the last <tt>serviceIdle</tt> event
     * is fired for {@link IdleStatus#BOTH_IDLE}.
     * @see #getLastIdleTime(IdleStatus)
     */
    public final long getLastBothIdleTime() {
        return getLastIdleTime(IdleStatus.BOTH_IDLE);
    }

    /**
     * Returns idle time for the specified type of idleness in seconds.
     */
    public final int getIdleTime(IdleStatus status) {
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
     * Returns idle time for the specified type of idleness in milliseconds.
     */
    public final long getIdleTimeInMillis(IdleStatus status) {
        return getIdleTime(status) * 1000L;
    }

    /**
     * Sets idle time for the specified type of idleness in seconds.
     */
    public final void setIdleTime(IdleStatus status, int idleTime) {
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

        if (idleTime == 0) {
            if (status == IdleStatus.BOTH_IDLE) {
                idleCountForBoth = 0;
            } else if (status == IdleStatus.READER_IDLE) {
                idleCountForRead = 0;
            } else if (status == IdleStatus.WRITER_IDLE) {
                idleCountForWrite = 0;
            }
        }
    }

    /**
     * Returns idle time for {@link IdleStatus#READER_IDLE} in seconds.
     */
    public final int getReaderIdleTime() {
        return getIdleTime(IdleStatus.READER_IDLE);
    }

    /**
     * Returns idle time for {@link IdleStatus#READER_IDLE} in milliseconds.
     */
    public final long getReaderIdleTimeInMillis() {
        return getIdleTimeInMillis(IdleStatus.READER_IDLE);
    }

    /**
     * Sets idle time for {@link IdleStatus#READER_IDLE} in seconds.
     */
    public final void setReaderIdleTime(int idleTime) {
        setIdleTime(IdleStatus.READER_IDLE, idleTime);
    }

    /**
     * Returns idle time for {@link IdleStatus#WRITER_IDLE} in seconds.
     */
    public final int getWriterIdleTime() {
        return getIdleTime(IdleStatus.WRITER_IDLE);
    }

    /**
     * Returns idle time for {@link IdleStatus#WRITER_IDLE} in milliseconds.
     */
    public final long getWriterIdleTimeInMillis() {
        return getIdleTimeInMillis(IdleStatus.WRITER_IDLE);
    }

    /**
     * Sets idle time for {@link IdleStatus#WRITER_IDLE} in seconds.
     */
    public final void setWriterIdleTime(int idleTime) {
        setIdleTime(IdleStatus.WRITER_IDLE, idleTime);
    }

    /**
     * Returns idle time for {@link IdleStatus#BOTH_IDLE} in seconds.
     */
    public final int getBothIdleTime() {
        return getIdleTime(IdleStatus.BOTH_IDLE);
    }

    /**
     * Returns idle time for {@link IdleStatus#BOTH_IDLE} in milliseconds.
     */
    public final long getBothIdleTimeInMillis() {
        return getIdleTimeInMillis(IdleStatus.BOTH_IDLE);
    }

    /**
     * Sets idle time for {@link IdleStatus#WRITER_IDLE} in seconds.
     */
    public final void setBothIdleTime(int idleTime) {
        setIdleTime(IdleStatus.BOTH_IDLE, idleTime);
    }

    /**
     * TODO add documentation
     */
    private void increaseIdleCount(IdleStatus status, long currentTime) {
        if (status == IdleStatus.BOTH_IDLE) {
            idleCountForBoth++;
            lastIdleTimeForBoth = currentTime;
        } else if (status == IdleStatus.READER_IDLE) {
            idleCountForRead++;
            lastIdleTimeForRead = currentTime;
        } else if (status == IdleStatus.WRITER_IDLE) {
            idleCountForWrite++;
            lastIdleTimeForWrite = currentTime;
        } else {
            throw new IllegalArgumentException("Unknown idle status: " + status);
        }
    }

    /**
     * TODO add documentation
     */
    public final void notifyIdleness(long currentTime) {
        IoServiceStatistics stats = (IoServiceStatistics) service.getStatistics();
        stats.updateThroughput(currentTime);

        synchronized (idlenessCheckLock) {
            notifyIdleness(currentTime,
                    getIdleTimeInMillis(IdleStatus.BOTH_IDLE),
                    IdleStatus.BOTH_IDLE, Math.max(stats.getLastIoTime(),
                            getLastIdleTime(IdleStatus.BOTH_IDLE)));

            notifyIdleness(currentTime,
                    getIdleTimeInMillis(IdleStatus.READER_IDLE),
                    IdleStatus.READER_IDLE, Math.max(stats.getLastReadTime(),
                            getLastIdleTime(IdleStatus.READER_IDLE)));

            notifyIdleness(currentTime,
                    getIdleTimeInMillis(IdleStatus.WRITER_IDLE),
                    IdleStatus.WRITER_IDLE, Math.max(stats.getLastWriteTime(),
                            getLastIdleTime(IdleStatus.WRITER_IDLE)));
        }
    }

    /**
     * TODO add documentation
     */
    private void notifyIdleness(long currentTime, long idleTime,
            IdleStatus status, long lastIoTime) {
        if (idleTime > 0 && lastIoTime != 0
                && currentTime - lastIoTime >= idleTime) {
            increaseIdleCount(status, currentTime);
            service.getListeners().fireServiceIdle(status);
        }
    }

    /**
     * TODO add documentation
     */
    protected void resetIdleCountForRead() {
        idleCountForBoth = 0;
        idleCountForRead = 0;
    }

    /**
     * TODO add documentation
     */
    protected void resetIdleCountForWrite() {
        idleCountForBoth = 0;
        idleCountForWrite = 0;
    }
}