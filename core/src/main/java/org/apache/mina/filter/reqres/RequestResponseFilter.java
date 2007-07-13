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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteRequest;
import org.apache.mina.common.WriteRequestWrapper;
import org.apache.mina.util.SessionLog;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class RequestResponseFilter extends IoFilterAdapter {

    private static final String RESPONSE_INSPECTOR = RequestResponseFilter.class
            .getName()
            + ".responseInspector";

    private static final String REQUEST_STORE = RequestResponseFilter.class
            .getName()
            + ".requestStore";

    private static final String UNRESPONDED_REQUESTS = RequestResponseFilter.class
            .getName()
            + ".unrespondedRequests";

    private final ResponseInspectorFactory responseInspectorFactory;

    private final ScheduledExecutorService timeoutScheduler;

    public RequestResponseFilter(final ResponseInspector responseInspector,
            ScheduledExecutorService timeoutScheduler) {
        if (responseInspector == null) {
            throw new NullPointerException("responseInspector");
        }
        if (timeoutScheduler == null) {
            throw new NullPointerException("timeoutScheduler");
        }
        this.responseInspectorFactory = new ResponseInspectorFactory() {
            public ResponseInspector getResponseInspector() {
                return responseInspector;
            }
        };
        this.timeoutScheduler = timeoutScheduler;
    }

    public RequestResponseFilter(
            ResponseInspectorFactory responseInspectorFactory,
            ScheduledExecutorService timeoutScheduler) {
        if (responseInspectorFactory == null) {
            throw new NullPointerException("responseInspectorFactory");
        }
        if (timeoutScheduler == null) {
            throw new NullPointerException("timeoutScheduler");
        }
        this.responseInspectorFactory = responseInspectorFactory;
        this.timeoutScheduler = timeoutScheduler;
    }

    @Override
    public void onPreAdd(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
        IoSession session = parent.getSession();
        session.setAttribute(RESPONSE_INSPECTOR, responseInspectorFactory
                .getResponseInspector());
        session.setAttribute(REQUEST_STORE, new HashMap<Object, Request>());
        session
                .setAttribute(UNRESPONDED_REQUESTS,
                        new LinkedHashSet<Request>());
    }

    @Override
    public void onPostRemove(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
        IoSession session = parent.getSession();
        session.removeAttribute(UNRESPONDED_REQUESTS);
        session.removeAttribute(REQUEST_STORE);
        session.removeAttribute(RESPONSE_INSPECTOR);
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session,
            Object message) throws Exception {
        ResponseInspector responseInspector = (ResponseInspector) session
                .getAttribute(RESPONSE_INSPECTOR);
        Object requestId = responseInspector.getRequestId(message);
        if (requestId == null) {
            // Not a response message.  Ignore.
            nextFilter.messageReceived(session, message);
            return;
        }

        // Retrieve (or remove) the corresponding request.
        ResponseType type = responseInspector.getResponseType(message);
        if (type == null) {
            nextFilter.exceptionCaught(session, new IllegalStateException(
                    responseInspector.getClass().getName()
                            + "#getResponseType() may not return null."));
        }

        Map<Object, Request> requestStore = getRequestStore(session);

        Request request;
        switch (type) {
        case WHOLE:
        case PARTIAL_LAST:
            synchronized (requestStore) {
                request = requestStore.remove(requestId);
            }
            break;
        case PARTIAL:
            synchronized (requestStore) {
                request = requestStore.get(requestId);
            }
            break;
        default:
            throw new InternalError();
        }

        if (request == null) {
            // A response message without request. Swallow the event because
            // the response might have arrived too late.
            if (SessionLog.isWarnEnabled(session)) {
                SessionLog.warn(session, "Unknown request ID '" + requestId
                        + "' for the response message. Timed out already?: "
                        + message);
            }
        } else {
            // Found a matching request.
            // Cancel the timeout task if needed.
            if (type != ResponseType.PARTIAL) {
                ScheduledFuture<?> scheduledFuture = request.getTimeoutFuture();
                if (scheduledFuture != null) {
                    scheduledFuture.cancel(false);
                    Set<Request> unrespondedRequests = getUnrespondedRequests(session);
                    synchronized (unrespondedRequests) {
                        unrespondedRequests.remove(request);
                    }
                }
            }

            // And forward the event.
            Response response = new Response(request, message, type);
            request.signal(response);
            nextFilter.messageReceived(session, response);
        }
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        Object message = writeRequest.getMessage();
        if (!(message instanceof Request)) {
            nextFilter.filterWrite(session, writeRequest);
            return;
        }

        Request request = (Request) message;
        if (request.getTimeoutFuture() != null) {
            nextFilter.exceptionCaught(session, new IllegalArgumentException(
                    "Request can not be reused."));
        }

        Map<Object, Request> requestStore = getRequestStore(session);
        Object oldValue = null;
        Object requestId = request.getId();
        synchronized (requestStore) {
            oldValue = requestStore.get(requestId);
            if (oldValue == null) {
                requestStore.put(requestId, request);
            }
        }
        if (oldValue != null) {
            nextFilter.exceptionCaught(session, new IllegalStateException(
                    "Duplicate request ID: " + request.getId()));
        }

        nextFilter.filterWrite(session, new RequestWriteRequest(writeRequest));
    }

    @Override
    public void messageSent(final NextFilter nextFilter,
            final IoSession session, WriteRequest writeRequest)
            throws Exception {
        if (writeRequest instanceof RequestWriteRequest) {
            // Schedule a task to be executed on timeout.
            RequestWriteRequest wrappedRequest = (RequestWriteRequest) writeRequest;
            WriteRequest actualRequest = wrappedRequest.getWriteRequest();
            final Request request = (Request) actualRequest.getMessage();

            // Find the timeout date avoiding overflow.
            Date timeoutDate = new Date(System.currentTimeMillis());
            if (Long.MAX_VALUE - request.getTimeoutMillis() < timeoutDate
                    .getTime()) {
                timeoutDate.setTime(Long.MAX_VALUE);
            } else {
                timeoutDate.setTime(timeoutDate.getTime()
                        + request.getTimeoutMillis());
            }

            TimeoutTask timeoutTask = new TimeoutTask(nextFilter, request,
                    session);

            // Schedule the timeout task.
            ScheduledFuture<?> timeoutFuture = timeoutScheduler.schedule(
                    timeoutTask, request.getTimeoutMillis(),
                    TimeUnit.MILLISECONDS);
            request.setTimeoutTask(timeoutTask);
            request.setTimeoutFuture(timeoutFuture);

            // Add the timtoue task to the unfinished task set.
            Set<Request> unrespondedRequests = getUnrespondedRequests(session);
            synchronized (unrespondedRequests) {
                unrespondedRequests.add(request);
            }

            // and forward the original write request.
            nextFilter.messageSent(session, wrappedRequest.getWriteRequest());
        } else {
            nextFilter.messageSent(session, writeRequest);
        }
    }

    @Override
    public void sessionClosed(NextFilter nextFilter, IoSession session)
            throws Exception {
        // Copy the unifished task set to avoid unnecessary lock acquisition.
        // Copying will be cheap because there won't be that many requests queued.
        Set<Request> unrespondedRequests = getUnrespondedRequests(session);
        List<Request> unrespondedRequestsCopy;
        synchronized (unrespondedRequests) {
            unrespondedRequestsCopy = new ArrayList<Request>(
                    unrespondedRequests);
            unrespondedRequests.clear();
        }

        // Generate timeout artifically.
        for (Request r : unrespondedRequestsCopy) {
            if (r.getTimeoutFuture().cancel(false)) {
                r.getTimeoutTask().run();
            }
        }

        // Clear the request store just in case we missed something, though it's unlikely.
        Map<Object, Request> requestStore = getRequestStore(session);
        synchronized (requestStore) {
            requestStore.clear();
        }

        // Now tell the main subject.
        nextFilter.sessionClosed(session);
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Request> getRequestStore(IoSession session) {
        return (Map<Object, Request>) session.getAttribute(REQUEST_STORE);
    }

    @SuppressWarnings("unchecked")
    private Set<Request> getUnrespondedRequests(IoSession session) {
        return (Set<Request>) session.getAttribute(UNRESPONDED_REQUESTS);
    }

    private class TimeoutTask implements Runnable {
        private final NextFilter filter;

        private final Request request;

        private final IoSession session;

        private TimeoutTask(NextFilter filter, Request request,
                IoSession session) {
            this.filter = filter;
            this.request = request;
            this.session = session;
        }

        public void run() {
            Set<Request> unrespondedRequests = getUnrespondedRequests(session);
            if (unrespondedRequests != null) {
                synchronized (unrespondedRequests) {
                    unrespondedRequests.remove(request);
                }
            }

            Map<Object, Request> requestStore = getRequestStore(session);
            Object requestId = request.getId();
            boolean timedOut;
            synchronized (requestStore) {
                if (requestStore.get(requestId) == request) {
                    requestStore.remove(requestId);
                    timedOut = true;
                } else {
                    timedOut = false;
                }
            }

            if (timedOut) {
                // Throw the exception only when it's really timed out.
                RequestTimeoutException e = new RequestTimeoutException(request);
                request.signal(e);
                filter.exceptionCaught(session, e);
            }
        }
    }

    private static class RequestWriteRequest extends WriteRequestWrapper {
        public RequestWriteRequest(WriteRequest writeRequest) {
            super(writeRequest);
        }

        public Object getMessage() {
            return ((Request) super.getMessage()).getMessage();
        }
    }
}
