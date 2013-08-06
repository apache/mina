/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mina.service.idlechecker;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.api.IdleStatus;
import org.apache.mina.session.AbstractIoSession;
import org.apache.mina.session.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An session idle detector using an index in place of polling every session every seconds.<br>
 * 
 * For each session read/write event :<br>
 * <ul>
 * <li>we calculate what is the supposed future idle date</li>
 * <li>we round it at the next second</li>
 * <li>we store a reference to this session in a circular buffer like :</li>
 * </ul>
 * 
 * <pre>
 * 
 *               +--- Current time
 *               |
 *               v
 * +---+---+...+---+---+...+---+
 * | 0 | 1 |   | T |T+1|   |599|
 * +---+---+...+---+---+...+---+
 *               |   |
 *               |   +--> { S2, S7, S12...} (sessions that will TO in one second)
 *               +------> { S5, S6, S8...} (sessions that are idle for the maximum delay of 1 hour )
 * </pre>
 * 
 * The maximum idle itme is one hour.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class IndexedIdleChecker implements IdleChecker {
    /** Maximum idle time in second : default to 1 hour */
    private static final int MAX_IDLE_TIME_IN_SEC = 60 * 60;

    /** Maximum idle time in milliseconds : default to 1 hour */
    private static final long MAX_IDLE_TIME_IN_MS = MAX_IDLE_TIME_IN_SEC * 1000L;

    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger(IndexedIdleChecker.class);

    // A speedup for logs
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    private static final AttributeKey<Integer> READ_IDLE_INDEX = AttributeKey.createKey(Integer.class,
            "idle.read.index");

    private static final AttributeKey<Integer> WRITE_IDLE_INDEX = AttributeKey.createKey(Integer.class,
            "idle.write.index");

    private long lastCheckTimeMs = System.currentTimeMillis();

    @SuppressWarnings("unchecked")
    private final Set<AbstractIoSession>[] readIdleSessionIndex = new Set[MAX_IDLE_TIME_IN_SEC];

    @SuppressWarnings("unchecked")
    private final Set<AbstractIoSession>[] writeIdleSessionIndex = new Set[MAX_IDLE_TIME_IN_SEC];

    /** The elapsed period between two checks : 1 second */
    private static final int GRANULARITY_IN_MS = 1000;

    private final Worker worker = new Worker();

    private volatile boolean running = true;

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        worker.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        running = false;
        try {
            // interrupt the sleep
            worker.interrupt();
            // wait for worker to stop
            worker.join();
        } catch (InterruptedException e) {
            // interrupted, we don't care much
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionRead(AbstractIoSession session, long timeInMs) {
        if (IS_DEBUG) {
            LOG.debug("session read event, compute idle index of session {}", session);
        }

        // remove from the old index position
        Integer oldIndex = session.getAttribute(READ_IDLE_INDEX);

        if (oldIndex != null && readIdleSessionIndex[oldIndex] != null) {
            if (IS_DEBUG) {
                LOG.debug("remove for old index {}", oldIndex);
            }

            readIdleSessionIndex[oldIndex].remove(session);
        }

        long idleTimeInMs = session.getConfig().getIdleTimeInMillis(IdleStatus.READ_IDLE);

        // is idle enabled ?
        if (idleTimeInMs <= 0L) {
            if (IS_DEBUG) {
                LOG.debug("no read idle configuration");
            }
        } else {
            int nextIdleTimeInSeconds = (int) ((timeInMs + idleTimeInMs) / 1000L);
            int index = nextIdleTimeInSeconds % MAX_IDLE_TIME_IN_SEC;

            if (IS_DEBUG) {
                LOG.debug("computed index : {}", index);
            }

            if (readIdleSessionIndex[index] == null) {
                readIdleSessionIndex[index] = Collections
                        .newSetFromMap(new ConcurrentHashMap<AbstractIoSession, Boolean>());
            }

            if (IS_DEBUG) {
                LOG.debug("marking session {} idle for index {}", session, index);
            }

            readIdleSessionIndex[index].add(session);
            session.setAttribute(READ_IDLE_INDEX, index);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionWritten(AbstractIoSession session, long timeInMs) {
        if (IS_DEBUG) {
            LOG.debug("session write event, compute idle index of session {}", session);
        }

        // remove from the old index position
        Integer oldIndex = session.getAttribute(WRITE_IDLE_INDEX);

        if (oldIndex != null && writeIdleSessionIndex[oldIndex] != null) {
            if (IS_DEBUG) {
                LOG.debug("remove for old index {}", oldIndex);
            }

            writeIdleSessionIndex[oldIndex].remove(session);
        }

        long idleTimeInMs = session.getConfig().getIdleTimeInMillis(IdleStatus.WRITE_IDLE);

        // is idle enabled ?
        if (idleTimeInMs <= 0L) {
            if (IS_DEBUG) {
                LOG.debug("no write idle configuration");
            }
        } else {
            int nextIdleTimeInSeconds = (int) ((timeInMs + idleTimeInMs) / 1000L);
            int index = nextIdleTimeInSeconds % MAX_IDLE_TIME_IN_SEC;

            if (writeIdleSessionIndex[index] == null) {
                writeIdleSessionIndex[index] = Collections
                        .newSetFromMap(new ConcurrentHashMap<AbstractIoSession, Boolean>());
            }

            writeIdleSessionIndex[index].add(session);
            session.setAttribute(WRITE_IDLE_INDEX, index);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int processIdleSession(long timeMs) {
        int counter = 0;
        long delta = timeMs - lastCheckTimeMs;

        if (LOG.isTraceEnabled()) {
            LOG.trace("checking idle time, last = {}, now = {}, delta = {}", new Object[] { lastCheckTimeMs, timeMs,
                    delta });
        }

        if (delta < 1000) {
            LOG.debug("not a second between the last checks, abort");
            return 0;
        }

        // if (lastCheckTimeMs == 0) {
        // LOG.debug("first check, we start now");
        // lastCheckTimeMs = System.currentTimeMillis() - 1000;
        // }
        int startIdx = ((int) (Math.max(lastCheckTimeMs, timeMs - MAX_IDLE_TIME_IN_MS + 1) / 1000L))
                % MAX_IDLE_TIME_IN_SEC;
        int endIdx = ((int) (timeMs / 1000L)) % MAX_IDLE_TIME_IN_SEC;

        LOG.trace("scaning from index {} to index {}", startIdx, endIdx);

        int index = startIdx;
        do {

            LOG.trace("scanning index {}", index);
            // look at the read idle index
            counter += processIndex(readIdleSessionIndex, index, IdleStatus.READ_IDLE);
            counter += processIndex(writeIdleSessionIndex, index, IdleStatus.WRITE_IDLE);

            index = (index + 1) % MAX_IDLE_TIME_IN_SEC;
        } while (index != endIdx);

        // save last check time for next call
        lastCheckTimeMs = timeMs;
        LOG.trace("detected {} idleing sessions", counter);
        return counter;
    }

    private int processIndex(Set<AbstractIoSession>[] indexByTime, int position, IdleStatus status) {
        Set<AbstractIoSession> sessions = indexByTime[position];

        if (sessions == null) {
            return 0;
        }

        int counter = 0;

        for (AbstractIoSession idleSession : sessions) {
            idleSession.setAttribute(status == IdleStatus.READ_IDLE ? READ_IDLE_INDEX : WRITE_IDLE_INDEX, null);
            // check if idle detection wasn't disabled since the index update
            if (idleSession.getConfig().getIdleTimeInMillis(status) > 0) {
                idleSession.processSessionIdle(status);
            }
            counter++;
        }
        // clear the processed index entry
        indexByTime[position] = null;
        return counter;
    }

    /**
     * Thread in charge of checking the idleing sessions and fire events
     */
    private class Worker extends Thread {

        public Worker() {
            super("IdleChecker");
            setDaemon(true);
        }

        @Override
        public void run() {
            while (running) {
                try {
                    sleep(GRANULARITY_IN_MS);
                    processIdleSession(System.currentTimeMillis());
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}
