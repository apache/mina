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

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoEventType;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestWrapper;

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
    public void filterWrite(int index, IoSession session,
            WriteRequest writeRequest) throws Exception {
        Object filteredMessage = doFilterWrite(index+1, session, writeRequest);
        if (filteredMessage != null && filteredMessage != writeRequest.getMessage()) {
            session.getFilterOut(index).filterWrite(index+1,
                    session, new FilteredWriteRequest(
                            filteredMessage, writeRequest));
        } else {
        	session.getFilterOut(index).filterWrite(index+1, session, writeRequest);
        }
    }

    @Override
    public void messageSent(int index, IoSession session,
            WriteRequest writeRequest) throws Exception {
        if (writeRequest instanceof FilteredWriteRequest) {
            FilteredWriteRequest req = (FilteredWriteRequest) writeRequest;
            if (req.getParent() == this) {
            	session.getFilterIn(index).messageSent(index+1, session, 
            			req.getParentRequest());
                return;
            }
        }

        session.getFilterIn(index).messageSent(index+1, session, writeRequest);
    }

    protected abstract Object doFilterWrite(
            int index, IoSession session, WriteRequest writeRequest) throws Exception;

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
