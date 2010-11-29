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

    public DemuxingProtocolCodecFactory() {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    public ProtocolEncoder getEncoder(IoSession session) throws Exception {
        return encoder;
    }

    /**
     * {@inheritDoc}
     */
    public ProtocolDecoder getDecoder(IoSession session) throws Exception {
        return decoder;
    }
    
    @SuppressWarnings("unchecked")
    public void addMessageEncoder(Class<?> messageType, Class<? extends MessageEncoder> encoderClass) {
        this.encoder.addMessageEncoder(messageType, encoderClass);
    }

    public <T> void addMessageEncoder(Class<T> messageType, MessageEncoder<? super T> encoder) {
        this.encoder.addMessageEncoder(messageType, encoder);
    }

    public <T> void addMessageEncoder(Class<T> messageType, MessageEncoderFactory<? super T> factory) {
        this.encoder.addMessageEncoder(messageType, factory);
    }
    
    @SuppressWarnings("unchecked")
    public void addMessageEncoder(Iterable<Class<?>> messageTypes, Class<? extends MessageEncoder> encoderClass) {
        for (Class<?> messageType : messageTypes) {
            addMessageEncoder(messageType, encoderClass);
        }
    }
    
    public <T> void addMessageEncoder(Iterable<Class<? extends T>> messageTypes, MessageEncoder<? super T> encoder) {
        for (Class<? extends T> messageType : messageTypes) {
            addMessageEncoder(messageType, encoder);
        }
    }
    
    public <T> void addMessageEncoder(Iterable<Class<? extends T>> messageTypes, MessageEncoderFactory<? super T> factory) {
        for (Class<? extends T> messageType : messageTypes) {
            addMessageEncoder(messageType, factory);
        }
    }
    
    public void addMessageDecoder(Class<? extends MessageDecoder> decoderClass) {
        this.decoder.addMessageDecoder(decoderClass);
    }

    public void addMessageDecoder(MessageDecoder decoder) {
        this.decoder.addMessageDecoder(decoder);
    }

    public void addMessageDecoder(MessageDecoderFactory factory) {
        this.decoder.addMessageDecoder(factory);
    }
}
