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

import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.common.AttributeKey;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceListener;
import org.apache.mina.common.IoSession;

/**
 * Collects statistics of an {@link IoService}. It's polling all the sessions
 * of a given IoService. It's attaching a {@link IoStatistics} object to all
 * the sessions polled and filling the throughput values.
 *
 * Usage :
 * <pre>
 * IoService service = ...
 * IoStatisticsCollector collector = new IoStatisticsCollector( service );
 * collector.start();
 * </pre>
 *
 * By default the {@link IoStatisticsCollector} is polling the sessions every 5
 * seconds. You can give a different polling time using a second constructor.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class IoStatisticsCollector {
    public static final AttributeKey STATISTICS = 
        new AttributeKey(IoStatisticsCollector.class, "statistics");

    private static volatile int nextId = 0;

    private final int id = nextId++;
    private final IoService service;
    private int pollingInterval = 5000;
    private Worker worker;

    // resume of session stats, for simplifying access to the statistics
    private final AtomicLong totalProcessedSessions = new AtomicLong();
    private final IoStatistics serviceStatistics = new IoStatistics();

    private final IoServiceListener serviceListener = new IoServiceListener() {
        public void serviceActivated(IoService service) {
        }

        public void serviceDeactivated(IoService service) {
        }

        public void sessionCreated(IoSession session) {
            addSession(session);
        }

        public void sessionDestroyed(IoSession session) {
            removeSession(session);
        }
    };

    /**
     * Create a stat collector for the given service with a default polling time of 5 seconds.
     * @param service the IoService to inspect
     */
    public IoStatisticsCollector(IoService service) {
        this(service, 5000);
    }

    /**
     * create a stat collector for the given given service
     * @param service the IoService to inspect
     * @param pollingInterval milliseconds
     */
    public IoStatisticsCollector(IoService service, int pollingInterval) {
        this.service = service;
        this.pollingInterval = pollingInterval;
    }

    /**
     * Start collecting stats for the {@link IoSession} of the service.
     * New sessions or destroyed will be automaticly added or removed.
     */
    public void start() {
        synchronized (this) {
            if (worker != null && worker.isAlive()) {
                throw new RuntimeException("Stat collecting already started");
            }

            for (IoSession ioSession : service.getManagedSessions()) {
                addSession(ioSession);
            }

            // listen for new ones
            service.addListener(serviceListener);

            // start polling
            worker = new Worker();
            worker.start();

        }

    }

    /**
     * Stop collecting stats. all the {@link IoStatistics} object will be removed of the
     * polled session attachements.
     */
    public void stop() {
        synchronized (this) {
            if (worker == null) {
                return;
            }

            service.removeListener(serviceListener);

            // stop worker
            worker.stop = true;
            worker.interrupt();
            while (worker.isAlive()) {
                try {
                    worker.join();
                } catch (InterruptedException e) {
                    //ignore since this is shutdown time
                }
            }

            for (IoSession session: service.getManagedSessions()) {
                session.removeAttribute(STATISTICS);
            }

            worker = null;
        }
    }

    /**
     * is the stat collector started and polling the {@link IoSession} of the {@link IoService}
     * @return true if started
     */
    public boolean isRunning() {
        synchronized (this) {
            return worker != null && worker.stop != true;
        }
    }

    private void addSession(IoSession session) {
        session.setAttribute(STATISTICS, new IoStatistics());
        totalProcessedSessions.incrementAndGet();
    }

    private void removeSession(IoSession session) {
        session.removeAttribute(STATISTICS);
    }

    /**
     * total number of sessions processed by the stat collector
     * @return number of sessions
     */
    public long getTotalProcessedSessions() {
        return totalProcessedSessions.get();
    }

    public float getByteReadThroughput() {
        return serviceStatistics.getByteReadThroughput();
    }

    public float getByteWriteThroughput() {
        return serviceStatistics.getByteWriteThroughput();
    }

    public float getMessageReadThroughput() {
        return serviceStatistics.getMessageReadThroughput();
    }

    public float getMessageWriteThroughput() {
        return serviceStatistics.getMessageWriteThroughput();
    }

    public long getSessionCount() {
        return service.getManagedSessions().size();
    }

    private class Worker extends Thread {

        boolean stop = false;

        private Worker() {
            super("StatCollectorWorker-" + id);
        }

        @Override
        public void run() {
            // Initialize...
            serviceStatistics.setLastReadBytes(service.getReadBytes());
            serviceStatistics.setLastWrittenBytes(service.getWrittenBytes());
            serviceStatistics.setLastReadMessages(service.getReadMessages());
            serviceStatistics.setLastWrittenMessages(service.getWrittenMessages());
            serviceStatistics.setLastPollingTime(System.currentTimeMillis());
            
            for (IoSession session: service.getManagedSessions()) {
                IoStatistics statistics =
                    (IoStatistics) session.getAttribute(STATISTICS);

                statistics.setLastReadBytes(session.getReadBytes());
                statistics.setLastWrittenBytes(session.getWrittenBytes());
                statistics.setLastReadMessages(session.getReadMessages());
                statistics.setLastWrittenMessages(session.getWrittenMessages());
            }

            while (!stop) {
                // wait polling time
                try {
                    Thread.sleep(pollingInterval);
                } catch (InterruptedException e) {
                }
                
                long readBytes, writtenBytes, readMessages, writtenMessages;
                
                // Calculate service throughput.
                readBytes = service.getReadBytes();
                writtenBytes = service.getWrittenBytes();
                readMessages = service.getReadMessages();
                writtenMessages = service.getWrittenMessages();
                
                serviceStatistics.setLastPollingTime(System.currentTimeMillis());
                serviceStatistics.setByteReadThroughput(
                        (readBytes - serviceStatistics.getLastReadBytes()) /
                        (pollingInterval / 1000f));
                serviceStatistics.setByteWriteThroughput(
                        (writtenBytes - serviceStatistics.getLastWrittenBytes()) /
                        (pollingInterval / 1000f));
                serviceStatistics.setMessageReadThroughput(
                        (readMessages - serviceStatistics.getLastReadMessages()) /
                        (pollingInterval / 1000f));
                serviceStatistics.setMessageWriteThroughput(
                        (writtenMessages - serviceStatistics.getLastWrittenMessages()) /
                        (pollingInterval / 1000f));
                serviceStatistics.setLastReadBytes(readBytes);
                serviceStatistics.setLastWrittenBytes(writtenBytes);
                serviceStatistics.setLastReadMessages(readMessages);
                serviceStatistics.setLastWrittenMessages(writtenMessages);

                // Calculate session throughput.
                for (IoSession session: service.getManagedSessions()) {
                    IoStatistics statistics =
                        (IoStatistics) session.getAttribute(STATISTICS);
                    if (statistics == null) {
                        continue;
                    }

                    statistics.setLastPollingTime(System.currentTimeMillis());
                    statistics.setByteReadThroughput(
                            (readBytes - statistics.getLastReadBytes()) /
                            (pollingInterval / 1000f));
                    statistics.setByteWriteThroughput(
                            (writtenBytes - statistics.getLastWrittenBytes()) /
                            (pollingInterval / 1000f));
                    statistics.setMessageReadThroughput(
                            (readMessages - statistics.getLastReadMessages()) /
                            (pollingInterval / 1000f));
                    statistics.setMessageWriteThroughput(
                            (writtenMessages - statistics.getLastWrittenMessages()) /
                            (pollingInterval / 1000f));
                    statistics.setLastReadBytes(readBytes);
                    statistics.setLastWrittenBytes(writtenBytes);
                    statistics.setLastReadMessages(readMessages);
                    statistics.setLastWrittenMessages(writtenMessages);
                }
            }
        }
    }
}