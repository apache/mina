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
import org.apache.mina.filterchain.ReadFilterChainController;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.apache.mina.session.AttributeKey;
import org.apache.mina.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link IoFilter} which translates binary or protocol specific data into message objects and vice versa using
 * {@link ProtocolCodecFactory}, {@link ProtocolEncoder}, or {@link ProtocolDecoder}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ProtocolCodecFilter extends AbstractIoFilter {
    /** A logger for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolCodecFilter.class);

    private static final Class<?>[] EMPTY_PARAMS = new Class[0];

    /** key for session attribute holding the encoder */
    private final AttributeKey<ProtocolEncoder> ENCODER = new AttributeKey<ProtocolEncoder>(ProtocolEncoder.class,
            "internal_encoder");

    /** key for session attribute holding the decoder */
    private final AttributeKey<ProtocolDecoder> DECODER = new AttributeKey<ProtocolDecoder>(ProtocolDecoder.class,
            "internal_decoder");

    /** The factory responsible for creating the encoder and decoder */
    private final ProtocolCodecFactory factory;

    /**
     * 
     * Creates a new instance of ProtocolCodecFilter, associating a factory for the creation of the encoder and decoder.
     * 
     * @param factory The associated factory
     */
    public ProtocolCodecFilter(final ProtocolCodecFactory factory) {
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
    public ProtocolCodecFilter(final ProtocolEncoder encoder, final ProtocolDecoder decoder) {
        if (encoder == null) {
            throw new IllegalArgumentException("encoder");
        }

        if (decoder == null) {
            throw new IllegalArgumentException("decoder");
        }

        // Create the inner Factory based on the two parameters
        this.factory = new ProtocolCodecFactory() {
            @Override
            public ProtocolEncoder getEncoder(final IoSession session) {
                return encoder;
            }

            @Override
            public ProtocolDecoder getDecoder(final IoSession session) {
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
    public ProtocolCodecFilter(final Class<? extends ProtocolEncoder> encoderClass,
            final Class<? extends ProtocolDecoder> decoderClass) {
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

        final ProtocolEncoder encoder;

        try {
            encoder = encoderClass.newInstance();
        } catch (final Exception e) {
            throw new IllegalArgumentException("encoderClass cannot be initialized");
        }

        final ProtocolDecoder decoder;

        try {
            decoder = decoderClass.newInstance();
        } catch (final Exception e) {
            throw new IllegalArgumentException("decoderClass cannot be initialized");
        }

        // Create the inner factory based on the two parameters.
        this.factory = new ProtocolCodecFactory() {
            @Override
            public ProtocolEncoder getEncoder(final IoSession session) {
                return encoder;
            }

            @Override
            public ProtocolDecoder getDecoder(final IoSession session) {
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
    public ProtocolEncoder getEncoder(final IoSession session) {
        return factory.getEncoder(session);
    }

    /**
     * Get the decoder instance from a given session.
     * 
     * @param session The associated session we will get the decoder from
     * @return The decoder instance, if any
     */
    public ProtocolDecoder getDecoder(final IoSession session) {
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
    public void messageReceived(final IoSession session, final Object message,
            final ReadFilterChainController controller) {
        LOGGER.debug("Processing a MESSAGE_RECEIVED for session {}", session);

        if (!(message instanceof ByteBuffer)) {
            controller.callReadNextFilter(message);
            return;
        }

        final ByteBuffer in = (ByteBuffer) message;
        final ProtocolDecoder decoder = getDecoder(session);

        // Loop until we don't have anymore byte in the buffer,
        // or until the decoder throws an unrecoverable exception or
        // can't decoder a message, because there are not enough
        // data in the buffer
        while (in.hasRemaining()) {
            // Call the decoder with the read bytes
            decoder.decode(session, in, controller);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageWriting(final IoSession session, final Object message,
            final WriteFilterChainController controller) {
        LOGGER.debug("Processing a MESSAGE_WRITTING for session {}", session);

        final ProtocolEncoder encoder = session.getAttribute(ENCODER, null);

        encoder.encode(session, message, controller);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionOpened(final IoSession session) {
        // Initialize the encoder and decoder if we use a factory
        if (factory != null) {
            final ProtocolEncoder encoder = factory.getEncoder(session);
            session.setAttribute(ENCODER, encoder);
            final ProtocolDecoder decoder = factory.getDecoder(session);
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
        final ProtocolEncoder encoder = session.removeAttribute(ENCODER);

        if (encoder == null) {
            return;
        }

        try {
            encoder.dispose(session);
        } catch (final Throwable t) {
            LOGGER.warn("Failed to dispose: " + encoder.getClass().getName() + " (" + encoder + ')');
        }
    }

    /**
     * Dispose the decoder, removing its instance from the session's attributes, and calling the associated dispose
     * method.
     */
    private void disposeDecoder(final IoSession session) {
        final ProtocolDecoder decoder = session.removeAttribute(DECODER);
        if (decoder == null) {
            return;
        }

        try {
            decoder.dispose(session);
        } catch (final Throwable t) {
            LOGGER.warn("Failed to dispose: " + decoder.getClass().getName() + " (" + decoder + ')');
        }
    }
}