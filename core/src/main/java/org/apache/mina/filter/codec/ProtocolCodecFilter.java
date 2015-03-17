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

import java.nio.ByteBuffer;

import org.apache.mina.api.AbstractIoFilter;
import org.apache.mina.api.IoFilter;
import org.apache.mina.api.IoSession;
import org.apache.mina.codec.ProtocolDecoder;
import org.apache.mina.codec.ProtocolDecoderException;
import org.apache.mina.codec.ProtocolEncoder;
import org.apache.mina.filterchain.ReadFilterChainController;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.apache.mina.session.AttributeKey;
import org.apache.mina.session.WriteRequest;
import org.apache.mina.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link IoFilter} which translates binary or protocol specific data into
 * message objects and vice versa using {@link ProtocolCodecFactory},
 * {@link ProtocolEncoder}, or {@link ProtocolDecoder}.
 * 
 * @param MESSAGE
 *            the kind of high level business message this filter will encode
 *            and decode.
 * @param ENCODED
 *            the kind of low level message (most of time {@link ByteBuffer})
 *            this filter will produce of consume.
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ProtocolCodecFilter<MESSAGE, ENCODED, ENCODING_STATE, DECODING_STATE> extends AbstractIoFilter {
    /** A logger for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolCodecFilter.class);

    /** the immutable encoder */
    private final ProtocolEncoder<MESSAGE, ENCODED, ENCODING_STATE> encoder;

    /** the immutable decoder */
    private final ProtocolDecoder<ENCODED, MESSAGE, DECODING_STATE> decoder;

    /** key for session attribute holding the encoder */
    private static final AttributeKey<Object> ENCODER = new AttributeKey<Object>(Object.class, "internal_encoder");

    /** key for session attribute holding the decoder */
    private static final AttributeKey<Object> DECODER = new AttributeKey<Object>(Object.class, "internal_decoder");

    /**
     * Creates a new instance of ProtocolCodecFilter, with the specified encoder
     * and decoder.
     * 
     */
    public ProtocolCodecFilter(ProtocolEncoder<MESSAGE, ENCODED, ENCODING_STATE> encoder,
            ProtocolDecoder<ENCODED, MESSAGE, DECODING_STATE> decoder) {
        Assert.assertNotNull(encoder, "encoder");
        Assert.assertNotNull(decoder, "decoder");
        this.encoder = encoder;
        this.decoder = decoder;
    }

    /**
     * Process the incoming message, calling the session decoder. As the
     * incoming buffer might contains more than one messages, we have to loop
     * until the decoder throws an exception. <code>
     *  while ( buffer not empty )
     *    try
     *      decode ( buffer )
     *    catch
     *      break;
     * </code>
     */
    @SuppressWarnings("unchecked")
    @Override
    public void messageReceived(IoSession session, Object in, ReadFilterChainController controller) {
        LOGGER.debug("Processing a MESSAGE_RECEIVED for session {}", session);

        DECODING_STATE state = getDecodingState(session);

        // Loop until the decoder cannot decode more
        MESSAGE msg;
        try {
            while (((msg = decoder.decode((ENCODED) in, state)) != null)) {
                super.messageReceived(session, msg, controller);
            }
        } catch (ProtocolDecoderException e) {
            LOGGER.debug("decoding exception : ", e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void messageWriting(IoSession session, WriteRequest message, WriteFilterChainController controller) {
        LOGGER.debug("Processing a MESSAGE_WRITTING for session {}", session);

        ENCODED encoded = encoder.encode((MESSAGE) message.getMessage(), getEncodingState(session));
        message.setMessage(encoded);

        super.messageWriting(session, message, controller);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionOpened(IoSession session) {
        // Initialize the encoder and decoder state

        ENCODING_STATE encodingState = encoder.createEncoderState();
        session.setAttribute(ENCODER, encodingState);

        DECODING_STATE decodingState = decoder.createDecoderState();
        session.setAttribute(DECODER, decodingState);
        super.sessionOpened(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionClosed(IoSession session) {
        decoder.finishDecode(getDecodingState(session));
        super.sessionClosed(session);

    }

    // ----------- Helper methods ---------------------------------------------

    @SuppressWarnings("unchecked")
    protected DECODING_STATE getDecodingState(IoSession session) {
        return (DECODING_STATE) session.getAttribute(DECODER);
    }

    @SuppressWarnings("unchecked")
    protected ENCODING_STATE getEncodingState(IoSession session) {
        return (ENCODING_STATE) session.getAttribute(ENCODER);
    }

}