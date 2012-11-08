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

    private static int MAX_IDLE_TIME_IN_SEC = 60 * 60; // 1 hour max idle

    private static long MAX_IDLE_TIME_IN_MS = MAX_IDLE_TIME_IN_SEC * 1000L; // 1 hour max idle

    private static final Logger LOG = LoggerFactory.getLogger(IndexedIdleChecker.class);

    private static final AttributeKey<Integer> READ_IDLE_INDEX = AttributeKey.createKey(Integer.class,
            "idle.read.index");

    private static final AttributeKey<Integer> WRITE_IDLE_INDEX = AttributeKey.createKey(Integer.class,
            "idle.write.index");

    private long lastCheckTimeMs = 0L;

    @SuppressWarnings("unchecked")
    private final Set<AbstractIoSession>[] readIdleSessionIndex = new Set[MAX_IDLE_TIME_IN_SEC];

    @SuppressWarnings("unchecked")
    private final Set<AbstractIoSession>[] writeIdleSessionIndex = new Set[MAX_IDLE_TIME_IN_SEC];

    private final int granularityInMs = 1000;

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
        } catch (final InterruptedException e) {
            // interrupted, we don't care much
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionRead(final AbstractIoSession session, final long timeInMs) {
        LOG.debug("session read event, compute idle index of session {}", session);

        // remove from the old index position
        final Integer oldIndex = session.getAttribute(READ_IDLE_INDEX);
        if (oldIndex != null && readIdleSessionIndex[oldIndex] != null) {
            LOG.debug("remove for old index {}", oldIndex);
            readIdleSessionIndex[oldIndex].remove(session);
        }

        final long idleTimeInMs = session.getConfig().getIdleTimeInMillis(IdleStatus.READ_IDLE);
        // is idle enabled ?
        if (idleTimeInMs <= 0L) {
            LOG.debug("no read idle configuration");
        } else {
            final int nextIdleTimeInSeconds = (int) ((timeInMs + idleTimeInMs) / 1000L) + 1;
            final int index = nextIdleTimeInSeconds % MAX_IDLE_TIME_IN_SEC;
            if (readIdleSessionIndex[index] == null) {
                readIdleSessionIndex[index] = Collections
                        .newSetFromMap(new ConcurrentHashMap<AbstractIoSession, Boolean>());
            }

            LOG.debug("marking session {} idle for index {}", session, index);
            readIdleSessionIndex[index].add(session);
            session.setAttribute(READ_IDLE_INDEX, index);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionWritten(final AbstractIoSession session, final long timeInMs) {
        LOG.debug("session write event, compute idle index of session {}", session);

        // remove from the old index position
        final Integer oldIndex = session.getAttribute(WRITE_IDLE_INDEX);
        if (oldIndex != null && writeIdleSessionIndex[oldIndex] != null) {
            LOG.debug("remove for old index {}", oldIndex);
            writeIdleSessionIndex[oldIndex].remove(session);
        }

        final long idleTimeInMs = session.getConfig().getIdleTimeInMillis(IdleStatus.WRITE_IDLE);
        // is idle enabled ?
        if (idleTimeInMs <= 0L) {
            LOG.debug("no write idle configuration");
        } else {
            final int nextIdleTimeInSeconds = (int) ((timeInMs + idleTimeInMs) / 1000L) + 1;
            final int index = nextIdleTimeInSeconds % MAX_IDLE_TIME_IN_SEC;
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
    public int processIdleSession(final long timeMs) {
        int counter = 0;
        final long delta = timeMs - lastCheckTimeMs;

        if (LOG.isDebugEnabled()) {
            LOG.debug("checking idle time, last = {}, now = {}, delta = {}", new Object[] { lastCheckTimeMs, timeMs,
                                    delta });
        }

        if (delta < 1000) {
            LOG.debug("not a second between the last checks, abort");
            return 0;
        }

        final int startIdx = ((int) (Math.max(lastCheckTimeMs, timeMs - MAX_IDLE_TIME_IN_MS + 1) / 1000L))
                % MAX_IDLE_TIME_IN_SEC;
        final int endIdx = ((int) (timeMs / 1000L)) % MAX_IDLE_TIME_IN_SEC;

        LOG.debug("scaning from index {} to index {}", startIdx, endIdx);

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
        LOG.debug("detected {} idleing sessions", counter);
        return counter;
    }

    private int processIndex(final Set<AbstractIoSession>[] indexByTime, final int position, final IdleStatus status) {
        final Set<AbstractIoSession> sessions = indexByTime[position];
        if (sessions == null) {
            return 0;
        }

        int counter = 0;

        for (final AbstractIoSession idleSession : sessions) {
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
                    sleep(granularityInMs);
                    processIdleSession(System.currentTimeMillis());
                } catch (final InterruptedException e) {
                    break;
                }
            }
        }
    }
}
