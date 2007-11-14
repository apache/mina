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
package org.apache.mina.integration.jmx;

import java.util.Date;
import java.util.List;

import org.apache.mina.common.AbstractIoSession;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.management.IoStatistics;
import org.apache.mina.management.IoStatisticsCollector;

/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class IoSessionManager implements IoSessionManagerMBean {

    private IoSession session;

    /**
     * create the session manager
     * @param session the MINA's session to manage
     */
    public IoSessionManager(IoSession session) {
        this.session = session;
    }

    public boolean isConnected() {
        return session.isConnected();
    }

    public long getReadBytes() {
        return session.getReadBytes();
    }

    public long getWrittenBytes() {
        return session.getWrittenBytes();
    }

    public long getReadMessages() {
        return ((AbstractIoSession) session).getReadMessages();
    }

    public long getWrittenMessages() {
        return ((AbstractIoSession) session).getWrittenMessages();
    }

    public void close() {
        session.close().awaitUninterruptibly();
    }

    public Date getCreationTime() {
        return new Date(session.getCreationTime());
    }

    public Date getLastIoTime() {
        return new Date(session.getLastIoTime());
    }

    public Date getLastReadTime() {
        return new Date(session.getLastReadTime());
    }

    public Date getLastWriteTime() {
        return new Date(session.getLastWriteTime());
    }

    public String[] getInstalledFilters() {
        List<IoFilterChain.Entry> filters = session.getFilterChain().getAll();
        String[] res = new String[filters.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = filters.get(i).getName();
        }
        return res;
    }

    public void addLastLoggingFilter() {
        LoggingFilter f = new LoggingFilter();
        session.getFilterChain().addLast("LoggerLast", f);
    }

    public void removeLastLoggingFilter() {
        session.getFilterChain().remove("LoggerLast");
    }

    public void addFirstLoggingFilter() {
        LoggingFilter f = new LoggingFilter();
        session.getFilterChain().addFirst("LoggerFirst", f);
    }

    public void removeFirstLoggingFilter() {
        session.getFilterChain().remove("LoggerFirst");
    }

    public float getByteReadThroughtput() {
        IoStatistics stats = (IoStatistics) session
                .getAttribute(IoStatisticsCollector.STATISTICS);
        if (stats == null) {
            return Float.NaN;
        } else {
            return stats.getByteReadThroughput();
        }
    }

    public float getByteWriteThroughtput() {
        IoStatistics stats = (IoStatistics) session
                .getAttribute(IoStatisticsCollector.STATISTICS);
        if (stats == null) {
            return Float.NaN;
        } else {
            return stats.getByteWriteThroughput();
        }
    }

    public float getMessageReadThroughtput() {
        IoStatistics stats = (IoStatistics) session
                .getAttribute(IoStatisticsCollector.STATISTICS);
        if (stats == null) {
            return Float.NaN;
        } else {
            return stats.getMessageReadThroughput();
        }
    }

    public float getMessageWriteThroughtput() {
        IoStatistics stats = (IoStatistics) session
                .getAttribute(IoStatisticsCollector.STATISTICS);
        if (stats == null) {
            return Float.NaN;
        } else {
            return stats.getMessageWriteThroughput();
        }
    }
}
