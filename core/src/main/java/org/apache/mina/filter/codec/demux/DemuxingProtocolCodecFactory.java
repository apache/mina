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

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderException;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.util.IdentityHashSet;

/**
 * A composite {@link ProtocolCodecFactory} that consists of multiple
 * {@link MessageEncoder}s and {@link MessageDecoder}s.
 * {@link ProtocolEncoder} and {@link ProtocolDecoder} this factory
 * returns demultiplex incoming messages and buffers to
 * appropriate {@link MessageEncoder}s and {@link MessageDecoder}s. 
 * 
 * <h2>Disposing resources acquired by {@link MessageEncoder} and {@link MessageDecoder}</h2>
 * <p>
 * Make your {@link MessageEncoder} and {@link MessageDecoder} to put all
 * resources that need to be released as a session attribute.  {@link #disposeCodecResources(IoSession)}
 * method will be invoked when a session is closed.  Override {@link #disposeCodecResources(IoSession)}
 * to release the resources you've put as an attribute.
 * <p>
 * We didn't provide any <tt>dispose</tt> method for {@link MessageEncoder} and {@link MessageDecoder}
 * because they can give you a big performance penalty in case you have a lot of
 * message types to handle.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 * 
 * @see MessageEncoder
 * @see MessageDecoder
 */
public class DemuxingProtocolCodecFactory implements ProtocolCodecFactory {
    private MessageDecoderFactory[] decoderFactories = new MessageDecoderFactory[0];

    private MessageEncoderFactory[] encoderFactories = new MessageEncoderFactory[0];

    private static final Class<?>[] EMPTY_PARAMS = new Class[0];

    public DemuxingProtocolCodecFactory() {
    }

    public void register(Class<?> encoderOrDecoderClass) {
        if (encoderOrDecoderClass == null) {
            throw new NullPointerException("encoderOrDecoderClass");
        }

        try {
            encoderOrDecoderClass.getConstructor(EMPTY_PARAMS);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "The specifiec class doesn't have a public default constructor.");
        }

        boolean registered = false;
        if (MessageEncoder.class.isAssignableFrom(encoderOrDecoderClass)) {
            register(new DefaultConstructorMessageEncoderFactory(
                    encoderOrDecoderClass));
            registered = true;
        }

        if (MessageDecoder.class.isAssignableFrom(encoderOrDecoderClass)) {
            register(new DefaultConstructorMessageDecoderFactory(
                    encoderOrDecoderClass));
            registered = true;
        }

        if (!registered) {
            throw new IllegalArgumentException("Unregisterable type: "
                    + encoderOrDecoderClass);
        }
    }

    public void register(MessageEncoder encoder) {
        register(new SingletonMessageEncoderFactory(encoder));
    }

    public void register(MessageEncoderFactory factory) {
        if (factory == null) {
            throw new NullPointerException("factory");
        }
        MessageEncoderFactory[] encoderFactories = this.encoderFactories;
        MessageEncoderFactory[] newEncoderFactories = new MessageEncoderFactory[encoderFactories.length + 1];
        System.arraycopy(encoderFactories, 0, newEncoderFactories, 0,
                encoderFactories.length);
        newEncoderFactories[encoderFactories.length] = factory;
        this.encoderFactories = newEncoderFactories;
    }

    public void register(MessageDecoder decoder) {
        register(new SingletonMessageDecoderFactory(decoder));
    }

    public void register(MessageDecoderFactory factory) {
        if (factory == null) {
            throw new NullPointerException("factory");
        }
        MessageDecoderFactory[] decoderFactories = this.decoderFactories;
        MessageDecoderFactory[] newDecoderFactories = new MessageDecoderFactory[decoderFactories.length + 1];
        System.arraycopy(decoderFactories, 0, newDecoderFactories, 0,
                decoderFactories.length);
        newDecoderFactories[decoderFactories.length] = factory;
        this.decoderFactories = newDecoderFactories;
    }

    public ProtocolEncoder getEncoder() throws Exception {
        return new ProtocolEncoderImpl();
    }

    public ProtocolDecoder getDecoder() throws Exception {
        return new ProtocolDecoderImpl();
    }

    /**
     * Implement this method to release all resources acquired to perform
     * encoding and decoding messages for the specified <tt>session</tt>.
     * By default, this method does nothing.
     * 
     * @param session the session that requires resource deallocation now
     */
    protected void disposeCodecResources(IoSession session) {
        // Do nothing by default; let users implement it as they want.

        // This statement is just to avoid compiler warning.  Please ignore. 
        session.getTransportType();
    }

    private class ProtocolEncoderImpl implements ProtocolEncoder {
        private final Map<Class<?>, MessageEncoder> encoders = new IdentityHashMap<Class<?>, MessageEncoder>();

        private ProtocolEncoderImpl() throws Exception {
            MessageEncoderFactory[] encoderFactories = DemuxingProtocolCodecFactory.this.encoderFactories;
            for (int i = encoderFactories.length - 1; i >= 0; i--) {
                MessageEncoder encoder = encoderFactories[i].getEncoder();
                Set<Class<?>> messageTypes = encoder.getMessageTypes();
                if (messageTypes == null) {
                    throw new IllegalStateException(encoder.getClass()
                            .getName()
                            + "#getMessageTypes() may not return null.");
                }

                Iterator<Class<?>> it = messageTypes.iterator();
                while (it.hasNext()) {
                    Class<?> type = it.next();
                    encoders.put(type, encoder);
                }
            }
        }

        public void encode(IoSession session, Object message,
                ProtocolEncoderOutput out) throws Exception {
            Class<?> type = message.getClass();
            MessageEncoder encoder = findEncoder(type);
            if (encoder == null) {
                throw new ProtocolEncoderException("Unexpected message type: "
                        + type);
            }

            encoder.encode(session, message, out);
        }

        private MessageEncoder findEncoder(Class<?> type) {
            MessageEncoder encoder = encoders.get(type);
            if (encoder == null) {
                encoder = findEncoder(type, new IdentityHashSet<Class<?>>());
            }

            return encoder;
        }

        private MessageEncoder findEncoder(Class<?> type,
                Set<Class<?>> triedClasses) {
            MessageEncoder encoder;

            if (triedClasses.contains(type))
                return null;
            triedClasses.add(type);

            encoder = encoders.get(type);
            if (encoder == null) {
                encoder = findEncoder(type, triedClasses);
                if (encoder != null)
                    return encoder;

                Class<?>[] interfaces = type.getInterfaces();
                for (int i = 0; i < interfaces.length; i++) {
                    encoder = findEncoder(interfaces[i], triedClasses);
                    if (encoder != null)
                        return encoder;
                }

                return null;
            } else
                return encoder;
        }

        public void dispose(IoSession session) throws Exception {
            DemuxingProtocolCodecFactory.this.disposeCodecResources(session);
        }
    }

    private class ProtocolDecoderImpl extends CumulativeProtocolDecoder {
        private final MessageDecoder[] decoders;

        private MessageDecoder currentDecoder;

        protected ProtocolDecoderImpl() throws Exception {
            MessageDecoderFactory[] decoderFactories = DemuxingProtocolCodecFactory.this.decoderFactories;
            decoders = new MessageDecoder[decoderFactories.length];
            for (int i = decoderFactories.length - 1; i >= 0; i--) {
                decoders[i] = decoderFactories[i].getDecoder();
            }
        }

        @Override
        protected boolean doDecode(IoSession session, ByteBuffer in,
                ProtocolDecoderOutput out) throws Exception {
            if (currentDecoder == null) {
                MessageDecoder[] decoders = this.decoders;
                int undecodables = 0;
                for (int i = decoders.length - 1; i >= 0; i--) {
                    MessageDecoder decoder = decoders[i];
                    int limit = in.limit();
                    int pos = in.position();

                    MessageDecoderResult result;
                    try {
                        result = decoder.decodable(session, in);
                    } finally {
                        in.position(pos);
                        in.limit(limit);
                    }

                    if (result == MessageDecoder.OK) {
                        currentDecoder = decoder;
                        break;
                    } else if (result == MessageDecoder.NOT_OK) {
                        undecodables++;
                    } else if (result != MessageDecoder.NEED_DATA) {
                        throw new IllegalStateException(
                                "Unexpected decode result (see your decodable()): "
                                        + result);
                    }
                }

                if (undecodables == decoders.length) {
                    // Throw an exception if all decoders cannot decode data.
                    String dump = in.getHexDump();
                    in.position(in.limit()); // Skip data
                    ProtocolDecoderException e = new ProtocolDecoderException(
                            "No appropriate message decoder: " + dump);
                    e.setHexdump(dump);
                    throw e;
                }

                if (currentDecoder == null) {
                    // Decoder is not determined yet (i.e. we need more data)
                    return false;
                }
            }

            MessageDecoderResult result = currentDecoder.decode(session, in,
                    out);
            if (result == MessageDecoder.OK) {
                currentDecoder = null;
                return true;
            } else if (result == MessageDecoder.NEED_DATA) {
                return false;
            } else if (result == MessageDecoder.NOT_OK) {
                currentDecoder = null;
                throw new ProtocolDecoderException(
                        "Message decoder returned NOT_OK.");
            } else {
                currentDecoder = null;
                throw new IllegalStateException(
                        "Unexpected decode result (see your decode()): "
                                + result);
            }
        }

        @Override
        public void finishDecode(IoSession session, ProtocolDecoderOutput out)
                throws Exception {
            if (currentDecoder == null) {
                return;
            }

            currentDecoder.finishDecode(session, out);
        }

        @Override
        public void dispose(IoSession session) throws Exception {
            super.dispose(session);

            // ProtocolEncoder.dispose() already called disposeCodec(),
            // so there's nothing more we need to do.
        }
    }

    private static class SingletonMessageEncoderFactory implements
            MessageEncoderFactory {
        private final MessageEncoder encoder;

        private SingletonMessageEncoderFactory(MessageEncoder encoder) {
            if (encoder == null) {
                throw new NullPointerException("encoder");
            }
            this.encoder = encoder;
        }

        public MessageEncoder getEncoder() {
            return encoder;
        }
    }

    private static class SingletonMessageDecoderFactory implements
            MessageDecoderFactory {
        private final MessageDecoder decoder;

        private SingletonMessageDecoderFactory(MessageDecoder decoder) {
            if (decoder == null) {
                throw new NullPointerException("decoder");
            }
            this.decoder = decoder;
        }

        public MessageDecoder getDecoder() {
            return decoder;
        }
    }

    private static class DefaultConstructorMessageEncoderFactory implements
            MessageEncoderFactory {
        private final Class<?> encoderClass;

        private DefaultConstructorMessageEncoderFactory(Class<?> encoderClass) {
            if (encoderClass == null) {
                throw new NullPointerException("encoderClass");
            }

            if (!MessageEncoder.class.isAssignableFrom(encoderClass)) {
                throw new IllegalArgumentException(
                        "encoderClass is not assignable to MessageEncoder");
            }
            this.encoderClass = encoderClass;
        }

        public MessageEncoder getEncoder() throws Exception {
            return (MessageEncoder) encoderClass.newInstance();
        }
    }

    private static class DefaultConstructorMessageDecoderFactory implements
            MessageDecoderFactory {
        private final Class<?> decoderClass;

        private DefaultConstructorMessageDecoderFactory(Class<?> decoderClass) {
            if (decoderClass == null) {
                throw new NullPointerException("decoderClass");
            }

            if (!MessageDecoder.class.isAssignableFrom(decoderClass)) {
                throw new IllegalArgumentException(
                        "decoderClass is not assignable to MessageDecoder");
            }
            this.decoderClass = decoderClass;
        }

        public MessageDecoder getDecoder() throws Exception {
            return (MessageDecoder) decoderClass.newInstance();
        }
    }
}
