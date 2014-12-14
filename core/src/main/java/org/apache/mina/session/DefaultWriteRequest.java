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

    /** The original message (before being processed by the filter chain */
    private Object originalMessage;

    /** the future to complete when this message is written */
    private IoFuture<Void> future;
    
    /**
     * The secure internal flag that tells if the message must be encrypted
     * when sent (false) or not (true)
     */
    private boolean secureInternal = false;

    private boolean confirmRequested = true;
    
    /**
     * Creates a new instance of a WriteRequest, storing the message as it was
     * when the IoSession.write() has been called.
     * 
     * @param message The original message
     */
    public DefaultWriteRequest(Object originalMessage) {
        this.message = originalMessage;
        this.originalMessage = originalMessage;
    }

    /**
     * Creates a new instance of a WriteRequest, storing the message as it was
     * when the IoSession.write() has been called.
     * 
     * @param message The message to write
     * @param originalMessage the original message
     * @param confirmRequested whether to send an event or not
     */
    public DefaultWriteRequest(Object message, Object originalMessage, boolean confirmRequested) {
        this.message = message;
        this.originalMessage = originalMessage;
        this.confirmRequested = confirmRequested;
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
    @Override
    public void setMessage(Object message) {
        this.message = message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoFuture<Void> getFuture() {
        return future;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFuture(final IoFuture<Void> future) {
        this.future = future;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getOriginalMessage() {
        return originalMessage;
    }

    /**
     * @see Object#toString()
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("WriteRequest[");

        if (future != null) {
            sb.append("Future : ");
            sb.append(future);
            sb.append(",");
        } else {
            sb.append("No future, ");
        }

        if (originalMessage != null) {
            // Dump the original message
            sb.append("Original message : '");

            if (originalMessage instanceof ByteBuffer) {
                sb.append(ByteBufferDumper.dump((ByteBuffer) originalMessage, 16, false));
            } else {
                sb.append(originalMessage);
            }

            sb.append("', ");
        } else {
            sb.append("No Orginal message,");
        }

        if (message != null) {
            // Dump the encoded message
            // Just dump the first 16 bytes
            sb.append("Encoded message : '");

            if (message instanceof ByteBuffer) {
                sb.append(ByteBufferDumper.dump((ByteBuffer) message, 16, false));
            } else {
                sb.append(message);
            }

            sb.append("'");
        } else {
            sb.append("No encoded message,");
        }

        sb.append("]");

        return sb.toString();
    }

    @Override
    public boolean isSecureInternal() {
        return secureInternal;
    }

    @Override
    public void setSecureInternal(boolean secureInternal) {
        this.secureInternal = secureInternal;        
    }

    @Override
    public boolean isConfirmRequested() {
        return confirmRequested;
    }
}