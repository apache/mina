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
package org.apache.mina.session;

import java.nio.ByteBuffer;

import org.apache.mina.api.IoFuture;
import org.apache.mina.util.ByteBufferDumper;

/**
 * Default implementation for write requests.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultWriteRequest implements WriteRequest {
    /** The stored message */
    private Object message;

    /** the future to complete when this message is written */
    private IoFuture<Void> future;

    /**
     * Creates a new instance of a WriteRequest
     * @param message The stored message
     */
    public DefaultWriteRequest(Object message) {
        this.message = message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getMessage() {
        return message;
    }

    /**
     * {@inheritDoc}
     */
    public IoFuture<Void> getFuture() {
        return future;
    }

    /**
     * Associates a Future to this WriteRequest instance
     * @param future The associated Future
     */
    public void setFuture(IoFuture<Void> future) {
        this.future = future;
    }
    
    /**
     * @see Object#toString()
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("WriteRequest[");
        
        if (future != null) {
            sb.append("Future,");
        }
        
        if (message != null) {
            // Just dump the first 16 bytes
            sb.append(ByteBufferDumper.dump((ByteBuffer)message, 16, false));
        }
        
        sb.append("]");
        
        return sb.toString();
    }
}