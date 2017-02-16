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
 * {@link DecodingState} which consumes all bytes until a fixed (ASCII) 
 * character is reached. The terminator is skipped.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class ConsumeToDynamicTerminatorDecodingState implements DecodingState {

    private IoBuffer buffer;

    /**
     * {@inheritDoc}
     */
    @Override
    public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        int beginPos = in.position();
        int terminatorPos = -1;
        int limit = in.limit();

        for (int i = beginPos; i < limit; i++) {
            byte b = in.get(i);
            if (isTerminator(b)) {
                terminatorPos = i;
                break;
            }
        }

        if (terminatorPos >= 0) {
            IoBuffer product;

            if (beginPos < terminatorPos) {
                in.limit(terminatorPos);

                if (buffer == null) {
                    product = in.slice();
                } else {
                    buffer.put(in);
                    product = buffer.flip();
                    buffer = null;
                }

                in.limit(limit);
            } else {
                // When input contained only terminator rather than actual data...
                if (buffer == null) {
                    product = IoBuffer.allocate(0);
                } else {
                    product = buffer.flip();
                    buffer = null;
                }
            }
            in.position(terminatorPos + 1);
            return finishDecode(product, out);
        }

        if (buffer == null) {
            buffer = IoBuffer.allocate(in.remaining());
            buffer.setAutoExpand(true);
        }
        buffer.put(in);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DecodingState finishDecode(ProtocolDecoderOutput out) throws Exception {
        IoBuffer product;
        // When input contained only terminator rather than actual data...
        if (buffer == null) {
            product = IoBuffer.allocate(0);
        } else {
            product = buffer.flip();
            buffer = null;
        }
        return finishDecode(product, out);
    }

    /**
     * Determines whether the specified <code>byte</code> is a terminator.
     * 
     * @param b the <code>byte</code> to check.
     * @return <tt>true</tt> if <code>b</code> is a terminator, 
     *         <tt>false</tt> otherwise.
     */
    protected abstract boolean isTerminator(byte b);

    /**
     * Invoked when this state has reached the terminator byte.
     * 
     * @param product the read bytes not including the terminator.
     * @param out the current {@link ProtocolDecoderOutput} used to write 
     *        decoded messages.
     * @return the next state if a state transition was triggered (use 
     *         <code>this</code> for loop transitions) or <code>null</code> if 
     *         the state machine has reached its end.
     * @throws Exception if the read data violated protocol specification.
     */
    protected abstract DecodingState finishDecode(IoBuffer product, ProtocolDecoderOutput out) throws Exception;
}
