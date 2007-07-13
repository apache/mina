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
 * The collected stats for a session. It's used by {@link StatCollector} to attach
 * throughput stats to an {@link IoSession}. You can accces a session stat using 
 * {@link IoSession} getAttribute method :
 * <pre>
 * IoSession session = ...
 * IoSessionStat stat = session.getAttribute( StatCollector.KEY );
 * </pre>
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IoSessionStat {
    long lastByteRead = -1;

    long lastByteWrite = -1;

    long lastMessageRead = -1;

    long lastMessageWrite = -1;

    float byteWrittenThroughput = 0;

    float byteReadThroughput = 0;

    float messageWrittenThroughput = 0;

    float messageReadThroughput = 0;

    //  last time the session was polled
    long lastPollingTime = System.currentTimeMillis();

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
    public float getByteWrittenThroughput() {
        return byteWrittenThroughput;
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
    public float getMessageWrittenThroughput() {
        return messageWrittenThroughput;
    }

    /**
     * used for the StatCollector, last polling value 
     */
    long getLastByteRead() {
        return lastByteRead;
    }

    /**
     * used for the StatCollector, last polling value 
     */
    long getLastByteWrite() {
        return lastByteWrite;
    }

    /**
     * used for the StatCollector, last polling value 
     */
    long getLastMessageRead() {
        return lastMessageRead;
    }

    /**
     * used for the StatCollector, last polling value 
     */
    long getLastMessageWrite() {
        return lastMessageWrite;
    }
}