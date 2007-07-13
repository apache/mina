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
 * Represents the type of idleness of {@link IoSession} or
 * {@link IoSession}.  There are three types of idleness:
 * <ul>
 *   <li>{@link #READER_IDLE} - No data is coming from the remote peer.</li>
 *   <li>{@link #WRITER_IDLE} - Session is not writing any data.</li>
 *   <li>{@link #BOTH_IDLE} - Both {@link #READER_IDLE} and {@link #WRITER_IDLE}.</li>
 * </ul>
 * <p>
 * Idle time settings are all disabled by default.  You can enable them
 * using {@link IoSession#setIdleTime(IdleStatus,int)}.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IdleStatus {
    /**
     * Represents the session status that no data is coming from the remote
     * peer.
     */
    public static final IdleStatus READER_IDLE = new IdleStatus("reader idle");

    /**
     * Represents the session status that the session is not writing any data.
     */
    public static final IdleStatus WRITER_IDLE = new IdleStatus("writer idle");

    /**
     * Represents both {@link #READER_IDLE} and {@link #WRITER_IDLE}.
     */
    public static final IdleStatus BOTH_IDLE = new IdleStatus("both idle");

    private final String strValue;

    /**
     * Creates a new instance.
     */
    private IdleStatus(String strValue) {
        this.strValue = strValue;
    }

    /**
     * Returns the string representation of this status.
     * <ul>
     *   <li>{@link #READER_IDLE} - <tt>"reader idle"</tt></li>
     *   <li>{@link #WRITER_IDLE} - <tt>"writer idle"</tt></li>
     *   <li>{@link #BOTH_IDLE} - <tt>"both idle"</tt></li>
     * </ul>
     */
    public String toString() {
        return strValue;
    }
}