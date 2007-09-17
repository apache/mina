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
import org.apache.mina.management.IoSessionStat;
import org.apache.mina.management.StatCollector;

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

    /**
     * @see archean.util.mina.IoSessionManagerMBean#isConnected()
     */
    public boolean isConnected() {
        return session.isConnected();
    }

    /**
     * @see archean.util.mina.IoSessionManagerMBean#getReadBytes()
     */
    public long getReadBytes() {
        return session.getReadBytes();
    }

    /**
     * @see archean.util.mina.IoSessionManagerMBean#getWrittenBytes()
     */
    public long getWrittenBytes() {
        return session.getWrittenBytes();
    }

    public long getReadMessages() {
        return ((AbstractIoSession) session).getReadMessages();
    }

    public long getWrittenMessages() {
        return ((AbstractIoSession) session).getWrittenMessages();
    }

    /**
     * @see archean.util.mina.IoSessionManagerMBean#close()
     */
    public void close() {
        session.close().awaitUninterruptibly();
    }

    /**
     * @see archean.util.mina.IoSessionManagerMBean#getCreationTime()
     */
    public Date getCreationTime() {
        return new Date(session.getCreationTime());
    }

    /**
     * @see archean.util.mina.IoSessionManagerMBean#getLastIoTime()
     */
    public Date getLastIoTime() {
        return new Date(session.getLastIoTime());
    }

    /**
     * @see archean.util.mina.IoSessionManagerMBean#getLastReadTime()
     */
    public Date getLastReadTime() {
        return new Date(session.getLastReadTime());
    }

    /**
     * @see archean.util.mina.IoSessionManagerMBean#getLastWriteTime()
     */
    public Date getLastWriteTime() {
        return new Date(session.getLastWriteTime());
    }

    /**
     * @see archean.util.mina.IoSessionManagerMBean#getInstalledFilters()
     */
    public String[] getInstalledFilters() {
        List<IoFilterChain.Entry> filters = session.getFilterChain().getAll();
        String[] res = new String[filters.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = filters.get(i).getName();
        }
        return res;
    }

    /**
     * @see archean.util.mina.IoSessionManagerMBean#addLastLoggingFilter()
     */
    public void addLastLoggingFilter() {
        LoggingFilter f = new LoggingFilter();
        session.getFilterChain().addLast("LoggerLast", f);
    }

    /**
     * @see archean.util.mina.IoSessionManagerMBean#removeLastLoggingFilter()
     */
    public void removeLastLoggingFilter() {

        session.getFilterChain().remove("LoggerLast");
    }

    /**
     * @see archean.util.mina.IoSessionManagerMBean#addFirstLoggingFilter()
     */
    public void addFirstLoggingFilter() {
        LoggingFilter f = new LoggingFilter();
        session.getFilterChain().addFirst("LoggerFirst", f);
    }

    public void removeFirstLoggingFilter() {

        session.getFilterChain().remove("LoggerFirst");
    }

    public float getByteReadThroughtput() {
        IoSessionStat stats = (IoSessionStat) session
                .getAttribute(StatCollector.KEY);
        if (stats == null) {
            return Float.NaN;
        } else {
            return stats.getByteReadThroughput();
        }
    }

    public float getByteWrittenThroughtput() {
        IoSessionStat stats = (IoSessionStat) session
                .getAttribute(StatCollector.KEY);
        if (stats == null) {
            return Float.NaN;
        } else {
            return stats.getByteWrittenThroughput();
        }
    }

    public float getMessageReadThroughtput() {
        IoSessionStat stats = (IoSessionStat) session
                .getAttribute(StatCollector.KEY);
        if (stats == null) {
            return Float.NaN;
        } else {
            return stats.getMessageReadThroughput();
        }
    }

    public float getMessageWrittenThroughtput() {
        IoSessionStat stats = (IoSessionStat) session
                .getAttribute(StatCollector.KEY);
        if (stats == null) {
            return Float.NaN;
        } else {
            return stats.getMessageWrittenThroughput();
        }
    }
}
