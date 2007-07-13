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
package org.apache.mina.common;

import java.net.SocketAddress;

/**
 * A wrapper for an existing {@link WriteRequest}.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class WriteRequestWrapper implements WriteRequest {

    private final WriteRequest writeRequest;

    /**
     * Creates a new instance that wraps the specified request.
     */
    public WriteRequestWrapper(WriteRequest writeRequest) {
        if (writeRequest == null) {
            throw new NullPointerException("writeRequest");
        }
        this.writeRequest = writeRequest;
    }

    public SocketAddress getDestination() {
        return writeRequest.getDestination();
    }

    public WriteFuture getFuture() {
        return writeRequest.getFuture();
    }

    public Object getMessage() {
        return writeRequest.getMessage();
    }

    /**
     * Returns the wrapped request object.
     */
    public WriteRequest getWriteRequest() {
        return writeRequest;
    }

    @Override
    public String toString() {
        return getMessage().toString();
    }
}
