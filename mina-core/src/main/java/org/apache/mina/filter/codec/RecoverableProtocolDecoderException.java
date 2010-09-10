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
package org.apache.mina.filter.codec;

/**
 * A special exception that tells the {@link ProtocolDecoder} can keep
 * decoding even after this exception is thrown.
 * <p>
 * Once {@link ProtocolCodecFilter} catches any other type of exception
 * than {@link RecoverableProtocolDecoderException}, it stops calling
 * the {@link ProtocolDecoder#decode(org.apache.mina.core.session.IoSession,
 *        org.apache.mina.core.buffer.IoBuffer, ProtocolDecoderOutput)}
 * immediately and fires an <tt>exceptionCaught</tt> event.
 * <p>
 * On the other hand, if {@link RecoverableProtocolDecoderException} is thrown,
 * it doesn't stop immediately but keeps calling the {@link ProtocolDecoder}
 * as long as the position of the read buffer changes.
 * <p>
 * {@link RecoverableProtocolDecoderException} is useful for a robust
 * {@link ProtocolDecoder} that can continue decoding even after any
 * protocol violation.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class RecoverableProtocolDecoderException extends
        ProtocolDecoderException {

    private static final long serialVersionUID = -8172624045024880678L;

    public RecoverableProtocolDecoderException() {
        // Do nothing
    }

    public RecoverableProtocolDecoderException(String message) {
        super(message);
    }

    public RecoverableProtocolDecoderException(Throwable cause) {
        super(cause);
    }

    public RecoverableProtocolDecoderException(String message, Throwable cause) {
        super(message, cause);
    }

}
