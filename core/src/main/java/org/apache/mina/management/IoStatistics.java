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
package org.apache.mina.management;

import org.apache.mina.common.IoSession;

/**
 * The collected stats for a session. It's used by {@link IoStatisticsCollector}
 * to attach throughput stats to an {@link IoSession}. You can access a session 
 * stat using {@link IoSession#getAttribute(Object)} method:
 * <pre>
 * IoSession session = ...
 * IoStatistics stat = session.getAttribute( IoStatisticsCollector.STATICTICS );
 * </pre>
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class IoStatistics {
    
    private long lastReadBytes = 0;
    private long lastWrittenBytes = 0;
    private long lastReadMessages = 0;
    private long lastWrittenMessages = 0;
    private float byteWriteThroughput = 0;
    private float byteReadThroughput = 0;
    private float messageWriteThroughput = 0;
    private float messageReadThroughput = 0;
    // last time the session was polled
    private long lastPollingTime = System.currentTimeMillis();

    /**
     * Bytes read per second
     * @return bytes per second
     */
    public float getByteReadThroughput() {
        return byteReadThroughput;
    }

    /**
     * Bytes written per second
     * @return bytes per second
     */
    public float getByteWriteThroughput() {
        return byteWriteThroughput;
    }

    /**
     * Messages read per second
     * @return messages per second
     */
    public float getMessageReadThroughput() {
        return messageReadThroughput;
    }

    /**
     * Messages written per second
     * @return messages per second
     */
    public float getMessageWriteThroughput() {
        return messageWriteThroughput;
    }

    void setLastReadBytes(long lastReadBytes) {
        this.lastReadBytes = lastReadBytes;
    }

    void setLastWrittenBytes(long lastWrittenBytes) {
        this.lastWrittenBytes = lastWrittenBytes;
    }

    void setLastReadMessages(long lastReadMessages) {
        this.lastReadMessages = lastReadMessages;
    }

    void setLastWrittenMessages(long lastWrittenMessages) {
        this.lastWrittenMessages = lastWrittenMessages;
    }

    void setByteWriteThroughput(float byteWriteThroughput) {
        this.byteWriteThroughput = byteWriteThroughput;
    }

    void setByteReadThroughput(float byteReadThroughput) {
        this.byteReadThroughput = byteReadThroughput;
    }

    void setMessageWriteThroughput(float messageWriteThroughput) {
        this.messageWriteThroughput = messageWriteThroughput;
    }

    void setMessageReadThroughput(float messageReadThroughput) {
        this.messageReadThroughput = messageReadThroughput;
    }

    /**
     * used for the StatCollector, last polling value
     */
    long getLastReadBytes() {
        return lastReadBytes;
    }

    /**
     * used for the StatCollector, last polling value
     */
    long getLastWrittenBytes() {
        return lastWrittenBytes;
    }

    /**
     * used for the StatCollector, last polling value
     */
    long getLastReadMessages() {
        return lastReadMessages;
    }

    /**
     * used for the StatCollector, last polling value
     */
    long getLastWrittenMessages() {
        return lastWrittenMessages;
    }

    long getLastPollingTime() {
        return lastPollingTime;
    }

    void setLastPollingTime(long lastPollingTime) {
        this.lastPollingTime = lastPollingTime;
    }
}