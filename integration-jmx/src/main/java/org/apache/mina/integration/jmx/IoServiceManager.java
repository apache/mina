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

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.management.IoStatisticsCollector;

/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class IoServiceManager implements IoServiceManagerMBean,
        MBeanRegistration {
    private final IoService service;
    private volatile IoStatisticsCollector collector;
    private volatile int pollingInterval;
    private final boolean autoStart;

    public IoServiceManager(
            IoService service, int pollingInterval, boolean autoStart) {
        this.autoStart = autoStart;
        this.service = service;
        this.pollingInterval = pollingInterval;
    }

    public IoServiceManager(IoService service, int pollingInterval) {
        this(service, pollingInterval, false);
    }

    public IoServiceManager(IoService service) {
        this(service, 5000, false);
    }

    public int getManagedSessionCount() {
        return service.getManagedSessions().size();
    }

    public void start() {
        if (collector != null && collector.isRunning()) {
            throw new IllegalStateException("Already collecting stats");
        }

        collector = new IoStatisticsCollector(service, pollingInterval);
        collector.start();
    }

    public int getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(int pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    public void stop() {
        if (collector != null && collector.isRunning()) {
            collector.stop();
        }

    }

    public float getTotalByteReadThroughput() {
        return collector.getByteReadThroughput();
    }

    public float getTotalByteWrittenThroughput() {
        return collector.getByteWriteThroughput();
    }

    public float getTotalMessageReadThroughput() {
        return collector.getMessageReadThroughput();
    }

    public float getTotalMessageWrittenThroughput() {
        return collector.getMessageWriteThroughput();
    }

    public float getAverageByteReadThroughput() {
        return collector.getByteReadThroughput() / collector.getSessionCount();
    }

    public float getAverageByteWrittenThroughput() {
        return collector.getByteWriteThroughput()
                / collector.getSessionCount();
    }

    public float getAverageMessageReadThroughput() {
        return collector.getMessageReadThroughput() / collector.getSessionCount();
    }

    public float getAverageMessageWrittenThroughput() {
        return collector.getMessageWriteThroughput()
                / collector.getSessionCount();
    }

    public void closeAllSessions() {
        for (Object element : service.getManagedSessions()) {
            IoSession session = (IoSession) element;
            session.close();
        }
    }

    public ObjectName preRegister(MBeanServer server, ObjectName name)
            throws Exception {
        return name;
    }

    public void postRegister(Boolean registrationDone) {
        if (registrationDone.booleanValue()) {
            if (autoStart) {
                start();
            }

        }
    }

    public void preDeregister() throws Exception {
        if (collector != null && collector.isRunning()) {
            stop();
        }
    }

    public void postDeregister() {
    }
}