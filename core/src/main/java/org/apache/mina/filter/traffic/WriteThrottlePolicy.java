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
package org.apache.mina.filter.traffic;

import org.apache.mina.common.WriteFuture;

/**
 * Tells {@link WriteThrottleFilter} what to do when there are too many
 * scheduled write requests in the session buffer.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public enum WriteThrottlePolicy {
    /**
     * Do nothing; disables the filter.
     */
    OFF,
    /**
     * Log a warning message, but doesn't limit anything.
     * The warning message is logged at most every 3 seconds to avoid
     * log flooding.
     */
    LOG,
    /**
     * Block the write operation until the size of write request buffer
     * is full.  You must use this policy in conjunction with the
     * {@link ReadThrottleFilterChainBuilder} to prevent the
     * {@link OutOfMemoryError} on the reader side.
     */
    BLOCK,
    /**
     * Make the write operation fail immediately.
     * The '{@link WriteFuture#getException() exception}' property of
     * the returned {@link WriteFuture} will be {@link WriteFloodException}.
     * The exception will also be notified as an <tt>exceptionCaught</tt>
     * event.
     */
    FAIL,
}
