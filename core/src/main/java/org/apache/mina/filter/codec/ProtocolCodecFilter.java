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
public class ProtocolCodecFilter<MESSAGE,ENCODED> extends AbstractIoFilter {
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
    private final ProtocolCodecFactory<MESSAGE,ENCODED> factory;

    /**
     * 
     * Creates a new instance of ProtocolCodecFilter, associating a factory for the creation of the encoder and decoder.
     * 
     * @param factory The associated factory
     */
    public ProtocolCodecFilter(final ProtocolCodecFactory<MESSAGE,ENCODED> factory) {
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        this.factory = factory;
    }

    /**
     * Creates a new instance of ProtocolCodecFilter, without any factory. The encoder/decoder factory will be created
     * as an inner class, using the two parameters (encoder and decoder).
     * 
     * @param encoder The class responsible for encoding the message
     * @param decoder The class responsible for decoding the message
     */
    public ProtocolCodecFilter(final ProtocolEncoder<MESSAGE,ENCODED> encoder, final ProtocolDecoder<ENCODED,MESSAGE> decoder) {
        if (encoder == null) {
            throw new IllegalArgumentException("encoder");
        }

        if (decoder == null) {
            throw new IllegalArgumentException("decoder");
        }

        // Create the inner Factory based on the two parameters
        this.factory = new ProtocolCodecFactory<MESSAGE,ENCODED>() {
            @Override
            public ProtocolEncoder<MESSAGE,ENCODED> getEncoder(final IoSession session) {
                return encoder;
            }

            @Override
            public ProtocolDecoder<ENCODED,MESSAGE> getDecoder(final IoSession session) {
                return decoder;
            }
        };
    }

    /**
     * Creates a new instance of ProtocolCodecFilter, without any factory. The encoder/decoder factory will be created
     * as an anonymous class, using the two parameters (encoder and decoder), which are class names. Instances for those
     * classes will be created in this constructor.
     * 
     * @param encoderClass The class responsible for encoding the message
     * @param decoderClass The class responsible for decoding the message
     */
    public ProtocolCodecFilter(final Class<? extends ProtocolEncoder<MESSAGE,ENCODED>> encoderClass,
            final Class<? extends ProtocolDecoder<ENCODED,MESSAGE>> decoderClass) {
        Assert.assertNotNull(encoderClass, "Encoder Class");
        Assert.assertNotNull(decoderClass, "Decoder Class");

        try {
            encoderClass.getConstructor(EMPTY_PARAMS);
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException("encoderClass doesn't have a public default constructor.");
        }

        try {
            decoderClass.getConstructor(EMPTY_PARAMS);
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException("decoderClass doesn't have a public default constructor.");
        }

        final ProtocolEncoder<MESSAGE,ENCODED> encoder;

        try {
            encoder = encoderClass.newInstance();
        } catch (final Exception e) {
            throw new IllegalArgumentException("encoderClass cannot be initialized");
        }

        final ProtocolDecoder<ENCODED,MESSAGE> decoder;

        try {
            decoder = decoderClass.newInstance();
        } catch (final Exception e) {
            throw new IllegalArgumentException("decoderClass cannot be initialized");
        }

        // Create the inner factory based on the two parameters.
        this.factory = new ProtocolCodecFactory<MESSAGE,ENCODED>() {
            @Override
            public ProtocolEncoder<MESSAGE,ENCODED> getEncoder(final IoSession session) {
                return encoder;
            }

            @Override
            public ProtocolDecoder<ENCODED,MESSAGE> getDecoder(final IoSession session) {
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
    public ProtocolEncoder<MESSAGE,ENCODED> getEncoder(final IoSession session) {
        return factory.getEncoder(session);
    }

    /**
     * Get the decoder instance from a given session.
     * 
     * @param session The associated session we will get the decoder from
     * @return The decoder instance, if any
     */
    public ProtocolDecoder<ENCODED,MESSAGE> getDecoder(final IoSession session) {
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
    @Override
    public void messageReceived(final IoSession session, final Object in,
            final ReadFilterChainController controller) {
        LOGGER.debug("Processing a MESSAGE_RECEIVED for session {}", session);

        final ProtocolDecoder<ENCODED,MESSAGE> decoder = getDecoder(session);

        // Loop until the codec cannot decode more
        MESSAGE msg;
        while ( (msg = decoder.decode(session, (ENCODED)in)) != null) {
            controller.callReadNextFilter(msg);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageWriting(IoSession session, WriteRequest message, WriteFilterChainController controller) {
        LOGGER.debug("Processing a MESSAGE_WRITTING for session {}", session);

        final ProtocolEncoder<MESSAGE,ENCODED> encoder = session.getAttribute(ENCODER, null);
        ENCODED encoded = encoder.encode(session,(MESSAGE) message.getMessage());
        message.setMessage(encoded);
        
        controller.callWriteNextFilter(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionOpened(final IoSession session) {
        // Initialize the encoder and decoder if we use a factory
        if (factory != null) {
            final ProtocolEncoder<MESSAGE,ENCODED> encoder = factory.getEncoder(session);
            session.setAttribute(ENCODER, encoder);
            final ProtocolDecoder<ENCODED,MESSAGE> decoder = factory.getDecoder(session);
            session.setAttribute(DECODER, decoder);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionClosed(final IoSession session) {
        disposeCodec(session);
    }

    // ----------- Helper methods ---------------------------------------------
    /**
     * Dispose the encoder, decoder, and the callback for the decoded messages.
     */
    private void disposeCodec(final IoSession session) {
        // We just remove the two instances of encoder/decoder to release resources
        // from the session
        disposeEncoder(session);
        disposeDecoder(session);
    }

    /**
     * Dispose the encoder, removing its instance from the session's attributes, and calling the associated dispose
     * method.
     */
    private void disposeEncoder(final IoSession session) {
        session.removeAttribute(ENCODER);
    }

    /**
     * Dispose the decoder, removing its instance from the session's attributes, and calling the associated dispose
     * method.
     */
    private void disposeDecoder(final IoSession session) {
        final ProtocolDecoder<ENCODED,MESSAGE> decoder = session.removeAttribute(DECODER);
        try {
            decoder.finishDecode(session);
        } catch (final Throwable t) {
            LOGGER.warn("Failed to dispose: " + decoder.getClass().getName() + " (" + decoder + ')');
        }
    }
}