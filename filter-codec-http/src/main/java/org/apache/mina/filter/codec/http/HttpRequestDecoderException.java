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
package org.apache.mina.filter.codec.http;

import org.apache.mina.filter.codec.ProtocolDecoderException;

/**
 * An exception thrown by HTTP decoders.
 *
 * This exception enables decoders which are capable of determining the type of
 * failure to specify the response which is ultimately returned to the client.
 *
 * @author irvingd
 * @author trustin
 * @version $Rev$, $Date$
 */
public class HttpRequestDecoderException extends ProtocolDecoderException {

    private static final long serialVersionUID = 3256999969109063480L;

    private HttpResponseStatus responseStatus;

    /**
     * Creates a new instance with the default response status code
     * ({@link HttpResponseStatus#BAD_REQUEST}).
     *
     * @param message The description of the failure
     */
    public HttpRequestDecoderException(String message) {
        this(message, HttpResponseStatus.BAD_REQUEST);
    }

    /**
     * Creates a new instance.
     *
     * @param message         A description of the failure
     * @param responseStatus  The associated response status
     */
    public HttpRequestDecoderException(String message,
            HttpResponseStatus responseStatus) {
        super(message);
        this.responseStatus = responseStatus;
    }

    /**
     * Returns the response status associated with this exception.
     */
    public HttpResponseStatus getResponseStatus() {
        return responseStatus;
    }
}
