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
package org.apache.mina.filter.query;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.apache.mina.api.IoSession;
import org.apache.mina.util.AbstractIoFuture;

/**
 * A future representing the promise of a reply to a request.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 * @param <REQUEST> the request type
 * @param <RESPONSE> the response type
 */
class RequestFuture<REQUEST extends Request, RESPONSE extends Response> extends AbstractIoFuture<RESPONSE> {

    private final IoSession session;

    private final Object id;

    private ScheduledFuture<?> schedFuture;

    public RequestFuture(IoSession session, Object id) {
        this.session = session;
        this.id = id;
    }

    @Override
    protected boolean cancelOwner(boolean mayInterruptIfRunning) {
        throw new IllegalStateException("not implemented");
    }

    void complete(RESPONSE response) {
        if (schedFuture != null) {
            schedFuture.cancel(true);
        }
        setResult(response);
    }

    void setTimeoutFuture(ScheduledFuture<?> schedFuture) {
        this.schedFuture = schedFuture;
    }

    Runnable timeout = new Runnable() {

        @SuppressWarnings("rawtypes")
        @Override
        public void run() {
            Map inFlight = session.getAttribute(RequestFilter.IN_FLIGHT_REQUESTS);
            if (inFlight != null) {
                inFlight.remove(id);
            }
            setException(new RequestTimeoutException());

        }
    };
}