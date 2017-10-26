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
package org.apache.mina.filter.codec.demux;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

/**
 * A convenience {@link ProtocolCodecFactory} that provides {@link DemuxingProtocolEncoder}
 * and {@link DemuxingProtocolDecoder} as a pair.
 * <p>
 * {@link DemuxingProtocolEncoder} and {@link DemuxingProtocolDecoder} demultiplex
 * incoming messages and buffers to appropriate {@link MessageEncoder}s and 
 * {@link MessageDecoder}s.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DemuxingProtocolCodecFactory implements ProtocolCodecFactory {

    private final DemuxingProtocolEncoder encoder = new DemuxingProtocolEncoder();

    private final DemuxingProtocolDecoder decoder = new DemuxingProtocolDecoder();

    /**
     * {@inheritDoc}
     */
    @Override
    public ProtocolEncoder getEncoder(IoSession session) throws Exception {
        return encoder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProtocolDecoder getDecoder(IoSession session) throws Exception {
        return decoder;
    }

    /**
     * Adds a new message encoder for a given message type
     * 
     * @param messageType The message type
     * @param encoderClass The associated encoder class
     */
    public void addMessageEncoder(Class<?> messageType, Class<? extends MessageEncoder> encoderClass) {
        this.encoder.addMessageEncoder(messageType, encoderClass);
    }

    /**
     * Adds a new message encoder for a given message type
     * 
     * @param <T> The message type
     * @param messageType The message type
     * @param encoder The associated encoder instance
     */
    public <T> void addMessageEncoder(Class<T> messageType, MessageEncoder<? super T> encoder) {
        this.encoder.addMessageEncoder(messageType, encoder);
    }

    /**
     * Adds a new message encoder for a given message type
     * 
     * @param <T> The message type
     * @param messageType The message type
     * @param factory The associated encoder factory
     */
    public <T> void addMessageEncoder(Class<T> messageType, MessageEncoderFactory<? super T> factory) {
        this.encoder.addMessageEncoder(messageType, factory);
    }

    /**
     * Adds a new message encoder for a list of message types
     * 
     * @param messageTypes The message types
     * @param encoderClass The associated encoder class
     */
    public void addMessageEncoder(Iterable<Class<?>> messageTypes, Class<? extends MessageEncoder> encoderClass) {
        for (Class<?> messageType : messageTypes) {
            addMessageEncoder(messageType, encoderClass);
        }
    }

    /**
     * Adds a new message encoder for a list of message types
     * 
     * @param <T> The message type
     * @param messageTypes The messages types
     * @param encoder The associated encoder instance
     */
    public <T> void addMessageEncoder(Iterable<Class<? extends T>> messageTypes, MessageEncoder<? super T> encoder) {
        for (Class<? extends T> messageType : messageTypes) {
            addMessageEncoder(messageType, encoder);
        }
    }

    /**
     * Adds a new message encoder for a list of message types
     * 
     * @param <T> The message type
     * @param messageTypes The messages types
     * @param factory The associated encoder factory
     */
    public <T> void addMessageEncoder(Iterable<Class<? extends T>> messageTypes,
            MessageEncoderFactory<? super T> factory) {
        for (Class<? extends T> messageType : messageTypes) {
            addMessageEncoder(messageType, factory);
        }
    }

    /**
     * Adds a new message decoder
     * 
     * @param decoderClass The associated decoder class
     */
    public void addMessageDecoder(Class<? extends MessageDecoder> decoderClass) {
        this.decoder.addMessageDecoder(decoderClass);
    }

    /**
     * Adds a new message decoder
     * 
     * @param decoder The associated decoder instance
     */
    public void addMessageDecoder(MessageDecoder decoder) {
        this.decoder.addMessageDecoder(decoder);
    }

    /**
     * Adds a new message decoder
     * 
     * @param factory The associated decoder factory
     */
    public void addMessageDecoder(MessageDecoderFactory factory) {
        this.decoder.addMessageDecoder(factory);
    }
}
