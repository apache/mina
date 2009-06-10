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
package org.apache.mina.filter.codec.statemachine;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * {@link DecodingState} which consumes all received bytes until the session is
 * closed.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class ConsumeToEndOfSessionDecodingState implements DecodingState {

    private IoBuffer buffer;
    private final int maxLength;
    
    /**
     * Creates a new instance using the specified maximum length.
     * 
     * @param maxLength the maximum number of bytes which will be consumed. If
     *        this max is reached a {@link ProtocolDecoderException} will be 
     *        thrown by {@link #decode(IoBuffer, ProtocolDecoderOutput)}.
     */
    public ConsumeToEndOfSessionDecodingState(int maxLength) {
        this.maxLength = maxLength;
    }

    /**
     * {@inheritDoc}
     */
    public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out)
            throws Exception {
        if (buffer == null) {
            buffer = IoBuffer.allocate(256).setAutoExpand(true);
        }

        if (buffer.position() + in.remaining() > maxLength) {
            throw new ProtocolDecoderException("Received data exceeds " + maxLength + " byte(s).");
        }
        buffer.put(in);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public DecodingState finishDecode(ProtocolDecoderOutput out)
            throws Exception {
        try {
            if (buffer == null) {
                buffer = IoBuffer.allocate(0);
            }
            buffer.flip();
            return finishDecode(buffer, out);
        } finally {
            buffer = null;
        }
    }

    /**
     * Invoked when this state has consumed all bytes until the session is 
     * closed.
     * 
     * @param product the bytes read.
     * @param out the current {@link ProtocolDecoderOutput} used to write 
     *        decoded messages.
     * @return the next state if a state transition was triggered (use 
     *         <code>this</code> for loop transitions) or <code>null</code> if 
     *         the state machine has reached its end.
     * @throws Exception if the read data violated protocol specification.
     */
    protected abstract DecodingState finishDecode(IoBuffer product,
            ProtocolDecoderOutput out) throws Exception;
}
