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
 * {@link DecodingState} which decodes a single <code>CRLF</code>.
 * If it is found, the bytes are consumed and <code>true</code>
 * is provided as the product. Otherwise, read bytes are pushed back
 * to the stream, and <code>false</code> is provided as the
 * product.
 * Note that if we find a CR but do not find a following LF, we raise
 * an error.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class CrLfDecodingState implements DecodingState {
    /**
     * Carriage return character
     */
    private static final byte CR = 13;
    
    /**
     * Line feed character
     */
    private static final byte LF = 10;

    private boolean hasCR;

    /**
     * {@inheritDoc}
     */
    public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out)
            throws Exception {
        boolean found = false;
        boolean finished = false;
        while (in.hasRemaining()) {
            byte b = in.get();
            if (!hasCR) {
                if (b == CR) {
                    hasCR = true;
                } else {
                    if (b == LF) {
                        found = true;
                    } else {
                        in.position(in.position() - 1);
                        found = false;
                    }
                    finished = true;
                    break;
                }
            } else {
                if (b == LF) {
                    found = true;
                    finished = true;
                    break;
                }
                
                throw new ProtocolDecoderException(
                        "Expected LF after CR but was: " + (b & 0xff));
            }
        }

        if (finished) {
            hasCR = false;
            return finishDecode(found, out);
        }
        
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public DecodingState finishDecode(ProtocolDecoderOutput out)
            throws Exception {
        return finishDecode(false, out);
    }

    /**
     * Invoked when this state has found a <code>CRLF</code>.
     * 
     * @param foundCRLF <code>true</code> if <code>CRLF</code> was found.
     * @param out the current {@link ProtocolDecoderOutput} used to write 
     *        decoded messages.
     * @return the next state if a state transition was triggered (use 
     *         <code>this</code> for loop transitions) or <code>null</code> if 
     *         the state machine has reached its end.
     * @throws Exception if the read data violated protocol specification.
     */
    protected abstract DecodingState finishDecode(boolean foundCRLF,
            ProtocolDecoderOutput out) throws Exception;
}
