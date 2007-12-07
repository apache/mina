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
 * A decoder which writes all read bytes in to a known <code>Bytes</code>
 * context until a <code>CRLF</code> has been encountered
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class ConsumeToCrLfDecodingState implements DecodingState {

    /**
     * Carriage return character
     */
    private static final byte CR = 13;

    /**
     * Line feed character
     */
    private static final byte LF = 10;

    private boolean lastIsCR;

    private IoBuffer buffer;

    /**
     * Creates a new instance.
     */
    public ConsumeToCrLfDecodingState() {
    }

    public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out)
            throws Exception {
        int beginPos = in.position();
        int limit = in.limit();
        int terminatorPos = -1;

        for (int i = beginPos; i < limit; i++) {
            byte b = in.get(i);
            if (b == CR) {
                lastIsCR = true;
            } else {
                if (b == LF && lastIsCR) {
                    terminatorPos = i;
                    break;
                }
                lastIsCR = false;
            }
        }

        if (terminatorPos >= 0) {
            IoBuffer product;

            int endPos = terminatorPos - 1;

            if (beginPos < endPos) {
                in.limit(endPos);

                if (buffer == null) {
                    product = in.slice();
                } else {
                    buffer.put(in);
                    product = buffer.flip();
                    buffer = null;
                }

                in.limit(limit);
            } else {
                // When input contained only CR or LF rather than actual data...
                if (buffer == null) {
                    product = IoBuffer.allocate(0);
                } else {
                    product = buffer.flip();
                    buffer = null;
                }
            }
            in.position(terminatorPos + 1);
            return finishDecode(product, out);
        } else {
            in.position(beginPos);
            if (buffer == null) {
                buffer = IoBuffer.allocate(in.remaining());
                buffer.setAutoExpand(true);
            }

            buffer.put(in);
            if (lastIsCR) {
                buffer.position(buffer.position() - 1);
            }
            return this;
        }
    }

    public DecodingState finishDecode(ProtocolDecoderOutput out) throws Exception {
        IoBuffer product;
        // When input contained only CR or LF rather than actual data...
        if (buffer == null) {
            product = IoBuffer.allocate(0);
        } else {
            product = buffer.flip();
            buffer = null;
        }
        return finishDecode(product, out);
    }

    protected abstract DecodingState finishDecode(IoBuffer product,
            ProtocolDecoderOutput out) throws Exception;
}
