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
package org.apache.mina.filter.reqres;

import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * TODO Add documentation
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Request {
    private final Object id;

    private final Object message;

    private final long timeoutMillis;

    private volatile Runnable timeoutTask;

    private volatile ScheduledFuture<?> timeoutFuture;

    private final BlockingQueue<Object> responses;

    private volatile boolean endOfResponses;

    public Request(Object id, Object message, long timeoutMillis) {
        this(id, message, true, timeoutMillis);
    }

    public Request(Object id, Object message, boolean useResponseQueue,
                   long timeoutMillis) {
        this(id, message, useResponseQueue, timeoutMillis,
                TimeUnit.MILLISECONDS);
    }

    public Request(Object id, Object message, long timeout, TimeUnit unit) {
        this(id, message, true, timeout, unit);
    }

    public Request(Object id, Object message, boolean useResponseQueue,
                   long timeout, TimeUnit unit) {
        if (id == null) {
            throw new IllegalArgumentException("id");
        }
        if (message == null) {
            throw new IllegalArgumentException("message");
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout: " + timeout
                    + " (expected: 0+)");
        } else if (timeout == 0) {
            timeout = Long.MAX_VALUE;
        }

        if (unit == null) {
            throw new IllegalArgumentException("unit");
        }

        this.id = id;
        this.message = message;
        this.responses = useResponseQueue ? new LinkedBlockingQueue<Object>() : null;
        this.timeoutMillis = unit.toMillis(timeout);
    }

    public Object getId() {
        return id;
    }

    public Object getMessage() {
        return message;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public boolean isUseResponseQueue() {
        return responses != null;
    }

    public boolean hasResponse() {
        checkUseResponseQueue();
        return !responses.isEmpty();
    }

    public Response awaitResponse() throws RequestTimeoutException,
            InterruptedException {
        checkUseResponseQueue();
        chechEndOfResponses();
        return convertToResponse(responses.take());
    }

    public Response awaitResponse(long timeout, TimeUnit unit)
            throws RequestTimeoutException, InterruptedException {
        checkUseResponseQueue();
        chechEndOfResponses();
        return convertToResponse(responses.poll(timeout, unit));
    }

    private Response convertToResponse(Object o) {
        if (o instanceof Response) {
            return (Response) o;
        }

        if (o == null) {
            return null;
        }

        throw (RequestTimeoutException) o;
    }

    public Response awaitResponseUninterruptibly()
            throws RequestTimeoutException {
        for (; ;) {
            try {
                return awaitResponse();
            } catch (InterruptedException e) {
                // Do nothing
            }
        }
    }

    private void chechEndOfResponses() {
        if (responses != null && endOfResponses && responses.isEmpty()) {
            throw new NoSuchElementException(
                    "All responses has been retrieved already.");
        }
    }

    private void checkUseResponseQueue() {
        if (responses == null) {
            throw new UnsupportedOperationException(
                    "Response queue is not available; useResponseQueue is false.");
        }
    }

    void signal(Response response) {
        signal0(response);
        if (response.getType() != ResponseType.PARTIAL) {
            endOfResponses = true;
        }
    }

    void signal(RequestTimeoutException e) {
        signal0(e);
        endOfResponses = true;
    }

    private void signal0(Object answer) {
        if (responses != null) {
            responses.add(answer);
        }
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!(o instanceof Request)) {
            return false;
        }

        Request that = (Request) o;
        return this.getId().equals(that.getId());
    }

    @Override
    public String toString() {
        String timeout = getTimeoutMillis() == Long.MAX_VALUE ? "max"
                : String.valueOf(getTimeoutMillis());

        return "request: { id=" + getId() + ", timeout=" + timeout
                + ", message=" + getMessage() + " }";
    }

    Runnable getTimeoutTask() {
        return timeoutTask;
    }

    void setTimeoutTask(Runnable timeoutTask) {
        this.timeoutTask = timeoutTask;
    }

    ScheduledFuture<?> getTimeoutFuture() {
        return timeoutFuture;
    }

    void setTimeoutFuture(ScheduledFuture<?> timeoutFuture) {
        this.timeoutFuture = timeoutFuture;
    }
}
