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
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * Represents a state in a decoder state machine used by 
 * {@link DecodingStateMachine}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface DecodingState {
    /**
     * Invoked when data is available for this state.
     * 
     * @param in the data to be decoded.
     * @param out used to write decoded objects.
     * @return the next state if a state transition was triggered (use 
     *         <code>this</code> for loop transitions) or <code>null</code> if 
     *         the state machine has reached its end.
     * @throws Exception if the read data violated protocol specification.
     */
    DecodingState decode(IoBuffer in, ProtocolDecoderOutput out)
            throws Exception;
    
    /**
     * Invoked when the associated {@link IoSession} is closed. This method is 
     * useful when you deal with protocols which don't specify the length of a 
     * message (e.g. HTTP responses without <tt>content-length</tt> header). 
     * Implement this method to process the remaining data that 
     * {@link #decode(IoBuffer, ProtocolDecoderOutput)} method didn't process 
     * completely.
     * 
     * @param out used to write decoded objects.
     * @return the next state if a state transition was triggered (use 
     *         <code>this</code> for loop transitions) or <code>null</code> if 
     *         the state machine has reached its end.
     * @throws Exception if the read data violated protocol specification.
     */
    DecodingState finishDecode(ProtocolDecoderOutput out) throws Exception;
}
