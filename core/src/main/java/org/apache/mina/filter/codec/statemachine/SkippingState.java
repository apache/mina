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
 * Skips data until {@link #canSkip(byte)} returns <tt>false</tt>.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class SkippingState implements DecodingState {

    private int skippedBytes;

    /**
     * Creates a new instance.
     */
    public SkippingState() {
    }

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
            } else {
                skippedBytes++;
            }
        }

        in.position(limit);
        return this;
    }

    protected abstract boolean canSkip(byte b);

    protected abstract DecodingState finishDecode(int skippedBytes)
            throws Exception;
}
