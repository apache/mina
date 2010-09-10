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
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * {@link DecodingState} which consumes all received bytes until a configured
 * number of read bytes has been reached. Please note that this state can
 * produce a buffer with less data than the configured length if the associated 
 * session has been closed unexpectedly.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class FixedLengthDecodingState implements DecodingState {

    private final int length;

    private IoBuffer buffer;

    /**
     * Constructs a new instance using the specified decode length.
     *
     * @param length the number of bytes to read.
     */
    public FixedLengthDecodingState(int length) {
        this.length = length;
    }

    /**
     * {@inheritDoc}
     */
    public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out)
            throws Exception {
        if (buffer == null) {
            if (in.remaining() >= length) {
                int limit = in.limit();
                in.limit(in.position() + length);
                IoBuffer product = in.slice();
                in.position(in.position() + length);
                in.limit(limit);
                return finishDecode(product, out);
            }

            buffer = IoBuffer.allocate(length);
            buffer.put(in);
            return this;
        }

        if (in.remaining() >= length - buffer.position()) {
            int limit = in.limit();
            in.limit(in.position() + length - buffer.position());
            buffer.put(in);
            in.limit(limit);
            IoBuffer product = this.buffer;
            this.buffer = null;
            return finishDecode(product.flip(), out);
        }
        
        buffer.put(in);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public DecodingState finishDecode(ProtocolDecoderOutput out)
            throws Exception {
        IoBuffer readData;
        if (buffer == null) {
            readData = IoBuffer.allocate(0);
        } else {
            readData = buffer.flip();
            buffer = null;
        }
        return finishDecode(readData ,out);
    }

    /**
     * Invoked when this state has consumed the configured number of bytes.
     * 
     * @param product the data.
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
