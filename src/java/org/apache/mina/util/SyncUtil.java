/*
 *   @(#) $Id$
 * 
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.mina.util;

import EDU.oswego.cs.dl.util.concurrent.Sync;


/**
 * Utility class that acquires lock from Doug Lea's <code>Sync</code> not
 * throwing {@link InterruptedException}.
 *
 * @author Trustin Lee (trustin@gmail.com)
 * @version $Rev$, $Date$
 */
public class SyncUtil {
    private SyncUtil() {
    }

    public static final void acquire(Sync sync) {
        boolean wasInterrupted = Thread.interrupted(); // record and clear

        for (;;) {
            try {
                sync.acquire(); // or any other method throwing

                // InterruptedException
                break;
            } catch (InterruptedException ex) { // re-interrupted; try again
                wasInterrupted = true;
            }
        }

        if (wasInterrupted) { // re-establish interrupted state
            Thread.currentThread().interrupt();
        }
    }
}
