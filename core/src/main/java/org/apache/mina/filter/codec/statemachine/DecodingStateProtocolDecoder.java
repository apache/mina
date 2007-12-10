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

import java.util.Queue;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.util.CircularQueue;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DecodingStateProtocolDecoder implements ProtocolDecoder {
    private final DecodingState state;
    private final Queue<IoBuffer> undecodedBuffers = new CircularQueue<IoBuffer>();
    private IoSession session;

    public DecodingStateProtocolDecoder(DecodingState state) {
        if (state == null) {
            throw new NullPointerException("state");
        }
        this.state = state;
    }
    
    public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out)
            throws Exception {
        if (this.session == null) {
            this.session = session;
        } else if (this.session != session) {
            throw new IllegalStateException(
                    getClass().getSimpleName() + " is a stateful decoder.  " +
    		    "You have to create one per session.");
        }

        undecodedBuffers.offer(in);
        for (;;) {
            IoBuffer b = undecodedBuffers.peek();
            if (b == null) {
                break;
            }

            state.decode(b, out);
            if (b.hasRemaining()) {
                return;
            } else {
                undecodedBuffers.poll();
            }
        }
    }

    public void finishDecode(IoSession session, ProtocolDecoderOutput out)
            throws Exception {
        state.finishDecode(out);
    }

    public void dispose(IoSession session) throws Exception {}
}
