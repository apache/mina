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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.UnknownMessageTypeException;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.util.CopyOnWriteMap;
import org.apache.mina.util.IdentityHashSet;

/**
 * A composite {@link ProtocolEncoder} that demultiplexes incoming message
 * encoding requests into an appropriate {@link MessageEncoder}.
 *
 * <h2>Disposing resources acquired by {@link MessageEncoder}</h2>
 * <p>
 * Override {@link #dispose(IoSession)} method. Please don't forget to call
 * <tt>super.dispose()</tt>.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 * @see MessageEncoderFactory
 * @see MessageEncoder
 */
public class DemuxingProtocolEncoder implements ProtocolEncoder {
    
    private final AttributeKey STATE = new AttributeKey(getClass(), "state");

    @SuppressWarnings("unchecked")
    private final Map<Class<?>, MessageEncoderFactory> type2encoderFactory = new CopyOnWriteMap<Class<?>, MessageEncoderFactory>();

    private static final Class<?>[] EMPTY_PARAMS = new Class[0];

    public DemuxingProtocolEncoder() {
        // Do nothing
    }

    @SuppressWarnings("unchecked")
    public void addMessageEncoder(Class<?> messageType, Class<? extends MessageEncoder> encoderClass) {
        if (encoderClass == null) {
            throw new IllegalArgumentException("encoderClass");
        }

        try {
            encoderClass.getConstructor(EMPTY_PARAMS);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "The specified class doesn't have a public default constructor.");
        }

        boolean registered = false;
        if (MessageEncoder.class.isAssignableFrom(encoderClass)) {
            addMessageEncoder(messageType, new DefaultConstructorMessageEncoderFactory(encoderClass));
            registered = true;
        }

        if (!registered) {
            throw new IllegalArgumentException(
                    "Unregisterable type: " + encoderClass);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void addMessageEncoder(Class<T> messageType, MessageEncoder<? super T> encoder) {
        addMessageEncoder(messageType, new SingletonMessageEncoderFactory(encoder));
    }

    public <T> void addMessageEncoder(Class<T> messageType, MessageEncoderFactory<? super T> factory) {
        if (messageType == null) {
            throw new IllegalArgumentException("messageType");
        }
        
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }
        
        synchronized (type2encoderFactory) {
            if (type2encoderFactory.containsKey(messageType)) {
                throw new IllegalStateException(
                        "The specified message type (" + messageType.getName() + ") is registered already.");
            }
            
            type2encoderFactory.put(messageType, factory);
        }
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
    
    /**
     * {@inheritDoc}
     */
    public void encode(IoSession session, Object message,
            ProtocolEncoderOutput out) throws Exception {
        State state = getState(session);
        MessageEncoder<Object> encoder = findEncoder(state, message.getClass());
        if (encoder != null) {
            encoder.encode(session, message, out);
        } else {
            throw new UnknownMessageTypeException(
                    "No message encoder found for message: " + message);
        }
    }

    protected MessageEncoder<Object> findEncoder(State state, Class<?> type) {
        return findEncoder(state, type, null);
    }

    @SuppressWarnings("unchecked")
    private MessageEncoder<Object> findEncoder(
            State state, Class type, Set<Class> triedClasses) {
        MessageEncoder encoder = null;

        if (triedClasses != null && triedClasses.contains(type)) {
            return null;
        }

        /*
         * Try the cache first.
         */
        encoder = state.findEncoderCache.get(type);
        if (encoder != null) {
            return encoder;
        }

        /*
         * Try the registered encoders for an immediate match.
         */
        encoder = state.type2encoder.get(type);

        if (encoder == null) {
            /*
             * No immediate match could be found. Search the type's interfaces.
             */

            if (triedClasses == null) {
                triedClasses = new IdentityHashSet<Class>();
            }
            triedClasses.add(type);

            Class[] interfaces = type.getInterfaces();
            for (Class element : interfaces) {
                encoder = findEncoder(state, element, triedClasses);
                if (encoder != null) {
                    break;
                }
            }
        }

        if (encoder == null) {
            /*
             * No match in type's interfaces could be found. Search the
             * superclass.
             */

            Class superclass = type.getSuperclass();
            if (superclass != null) {
                encoder = findEncoder(state, superclass);
            }
        }

        /*
         * Make sure the encoder is added to the cache. By updating the cache
         * here all the types (superclasses and interfaces) in the path which
         * led to a match will be cached along with the immediate message type.
         */
        if (encoder != null) {
            state.findEncoderCache.put(type, encoder);
        }

        return encoder;
    }

    /**
     * {@inheritDoc}
     */
    public void dispose(IoSession session) throws Exception {
        session.removeAttribute(STATE);
    }
    
    private State getState(IoSession session) throws Exception {
        State state = (State) session.getAttribute(STATE);
        if (state == null) {
            state = new State();
            State oldState = (State) session.setAttributeIfAbsent(STATE, state);
            if (oldState != null) {
                state = oldState;
            }
        }
        return state;
    }
    
    private class State {
        @SuppressWarnings("unchecked")
        private final Map<Class<?>, MessageEncoder> findEncoderCache = new ConcurrentHashMap<Class<?>, MessageEncoder>();

        @SuppressWarnings("unchecked")
        private final Map<Class<?>, MessageEncoder> type2encoder = new ConcurrentHashMap<Class<?>, MessageEncoder>();
        
        @SuppressWarnings("unchecked")
        private State() throws Exception {
            for (Map.Entry<Class<?>, MessageEncoderFactory> e: type2encoderFactory.entrySet()) {
                type2encoder.put(e.getKey(), e.getValue().getEncoder());
            }
        }
    }

    private static class SingletonMessageEncoderFactory<T> implements
            MessageEncoderFactory<T> {
        private final MessageEncoder<T> encoder;

        private SingletonMessageEncoderFactory(MessageEncoder<T> encoder) {
            if (encoder == null) {
                throw new IllegalArgumentException("encoder");
            }
            this.encoder = encoder;
        }

        public MessageEncoder<T> getEncoder() {
            return encoder;
        }
    }

    private static class DefaultConstructorMessageEncoderFactory<T> implements
            MessageEncoderFactory<T> {
        private final Class<MessageEncoder<T>> encoderClass;

        private DefaultConstructorMessageEncoderFactory(Class<MessageEncoder<T>> encoderClass) {
            if (encoderClass == null) {
                throw new IllegalArgumentException("encoderClass");
            }

            if (!MessageEncoder.class.isAssignableFrom(encoderClass)) {
                throw new IllegalArgumentException(
                        "encoderClass is not assignable to MessageEncoder");
            }
            this.encoderClass = encoderClass;
        }

        public MessageEncoder<T> getEncoder() throws Exception {
            return encoderClass.newInstance();
        }
    }
}
