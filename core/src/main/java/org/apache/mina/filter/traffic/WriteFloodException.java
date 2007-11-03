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

import java.util.Collection;

import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteException;
import org.apache.mina.common.WriteRequest;

/**
 * An exception that is thrown by {@link WriteThrottleFilter} when
 * there are too many scheduled write requests or too much amount 
 * of scheduled write data in an {@link IoSession}'s internal write
 * request queue.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class WriteFloodException extends WriteException implements IoFloodException {

    private static final long serialVersionUID = 7377810360950976904L;

    public WriteFloodException(Collection<WriteRequest> requests,
            String message, Throwable cause) {
        super(requests, message, cause);
    }

    public WriteFloodException(Collection<WriteRequest> requests,
            String s) {
        super(requests, s);
    }

    public WriteFloodException(Collection<WriteRequest> requests,
            Throwable cause) {
        super(requests, cause);
    }

    public WriteFloodException(Collection<WriteRequest> requests) {
        super(requests);
    }

    public WriteFloodException(WriteRequest request,
            String message, Throwable cause) {
        super(request, message, cause);
    }

    public WriteFloodException(WriteRequest request, String s) {
        super(request, s);
    }

    public WriteFloodException(WriteRequest request, Throwable cause) {
        super(request, cause);
    }

    public WriteFloodException(WriteRequest request) {
        super(request);
    }
}
