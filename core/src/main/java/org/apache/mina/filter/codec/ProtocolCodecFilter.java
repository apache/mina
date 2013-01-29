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
 * An {@link IoFilter} which translates binary or protocol specific data into message objects and vice versa using
 * {@link ProtocolCodecFactory}, {@link ProtocolEncoder}, or {@link ProtocolDecoder}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ProtocolCodecFilter<MESSAGE, ENCODED> extends AbstractIoFilter {
    /** A logger for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolCodecFilter.class);

    private static final Class<?>[] EMPTY_PARAMS = new Class[0];

    /** key for session attribute holding the encoder */
    @SuppressWarnings("rawtypes")
    private final AttributeKey<ProtocolEncoder> ENCODER = new AttributeKey<ProtocolEncoder>(ProtocolEncoder.class,
            "internal_encoder");

    /** key for session attribute holding the decoder */
    @SuppressWarnings("rawtypes")
    private final AttributeKey<ProtocolDecoder> DECODER = new AttributeKey<ProtocolDecoder>(ProtocolDecoder.class,
            "internal_decoder");

    /** The factory responsible for creating the encoder and decoder */
    private final ProtocolCodecFactory<MESSAGE, ENCODED> factory;

    /**
     * 
     * Creates a new instance of ProtocolCodecFilter, associating a factory for the creation of the encoder and decoder.
     * 
     * @param factory The associated factory
     */
    public ProtocolCodecFilter(ProtocolCodecFactory<MESSAGE, ENCODED> factory) {
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        this.factory = factory;
    }

    /**
     * Creates a new instance of ProtocolCodecFilter, without any factory. The encoder/decoder factory will be created
     * as an anonymous class, using the two parameters (encoder and decoder), which are class names. Instances for those
     * classes will be created in this constructor.
     * 
     * @param encoderClass The class responsible for encoding the message
     * @param decoderClass The class responsible for decoding the message
     */
    public ProtocolCodecFilter(Class<? extends ProtocolEncoder<MESSAGE, ENCODED>> encoderClass,
            Class<? extends ProtocolDecoder<ENCODED, MESSAGE>> decoderClass) {
        Assert.assertNotNull(encoderClass, "Encoder Class");
        Assert.assertNotNull(decoderClass, "Decoder Class");

        try {
            encoderClass.getConstructor(EMPTY_PARAMS);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("encoderClass doesn't have a public default constructor.");
        }

        try {
            decoderClass.getConstructor(EMPTY_PARAMS);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("decoderClass doesn't have a public default constructor.");
        }

        final ProtocolEncoder<MESSAGE, ENCODED> encoder;

        try {
            encoder = encoderClass.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("encoderClass cannot be initialized");
        }

        final ProtocolDecoder<ENCODED, MESSAGE> decoder;

        try {
            decoder = decoderClass.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("decoderClass cannot be initialized");
        }

        // Create the inner factory based on the two parameters.
        this.factory = new ProtocolCodecFactory<MESSAGE, ENCODED>() {
            @Override
            public ProtocolEncoder<MESSAGE, ENCODED> getEncoder(IoSession session) {
                return encoder;
            }

            @Override
            public ProtocolDecoder<ENCODED, MESSAGE> getDecoder(IoSession session) {
                return decoder;
            }
        };
    }

    /**
     * Get the encoder instance from a given session.
     * 
     * @param session The associated session we will get the encoder from
     * @return The encoder instance, if any
     */
    public ProtocolEncoder<MESSAGE, ENCODED> getEncoder(IoSession session) {
        return factory.getEncoder(session);
    }

    /**
     * Get the decoder instance from a given session.
     * 
     * @param session The associated session we will get the decoder from
     * @return The decoder instance, if any
     */
    public ProtocolDecoder<ENCODED, MESSAGE> getDecoder(IoSession session) {
        return factory.getDecoder(session);
    }

    /**
     * Process the incoming message, calling the session decoder. As the incoming buffer might contains more than one
     * messages, we have to loop until the decoder throws an exception. <code>
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

        ProtocolDecoder<ENCODED, MESSAGE> decoder = getDecoder(session);

        // Loop until the codec cannot decode more
        MESSAGE[] msg;
        try {
            while ((msg = decoder.decode((ENCODED) in)) != null) {
                for (MESSAGE m : msg) {
                    controller.callReadNextFilter(m);
                }
            }
        } catch (ProtocolDecoderException e) {
            LOGGER.debug("decoding exception : ", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void messageWriting(IoSession session, WriteRequest message, WriteFilterChainController controller) {
        LOGGER.debug("Processing a MESSAGE_WRITTING for session {}", session);

        ProtocolEncoder<MESSAGE, ENCODED> encoder = getEncoder(session);
        ENCODED encoded = encoder.encode((MESSAGE) message.getMessage());
        message.setMessage(encoded);

        controller.callWriteNextFilter(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionOpened(IoSession session) {
        // Initialize the encoder and decoder if we use a factory
        if (factory != null) {
            ProtocolEncoder<MESSAGE, ENCODED> encoder = factory.getEncoder(session);
            session.setAttribute(ENCODER, encoder);
            ProtocolDecoder<ENCODED, MESSAGE> decoder = factory.getDecoder(session);
            session.setAttribute(DECODER, decoder);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionClosed(IoSession session) {
        disposeCodec(session);
    }

    // ----------- Helper methods ---------------------------------------------
    /**
     * Dispose the encoder, decoder, and the callback for the decoded messages.
     */
    private void disposeCodec(IoSession session) {
        // We just remove the two instances of encoder/decoder to release resources
        // from the session
        disposeEncoder(session);
        disposeDecoder(session);
    }

    /**
     * Dispose the encoder, removing its instance from the session's attributes, and calling the associated dispose
     * method.
     */
    private void disposeEncoder(IoSession session) {
        session.removeAttribute(ENCODER);
    }

    /**
     * Dispose the decoder, removing its instance from the session's attributes, and calling the associated dispose
     * method.
     */
    private void disposeDecoder(IoSession session) {
        @SuppressWarnings("unchecked")
        ProtocolDecoder<ENCODED, MESSAGE> decoder = session.removeAttribute(DECODER);
        try {
            decoder.finishDecode();
        } catch (Throwable t) {
            LOGGER.warn("Failed to dispose: " + decoder.getClass().getName() + " (" + decoder + ')');
        }
    }
}