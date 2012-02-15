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

import java.util.HashSet;
import java.util.Set;

import org.apache.mina.api.IdleStatus;
import org.apache.mina.session.AbstractIoSession;
import org.apache.mina.session.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexedIdleChecker implements IdleChecker {

    private static int MAX_IDLE_TIME_IN_SEC = 60 * 60; // 1 hour max idle

    private static long MAX_IDLE_TIME_IN_MS = MAX_IDLE_TIME_IN_SEC * 1000L; // 1 hour max idle

    private static final Logger LOG = LoggerFactory.getLogger(IndexedIdleChecker.class);

    private static final AttributeKey<Integer> READ_IDLE_INDEX = AttributeKey.createKey(Integer.class,
            "idle.read.index");

    private static final AttributeKey<Integer> WRITE_IDLE_INDEX = AttributeKey.createKey(Integer.class,
            "idle.write.index");

    private long lastCheckTime = 0L;

    @SuppressWarnings("unchecked")
    private Set<AbstractIoSession>[] readIdleSessionIndex = new Set[MAX_IDLE_TIME_IN_SEC];

    @SuppressWarnings("unchecked")
    private Set<AbstractIoSession>[] writeIdleSessionIndex = new Set[MAX_IDLE_TIME_IN_SEC];

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionRead(AbstractIoSession session, long timeInMs) {
        LOG.debug("session read event, compute idle index of session {}", session);

        // remove from the old index position
        Integer oldIndex = session.getAttribute(READ_IDLE_INDEX);
        if (oldIndex != null && readIdleSessionIndex[oldIndex] != null) {
            LOG.debug("remove for old index {}", oldIndex);
            readIdleSessionIndex[oldIndex].remove(session);
        }

        long idleTimeInMs = session.getConfig().getIdleTimeInMillis(IdleStatus.READ_IDLE);
        // is idle enabled ?
        if (idleTimeInMs <= 0L) {
            LOG.debug("no read idle configuration");
        } else {
            int nextIdleTimeInSeconds = (int) ((timeInMs + idleTimeInMs) / 1000L);
            int index = nextIdleTimeInSeconds % MAX_IDLE_TIME_IN_SEC;
            if (readIdleSessionIndex[index] == null) {
                readIdleSessionIndex[index] = new HashSet<AbstractIoSession>();
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
        LOG.debug("session write event, compute idle index of session {}", session);

        // remove from the old index position
        Integer oldIndex = session.getAttribute(WRITE_IDLE_INDEX);
        if (oldIndex != null && writeIdleSessionIndex[oldIndex] != null) {
            LOG.debug("remove for old index {}", oldIndex);
            writeIdleSessionIndex[oldIndex].remove(session);
        }

        long idleTimeInMs = session.getConfig().getIdleTimeInMillis(IdleStatus.WRITE_IDLE);
        // is idle enabled ?
        if (idleTimeInMs <= 0L) {
            LOG.debug("no write idle configuration");
        } else {
            int nextIdleTimeInSeconds = (int) ((timeInMs + idleTimeInMs) / 1000L);
            int index = nextIdleTimeInSeconds % MAX_IDLE_TIME_IN_SEC;
            if (writeIdleSessionIndex[index] == null) {
                writeIdleSessionIndex[index] = new HashSet<AbstractIoSession>();
            }

            writeIdleSessionIndex[index].add(session);
            session.setAttribute(WRITE_IDLE_INDEX, index);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int processIdleSession(long time) {
        int counter = 0;
        long delta = time - lastCheckTime;

        if (LOG.isDebugEnabled()) {
            LOG.debug("checking idle time, last = {}, now = {}, delta = {}",
                    new Object[] { lastCheckTime, time, delta });
        }

        if (delta < 1000) {
            LOG.debug("not a second between the last checks, abort");
            return 0;
        }

        int startIdx = ((int) (Math.max(lastCheckTime, time - MAX_IDLE_TIME_IN_MS) / 1000L)) % MAX_IDLE_TIME_IN_SEC;
        int endIdx = ((int) (time / 1000L)) % MAX_IDLE_TIME_IN_SEC;

        for (int index = startIdx; index != endIdx; index = (index + 1) % MAX_IDLE_TIME_IN_SEC) {
            // look at the read idle index
            counter += processIndex(readIdleSessionIndex, index, IdleStatus.READ_IDLE);
            counter += processIndex(writeIdleSessionIndex, index, IdleStatus.WRITE_IDLE);

        }
        // save last check time for next call
        lastCheckTime = time;
        LOG.debug("detected {} idleing sessions", counter);
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
}
