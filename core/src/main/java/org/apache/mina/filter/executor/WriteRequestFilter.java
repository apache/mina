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
package org.apache.mina.filter.executor;

import org.apache.mina.common.IoEvent;
import org.apache.mina.common.IoEventType;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoProcessor;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.common.WriteRequest;

/**
 * Attaches an {@link IoEventQueueHandler} to an {@link IoSession}'s
 * {@link WriteRequest} queue to provide accurate write queue status tracking.
 * <p>
 * The biggest difference from {@link OrderedThreadPoolExecutor} and
 * {@link UnorderedThreadPoolExecutor} is that {@link IoEventQueueHandler#polled(Object, IoEvent)}
 * is invoked when the write operation is completed by an {@link IoProcessor},
 * consequently providing the accurate tracking of the write request queue
 * status to the {@link IoEventQueueHandler}.
 * <p>
 * Most common usage of this filter could be detecting an {@link IoSession}
 * which writes too fast which will cause {@link OutOfMemoryError} soon:
 * <pre>
 *     session.getFilterChain().addLast(
 *             "writeThrottle",
 *             new WriteRequestFilter(new IoEventQueueThrottle()));
 * </pre>
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class WriteRequestFilter extends IoFilterAdapter {

    private final IoEventQueueHandler queueHandler;

    /**
     * Creates a new instance with a new default {@link IoEventQueueThrottle}.
     */
    public WriteRequestFilter() {
        this(new IoEventQueueThrottle());
    }

    /**
     * Creates a new instance with the specified {@link IoEventQueueHandler}.
     */
    public WriteRequestFilter(IoEventQueueHandler queueHandler) {
        if (queueHandler == null) {
            throw new NullPointerException("queueHandler");
        }
        this.queueHandler = queueHandler;
    }

    /**
     * Returns the {@link IoEventQueueHandler} which is attached to this
     * filter.
     */
    public IoEventQueueHandler getQueueHandler() {
        return queueHandler;
    }

    @Override
    public void filterWrite(
            NextFilter nextFilter,
            IoSession session, WriteRequest writeRequest) throws Exception {

        final IoEvent e = new IoEvent(IoEventType.WRITE, session, writeRequest);

        if (queueHandler.accept(this, e)) {
            nextFilter.filterWrite(session, writeRequest);
            WriteFuture writeFuture = writeRequest.getFuture();
            if (writeFuture == null) {
                return;
            }

            // We can track the write request only when it has a future.
            queueHandler.offered(this, e);
            writeFuture.addListener(new IoFutureListener<WriteFuture>() {
                public void operationComplete(WriteFuture future) {
                    queueHandler.polled(WriteRequestFilter.this, e);
                }
            });
        }
    }
}
