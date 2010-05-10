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
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * {@link ProtocolDecoder} which uses a {@link DecodingState} to decode data.
 * Use a {@link DecodingStateMachine} as {@link DecodingState} to create
 * a state machine which can decode your protocol.
 * <p>
 * NOTE: This is a stateful decoder. You should create one instance per session.
 * </p>
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DecodingStateProtocolDecoder implements ProtocolDecoder {
    private final DecodingState state;
    private final Queue<IoBuffer> undecodedBuffers = new ConcurrentLinkedQueue<IoBuffer>();
    private IoSession session;

    /**
     * Creates a new instance using the specified {@link DecodingState} 
     * instance.
     * 
     * @param state the {@link DecodingState}.
     * @throws IllegalArgumentException if the specified state is <code>null</code>.
     */
    public DecodingStateProtocolDecoder(DecodingState state) {
        if (state == null) {
            throw new IllegalArgumentException("state");
        }
        this.state = state;
    }

    /**
     * {@inheritDoc}
     */
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

            int oldRemaining = b.remaining();
            state.decode(b, out);
            int newRemaining = b.remaining();
            if (newRemaining != 0) {
                if (oldRemaining == newRemaining) {
                    throw new IllegalStateException(
                            DecodingState.class.getSimpleName() + " must " +
                            "consume at least one byte per decode().");
                }
            } else {
                undecodedBuffers.poll();
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void finishDecode(IoSession session, ProtocolDecoderOutput out)
            throws Exception {
        state.finishDecode(out);
    }

    /**
     * {@inheritDoc}
     */
    public void dispose(IoSession session) throws Exception {
        // Do nothing
    }
}
