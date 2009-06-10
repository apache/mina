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
 * {@link DecodingState} which skips data until {@link #canSkip(byte)} returns 
 * <tt>false</tt>.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class SkippingState implements DecodingState {

    private int skippedBytes;

    /**
     * {@inheritDoc}
     */
    public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out)
            throws Exception {
        int beginPos = in.position();
        int limit = in.limit();
        for (int i = beginPos; i < limit; i++) {
            byte b = in.get(i);
            if (!canSkip(b)) {
                in.position(i);
                int answer = this.skippedBytes;
                this.skippedBytes = 0;
                return finishDecode(answer);
            }
            
            skippedBytes++;
        }

        in.position(limit);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public DecodingState finishDecode(ProtocolDecoderOutput out)
            throws Exception {
        return finishDecode(skippedBytes);
    }

    /**
     * Called to determine whether the specified byte can be skipped.
     * 
     * @param b the byte to check.
     * @return <code>true</code> if the byte can be skipped.
     */
    protected abstract boolean canSkip(byte b);

    /**
     * Invoked when this state cannot skip any more bytes.
     * 
     * @param skippedBytes the number of bytes skipped.
     * @return the next state if a state transition was triggered (use 
     *         <code>this</code> for loop transitions) or <code>null</code> if 
     *         the state machine has reached its end.
     * @throws Exception if the read data violated protocol specification.
     */
    protected abstract DecodingState finishDecode(int skippedBytes)
            throws Exception;
}
