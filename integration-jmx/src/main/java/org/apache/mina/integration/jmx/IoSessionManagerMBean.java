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

/**
 * MBean interface for the session manager, it's used for instrumenting IoSession
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoSessionManagerMBean {
    /**
     * is the session is connected
     * @return connection status
     */
    public boolean isConnected();

    /**
     * bytes read from the beginning
     * @return total of bytes read
     */
    public long getReadBytes();

    /**
     * bytes written from the beginning
     * @return total of bytes written
     */
    public long getWrittenBytes();

    /**
     * PDU decoded from the beginning. Only revelent if a ProtocolCodecFilter is installed.
     * @return Number of read messages
     */
    public long getReadMessages();

    /**
     * PDU encoded from the beginning. Only revelent if a ProtocolCodecFilter is installed.
     * @return Number of written messages
     */
    public long getWrittenMessages();

    /**
     * close the session
     */
    public void close() throws InterruptedException;

    /**
     * when the session was created
     * @return the date of session creation
     */
    public Date getCreationTime();

    /**
     * last time the session processed an IO
     * @return date of last IO
     */
    public Date getLastIoTime();

    /**
     * last time the session processed a write
     * @return date of last write
     */
    public Date getLastWriteTime();

    /**
     * last time the session processed an read
     * @return date of last read
     */
    public Date getLastReadTime();

    /**
     * get the list of filters installed in the filter chain
     * @return array of filter names
     */
    public String[] getInstalledFilters();

    /**
     * add a logging filter at end of the chain
     */
    public void addLastLoggingFilter();

    /**
     * remove the logging filter at end of the chain
     */
    public void removeLastLoggingFilter();

    /**
     * add a logging filter at begining of the chain
     */
    public void addFirstLoggingFilter();

    /**
     * remove the logging filter at begining of the chain
     */
    public void removeFirstLoggingFilter();

    /**
     * read and write IDLE time
     * @return idle time in milli-seconds
     */
    public long getBothIdleTime();

    /**
     * read IDLE time
     * @return read idle time in milli-seconds
     */
    public long getReadIdleTime();

    /**
     * write IDLE time
     * @return write idle time in milli-seconds
     */
    public long getWriteIdleTime();

    /**
     * get the read bytes per second throughput
     * works only if a stat collector is inspecting this session,
     * @return read bytes per seconds
     */
    public float getByteReadThroughtput();

    /**
     * get the written bytes per second throughput
     * works only if a stat collector is inspecting this session,
     * @return written bytes per seconds
     */
    public float getByteWrittenThroughtput();

    /**
     * get the read messages per second throughput
     * works only if a stat collector is inspecting this session, and only if a ProtocolDecoderFilter is used
     * @return read messages per seconds
     */
    public float getMessageReadThroughtput();

    /**
     * get the written messages per second throughput
     * works only if a stat collector is inspecting this session, and only if a ProtocolDecoderFilter is used
     * @return written messages per seconds
     */
    public float getMessageWrittenThroughtput();

}