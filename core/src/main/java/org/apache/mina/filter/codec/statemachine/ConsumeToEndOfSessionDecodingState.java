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
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * A {@link DecodingState} which consumes all received bytes until a configured
 * number of read bytes has been reached.  Please note that this state can
 * produce the buffer with less data if the associated session has been
 * closed unexpectedly.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class ConsumeToEndOfSessionDecodingState implements DecodingState {

    private IoBuffer buffer;
    private final int maxLength;
    
    public ConsumeToEndOfSessionDecodingState(int maxLength) {
        this.maxLength = maxLength;
    }

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

    protected abstract DecodingState finishDecode(IoBuffer readData,
            ProtocolDecoderOutput out) throws Exception;
}
