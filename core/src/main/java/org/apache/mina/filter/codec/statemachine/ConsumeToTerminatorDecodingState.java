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

import org.apache.mina.common.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * Consumes until a fixed (ASCII) character is reached.
 * The terminator is skipped.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class ConsumeToTerminatorDecodingState implements DecodingState {

    private final byte terminator;

    private IoBuffer buffer;

    /**
     * @param terminator  The terminator character
     */
    public ConsumeToTerminatorDecodingState(byte terminator) {
        this.terminator = terminator;
    }

    public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out)
            throws Exception {
        int terminatorPos = in.indexOf(terminator);

        if (terminatorPos >= 0) {
            int limit = in.limit();
            IoBuffer product;

            if (in.position() < terminatorPos) {
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
                    product = IoBuffer.allocate(1);
                    product.limit(0);
                } else {
                    product = buffer.flip();
                    buffer = null;
                }
            }
            in.position(terminatorPos + 1);
            return finishDecode(product, out);
        } else {
            if (buffer == null) {
                buffer = IoBuffer.allocate(in.remaining());
                buffer.setAutoExpand(true);
            }
            buffer.put(in);
            return this;
        }
    }

    protected abstract DecodingState finishDecode(IoBuffer product,
            ProtocolDecoderOutput out) throws Exception;
}
