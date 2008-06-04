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

import java.util.ArrayList;
import java.util.List;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @TODO Add documentation
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class DecodingStateMachine implements DecodingState {
    private final Logger log = LoggerFactory
            .getLogger(DecodingStateMachine.class);

    private final List<Object> childProducts = new ArrayList<Object>();

    private final ProtocolDecoderOutput childOutput = new ProtocolDecoderOutput() {
        public void flush() {
        }

        public void write(Object message) {
            childProducts.add(message);
        }
    };

    private DecodingState currentState;
    private boolean initialized;

    protected abstract DecodingState init() throws Exception;

    protected abstract DecodingState finishDecode(List<Object> childProducts,
            ProtocolDecoderOutput out) throws Exception;

    protected abstract void destroy() throws Exception;

    public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out)
            throws Exception {
        DecodingState state = getCurrentState();

        final int limit = in.limit();
        int pos = in.position();

        try {
            for (;;) {
                // Wait for more data if all data is consumed.
                if (pos == limit) {
                    break;
                }

                DecodingState oldState = state;
                state = state.decode(in, childOutput);

                // If finished, call finishDecode
                if (state == null) {
                    return finishDecode(childProducts, out);
                }

                int newPos = in.position();

                // Wait for more data if nothing is consumed and state didn't change.
                if (newPos == pos && oldState == state) {
                    break;
                }
                pos = newPos;
            }

            return this;
        } catch (Exception e) {
            state = null;
            throw e;
        } finally {
            this.currentState = state;

            // Destroy if decoding is finished or failed.
            if (state == null) {
                cleanup();
            }
        }
    }

    public DecodingState finishDecode(ProtocolDecoderOutput out)
            throws Exception {
        DecodingState nextState;
        DecodingState state = getCurrentState();
        try {
            for (;;) {
                DecodingState oldState = state;
                state = state.finishDecode(childOutput);
                if (state == null) {
                    // Finished
                    break;
                }
    
                // Exit if state didn't change.
                if (oldState == state) {
                    break;
                }
            }
        } catch (Exception e) {
            state = null;
            log.debug(
                    "Ignoring the exception caused by a closed session.", e);
        } finally {
            this.currentState = state;
            nextState = finishDecode(childProducts, out);
            if (state == null) {
                cleanup();
            }
        }
        return nextState;
    }

    private void cleanup() {
        if (!initialized) {
            throw new IllegalStateException();
        }
        
        initialized = false;
        childProducts.clear();
        try {
            destroy();
        } catch (Exception e2) {
            log.warn("Failed to destroy a decoding state machine.", e2);
        }
    }

    private DecodingState getCurrentState() throws Exception {
        DecodingState state = this.currentState;
        if (state == null) {
            state = init();
            initialized = true;
        }
        return state;
    }
}
