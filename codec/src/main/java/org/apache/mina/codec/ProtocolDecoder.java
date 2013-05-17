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
package org.apache.mina.codec;

import java.nio.ByteBuffer;

/**
 * Decodes binary or protocol-specific data into higher-level message objects.
 * 
 * Must be immutable, a CONTEXT will be provided per session.
 * 
 * @param <INPUT> the incoming message to decode (the low level message, usually a {@link ByteBuffer})
 * @param <OUTPUT> the decoded message (your high level protocol Pojo/DTO)
 * @param <DECODING_STATE> the context where to save the current decoding state (e.g. accumulated bytes, encoding
 *        context switching..)
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public interface ProtocolDecoder<INPUT, OUTPUT, DECODING_STATE> {

    /**
     * Create a new session context for this decoder
     * 
     * @return the newly created context
     */
    DECODING_STATE createDecoderState();

    /**
     * Decode binary or protocol-specific content of type <code>INPUT</code> into higher-level protocol message objects,
     * of type OUTPUT
     * 
     * @param input the received message to decode
     * @param context the decoding context (will be stored in the session for the next decode call)
     * @return the decoded message or <code>null</code> if nothing was decoded
     */
    OUTPUT decode(INPUT input, DECODING_STATE context);

    /**
     * Finish decoding, for example if the decoder accumulated some unused input, it should discard it, or throw an
     * Exception
     */
    void finishDecode(DECODING_STATE context);
}