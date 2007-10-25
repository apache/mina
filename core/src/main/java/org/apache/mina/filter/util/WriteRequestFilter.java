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
package org.apache.mina.filter.util;

import org.apache.mina.common.IoEventType;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteRequest;
import org.apache.mina.common.WriteRequestWrapper;

/**
 * An abstract {@link IoFilter} that simplifies the implementation of
 * an {@link IoFilter} that filters an {@link IoEventType#WRITE} event.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 *
 */
public abstract class WriteRequestFilter extends IoFilterAdapter {

    @Override
    public void exceptionCaught(NextFilter nextFilter, IoSession session,
            Throwable cause) throws Exception {
        nextFilter.exceptionCaught(session, cause);
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        Object filteredMessage = doFilterWrite(nextFilter, session, writeRequest);
        if (filteredMessage != null && filteredMessage != writeRequest.getMessage()) {
            nextFilter.filterWrite(
                    session, new FilteredWriteRequest(
                            filteredMessage, writeRequest));
        } else {
            nextFilter.filterWrite(session, writeRequest);
        }
    }

    @Override
    public void messageSent(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        if (writeRequest instanceof FilteredWriteRequest) {
            FilteredWriteRequest req = (FilteredWriteRequest) writeRequest;
            if (req.getParent() == this) {
                nextFilter.messageSent(session, req.getWriteRequest());
                return;
            }
        }

        nextFilter.messageSent(session, writeRequest);
    }

    protected abstract Object doFilterWrite(
            NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception;

    private class FilteredWriteRequest extends WriteRequestWrapper {
        private final Object filteredMessage;

        public FilteredWriteRequest(Object filteredMessage, WriteRequest writeRequest) {
            super(writeRequest);

            if (filteredMessage == null) {
                throw new NullPointerException("filteredMessage");
            }
            this.filteredMessage = filteredMessage;
        }

        public WriteRequestFilter getParent() {
            return WriteRequestFilter.this;
        }

        @Override
        public Object getMessage() {
            return filteredMessage;
        }
    }
}
