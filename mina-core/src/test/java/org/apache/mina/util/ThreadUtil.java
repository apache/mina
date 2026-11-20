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
package org.apache.mina.util;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import static org.junit.Assert.fail;

public final class ThreadUtil {

    private ThreadUtil() {
    }

    public static void assertNoDeadlock() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreadsIDs = bean.findDeadlockedThreads();

        if (deadlockedThreadsIDs != null && deadlockedThreadsIDs.length > 0) {
            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append("Deadlock detected:\n");

            for (int i = 0; i < deadlockedThreadsIDs.length; i++) {
                long id = deadlockedThreadsIDs[i];
                ThreadInfo threadInfo = bean.getThreadInfo(id);
                errorMessage.append(format(threadInfo)).append('\n');
            }

            fail(errorMessage.toString());
        }
    }

    private static String format(ThreadInfo threadInfo) {
        StringBuilder out = new StringBuilder();

        out.append('"').append(threadInfo.getThreadName()).append('"');
        out.append(' ');
        out.append("priority=").append(threadInfo.getPriority());
        out.append(' ');
        out.append("id=").append(threadInfo.getThreadId());
        out.append(' ');
        out.append(threadInfo.getThreadState());

        LockInfo lockInfo = threadInfo.getLockInfo();

        if (lockInfo != null) {
            out.append(" on ");
            out.append(lockInfo.getClassName());
            out.append('@');
            out.append(Integer.toHexString(lockInfo.getIdentityHashCode()));
        }

        long lockOwnerId = threadInfo.getLockOwnerId();

        if (lockOwnerId != -1) {
            out.append(" owned by ");
            out.append("\"").append(threadInfo.getLockOwnerName()).append('\"');
            out.append(' ');
            out.append(lockOwnerId);
        }

        return out.toString();
    }
}
