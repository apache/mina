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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.util.WriteRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Add documentation
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @org.apache.xbean.XBean
 */
public class RequestResponseFilter extends WriteRequestFilter {

    private final AttributeKey RESPONSE_INSPECTOR = new AttributeKey(getClass(), "responseInspector");
    private final AttributeKey REQUEST_STORE = new AttributeKey(getClass(), "requestStore");
    private final AttributeKey UNRESPONDED_REQUEST_STORE = new AttributeKey(getClass(), "unrespondedRequestStore");

    private final ResponseInspectorFactory responseInspectorFactory;
    private final ScheduledExecutorService timeoutScheduler;

    private final static Logger LOGGER = LoggerFactory.getLogger(RequestResponseFilter.class);

    public RequestResponseFilter(final ResponseInspector responseInspector,
            ScheduledExecutorService timeoutScheduler) {
        if (responseInspector == null) {
            throw new IllegalArgumentException("responseInspector");
        }
        if (timeoutScheduler == null) {
            throw new IllegalArgumentException("timeoutScheduler");
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
            throw new IllegalArgumentException("responseInspectorFactory");
        }
        if (timeoutScheduler == null) {
            throw new IllegalArgumentException("timeoutScheduler");
        }
        this.responseInspectorFactory = responseInspectorFactory;
        this.timeoutScheduler = timeoutScheduler;
    }

    @Override
    public void onPreAdd(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
        if (parent.contains(this)) {
            throw new IllegalArgumentException(
                    "You can't add the same filter instance more than once.  Create another instance and add it.");
        }

        IoSession session = parent.getSession();
        session.setAttribute(RESPONSE_INSPECTOR, responseInspectorFactory
                .getResponseInspector());
        session.setAttribute(REQUEST_STORE, createRequestStore(session));
        session.setAttribute(UNRESPONDED_REQUEST_STORE, createUnrespondedRequestStore(session));
    }

    @Override
    public void onPostRemove(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
        IoSession session = parent.getSession();

        destroyUnrespondedRequestStore(getUnrespondedRequestStore(session));
        destroyRequestStore(getRequestStore(session));

        session.removeAttribute(UNRESPONDED_REQUEST_STORE);
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
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Unknown request ID '" + requestId
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
                    Set<Request> unrespondedRequests = getUnrespondedRequestStore(session);
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
    protected Object doFilterWrite(
            final NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        Object message = writeRequest.getMessage();
        if (!(message instanceof Request)) {
            return null;
        }

        final Request request = (Request) message;
        if (request.getTimeoutFuture() != null) {
            throw new IllegalArgumentException("Request can not be reused.");
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
            throw new IllegalStateException(
                    "Duplicate request ID: " + request.getId());
        }

        // Schedule a task to be executed on timeout.
        TimeoutTask timeoutTask = new TimeoutTask(
                nextFilter, request, session);
        ScheduledFuture<?> timeoutFuture = timeoutScheduler.schedule(
                timeoutTask, request.getTimeoutMillis(),
                TimeUnit.MILLISECONDS);
        request.setTimeoutTask(timeoutTask);
        request.setTimeoutFuture(timeoutFuture);

        // Add the timeout task to the unfinished task set.
        Set<Request> unrespondedRequests = getUnrespondedRequestStore(session);
        synchronized (unrespondedRequests) {
            unrespondedRequests.add(request);
        }

        return request.getMessage();
    }

    @Override
    public void sessionClosed(NextFilter nextFilter, IoSession session)
            throws Exception {
        // Copy the unfinished task set to avoid unnecessary lock acquisition.
        // Copying will be cheap because there won't be that many requests queued.
        Set<Request> unrespondedRequests = getUnrespondedRequestStore(session);
        List<Request> unrespondedRequestsCopy;
        synchronized (unrespondedRequests) {
            unrespondedRequestsCopy = new ArrayList<Request>(
                    unrespondedRequests);
            unrespondedRequests.clear();
        }

        // Generate timeout artificially.
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
    private Set<Request> getUnrespondedRequestStore(IoSession session) {
        return (Set<Request>) session.getAttribute(UNRESPONDED_REQUEST_STORE);
    }

    /**
     * Returns a {@link Map} which stores {@code messageId}-{@link Request}
     * pairs whose {@link Response}s are not received yet.  Please override
     * this method if you need to use other {@link Map} implementation
     * than the default one ({@link HashMap}).
     */
    protected Map<Object, Request> createRequestStore(
            IoSession session) {
        return new ConcurrentHashMap<Object, Request>();
    }

    /**
     * Returns a {@link Set} which stores {@link Request} whose
     * {@link Response}s are not received yet. Please override
     * this method if you need to use other {@link Set} implementation
     * than the default one ({@link LinkedHashSet}).  Please note that
     * the {@link Iterator} of the returned {@link Set} have to iterate
     * its elements in the insertion order to ensure that
     * {@link RequestTimeoutException}s are thrown in the order which
     * {@link Request}s were written.  If you don't need to guarantee
     * the order of thrown exceptions, any {@link Set} implementation
     * can be used.
     */
    protected Set<Request> createUnrespondedRequestStore(
            IoSession session) {
        return new LinkedHashSet<Request>();
    }

    /**
     * Releases any resources related with the {@link Map} created by
     * {@link #createRequestStore(IoSession)}.  This method is useful
     * if you override {@link #createRequestStore(IoSession)}.
     *
     * @param requestStore what you returned in {@link #createRequestStore(IoSession)}
     */
    protected void destroyRequestStore(
            Map<Object, Request> requestStore) {
        // Do nothing
    }

    /**
     * Releases any resources related with the {@link Set} created by
     * {@link #createUnrespondedRequestStore(IoSession)}.  This method is
     * useful if you override {@link #createUnrespondedRequestStore(IoSession)}.
     *
     * @param unrespondedRequestStore what you returned in {@link #createUnrespondedRequestStore(IoSession)}
     */
    protected void destroyUnrespondedRequestStore(
            Set<Request> unrespondedRequestStore) {
        // Do nothing
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
            Set<Request> unrespondedRequests = getUnrespondedRequestStore(session);
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
}
