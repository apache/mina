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

/**
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoServiceManagerMBean {
    /**
     * amount of session currently managed
     * @return session count
     */
    int getManagedSessionCount();

    /**
     * start collecting throughput statistics for all the managed sessions 
     * @param millisecondsPolling polling time in milliseconds like 5000 for computing throughput every 5 seconds
     */
    void startCollectingStats(int millisecondsPolling);

    /**
     * stop collecting throughput statistics 
     */
    void stopCollectingStats();

    /**
     * bytes read per seconds sum of all the managed sessions  
     * @return bytes per seconds
     */
    float getTotalByteReadThroughput();

    /**
     * bytes written per seconds sum for all the managed sessions  
     * @return bytes per seconds
     */
    float getTotalByteWrittenThroughput();

    /**
     * messages read per seconds sum of all the managed sessions  
     * @return messages per seconds
     */
    float getTotalMessageReadThroughput();

    /**
     * messages written per seconds sum for all the managed sessions  
     * @return messages per seconds
     */
    float getTotalMessageWrittenThroughput();

    /**
     * average bytes read per seconds for all the managed sessions  
     * @return bytes per seconds
     */
    float getAverageByteReadThroughput();

    /**
     * average bytes written per seconds for all the managed sessions  
     * @return bytes per seconds
     */
    float getAverageByteWrittenThroughput();

    /**
     * average messages read per seconds for all the managed sessions  
     * @return messages per seconds
     */
    float getAverageMessageReadThroughput();

    /**
     * average messages written per seconds for all the managed sessions  
     * @return messages per seconds
     */
    float getAverageMessageWrittenThroughput();

    /**
     * close all the managed sessions
     */
    void closeAllSessions();

}