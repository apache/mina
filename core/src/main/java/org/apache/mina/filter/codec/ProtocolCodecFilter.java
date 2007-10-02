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

import java.net.SocketAddress;
import java.util.Queue;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.DefaultWriteFuture;
import org.apache.mina.common.DefaultWriteRequest;
import org.apache.mina.common.AttributeKey;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionLogger;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.common.WriteRequest;
import org.apache.mina.common.WriteRequestWrapper;

/**
 * An {@link IoFilter} which translates binary or protocol specific data into
 * message object and vice versa using {@link ProtocolCodecFactory},
 * {@link ProtocolEncoder}, or {@link ProtocolDecoder}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class ProtocolCodecFilter extends IoFilterAdapter {

    private static final Class<?>[] EMPTY_PARAMS = new Class[0];
    private static final IoBuffer EMPTY_BUFFER = IoBuffer.wrap(new byte[0]);

    private final AttributeKey ENCODER = new AttributeKey(getClass(), "encoder");
    private final AttributeKey DECODER = new AttributeKey(getClass(), "decoder");
    private final AttributeKey DECODER_OUT = new AttributeKey(getClass(), "decoderOut");
    private final ProtocolCodecFactory factory;

    public ProtocolCodecFilter(ProtocolCodecFactory factory) {
        if (factory == null) {
            throw new NullPointerException("factory");
        }
        this.factory = factory;
    }

    public ProtocolCodecFilter(final ProtocolEncoder encoder,
            final ProtocolDecoder decoder) {
        if (encoder == null) {
            throw new NullPointerException("encoder");
        }
        if (decoder == null) {
            throw new NullPointerException("decoder");
        }

        this.factory = new ProtocolCodecFactory() {
            public ProtocolEncoder getEncoder() {
                return encoder;
            }

            public ProtocolDecoder getDecoder() {
                return decoder;
            }
        };
    }

    public ProtocolCodecFilter(
            final Class<? extends ProtocolEncoder> encoderClass,
            final Class<? extends ProtocolDecoder> decoderClass) {
        if (encoderClass == null) {
            throw new NullPointerException("encoderClass");
        }
        if (decoderClass == null) {
            throw new NullPointerException("decoderClass");
        }
        if (!ProtocolEncoder.class.isAssignableFrom(encoderClass)) {
            throw new IllegalArgumentException("encoderClass: "
                    + encoderClass.getName());
        }
        if (!ProtocolDecoder.class.isAssignableFrom(decoderClass)) {
            throw new IllegalArgumentException("decoderClass: "
                    + decoderClass.getName());
        }
        try {
            encoderClass.getConstructor(EMPTY_PARAMS);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "encoderClass doesn't have a public default constructor.");
        }
        try {
            decoderClass.getConstructor(EMPTY_PARAMS);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "decoderClass doesn't have a public default constructor.");
        }

        this.factory = new ProtocolCodecFactory() {
            public ProtocolEncoder getEncoder() throws Exception {
                return encoderClass.newInstance();
            }

            public ProtocolDecoder getDecoder() throws Exception {
                return decoderClass.newInstance();
            }
        };
    }
    
    public ProtocolEncoder getEncoder(IoSession session) {
        return (ProtocolEncoder) session.getAttribute(ENCODER);
    }
    
    public ProtocolDecoder getDecoder(IoSession session) {
        return (ProtocolDecoder) session.getAttribute(DECODER);
    }
    
    @Override
    public void onPreAdd(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
        if (parent.contains(this)) {
            throw new IllegalArgumentException(
                    "You can't add the same filter instance more than once.");
        }
    }

    @Override
    public void onPostRemove(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
        disposeEncoder(parent.getSession());
        disposeDecoder(parent.getSession());
        disposeDecoderOut(parent.getSession());
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session,
            Object message) throws Exception {
        if (!(message instanceof IoBuffer)) {
            nextFilter.messageReceived(session, message);
            return;
        }

        IoBuffer in = (IoBuffer) message;
        ProtocolDecoder decoder = getDecoder0(session);
        ProtocolDecoderOutput decoderOut = getDecoderOut(session, nextFilter);

        try {
            synchronized (decoderOut) {
                decoder.decode(session, in, decoderOut);
            }
        } catch (Throwable t) {
            ProtocolDecoderException pde;
            if (t instanceof ProtocolDecoderException) {
                pde = (ProtocolDecoderException) t;
            } else {
                pde = new ProtocolDecoderException(t);
            }
            pde.setHexdump(in.getHexDump());
            throw pde;
        } finally {
            decoderOut.flush();
        }
    }

    @Override
    public void messageSent(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        if (writeRequest instanceof EncodedWriteRequest) {
            return;
        }

        if (!(writeRequest instanceof MessageWriteRequest)) {
            nextFilter.messageSent(session, writeRequest);
            return;
        }

        MessageWriteRequest wrappedRequest = (MessageWriteRequest) writeRequest;
        nextFilter.messageSent(session, wrappedRequest.getWriteRequest());
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        Object message = writeRequest.getMessage();
        if (message instanceof IoBuffer) {
            nextFilter.filterWrite(session, writeRequest);
            return;
        }

        ProtocolEncoder encoder = getEncoder0(session);
        ProtocolEncoderOutputImpl encoderOut = getEncoderOut(session,
                nextFilter, writeRequest);

        try {
            encoder.encode(session, message, encoderOut);
            encoderOut.flush();
            nextFilter.filterWrite(session, new MessageWriteRequest(
                    writeRequest));
        } catch (Throwable t) {
            ProtocolEncoderException pee;
            if (t instanceof ProtocolEncoderException) {
                pee = (ProtocolEncoderException) t;
            } else {
                pee = new ProtocolEncoderException(t);
            }
            throw pee;
        }
    }

    @Override
    public void sessionClosed(NextFilter nextFilter, IoSession session)
            throws Exception {
        // Call finishDecode() first when a connection is closed.
        ProtocolDecoder decoder = getDecoder0(session);
        ProtocolDecoderOutput decoderOut = getDecoderOut(session, nextFilter);
        try {
            decoder.finishDecode(session, decoderOut);
        } catch (Throwable t) {
            ProtocolDecoderException pde;
            if (t instanceof ProtocolDecoderException) {
                pde = (ProtocolDecoderException) t;
            } else {
                pde = new ProtocolDecoderException(t);
            }
            throw pde;
        } finally {
            // Dispose all.
            disposeEncoder(session);
            disposeDecoder(session);
            disposeDecoderOut(session);
            decoderOut.flush();
        }

        nextFilter.sessionClosed(session);
    }

    private ProtocolEncoder getEncoder0(IoSession session) throws Exception {
        ProtocolEncoder encoder = (ProtocolEncoder) session
                .getAttribute(ENCODER);
        if (encoder == null) {
            encoder = factory.getEncoder();
            session.setAttribute(ENCODER, encoder);
        }
        return encoder;
    }

    private ProtocolEncoderOutputImpl getEncoderOut(IoSession session,
            NextFilter nextFilter, WriteRequest writeRequest) {
        return new ProtocolEncoderOutputImpl(session, nextFilter, writeRequest);
    }

    private ProtocolDecoder getDecoder0(IoSession session) throws Exception {
        ProtocolDecoder decoder = (ProtocolDecoder) session
                .getAttribute(DECODER);
        if (decoder == null) {
            decoder = factory.getDecoder();
            session.setAttribute(DECODER, decoder);
        }
        return decoder;
    }

    private ProtocolDecoderOutput getDecoderOut(IoSession session,
            NextFilter nextFilter) {
        ProtocolDecoderOutput out = (ProtocolDecoderOutput) session.getAttribute(DECODER_OUT);
        if (out == null) {
            out = new ProtocolDecoderOutputImpl(session, nextFilter);
            session.setAttribute(DECODER_OUT, out);
        }
        return out;
    }

    private void disposeEncoder(IoSession session) {
        ProtocolEncoder encoder = (ProtocolEncoder) session
                .removeAttribute(ENCODER);
        if (encoder == null) {
            return;
        }

        try {
            encoder.dispose(session);
        } catch (Throwable t) {
            IoSessionLogger.warn(session, "Failed to dispose: "
                    + encoder.getClass().getName() + " (" + encoder + ')');
        }
    }

    private void disposeDecoder(IoSession session) {
        ProtocolDecoder decoder = (ProtocolDecoder) session
                .removeAttribute(DECODER);
        if (decoder == null) {
            return;
        }

        try {
            decoder.dispose(session);
        } catch (Throwable t) {
            IoSessionLogger.warn(session, "Falied to dispose: "
                    + decoder.getClass().getName() + " (" + decoder + ')');
        }
    }

    private void disposeDecoderOut(IoSession session) {
        session.removeAttribute(DECODER_OUT);
    }

    private static class EncodedWriteRequest extends DefaultWriteRequest {
        private EncodedWriteRequest(IoBuffer encodedMessage,
                WriteFuture future, SocketAddress destination) {
            super(encodedMessage, future, destination);
        }
    }

    private static class MessageWriteRequest extends WriteRequestWrapper {
        private MessageWriteRequest(WriteRequest writeRequest) {
            super(writeRequest);
        }

        @Override
        public Object getMessage() {
            return EMPTY_BUFFER;
        }
    }

    private static class ProtocolDecoderOutputImpl extends
            AbstractProtocolDecoderOutput {
        private final IoSession session;
        private final NextFilter nextFilter;

        public ProtocolDecoderOutputImpl(
                IoSession session, NextFilter nextFilter) {
            this.session = session;
            this.nextFilter = nextFilter;
        }

        public void flush() {
            Queue<Object> messageQueue = getMessageQueue();
            while (!messageQueue.isEmpty()) {
                nextFilter.messageReceived(session, messageQueue.poll());
            }
        }
    }

    private static class ProtocolEncoderOutputImpl extends
            AbstractProtocolEncoderOutput {
        private final IoSession session;

        private final NextFilter nextFilter;

        private final WriteRequest writeRequest;

        public ProtocolEncoderOutputImpl(IoSession session,
                NextFilter nextFilter, WriteRequest writeRequest) {
            this.session = session;
            this.nextFilter = nextFilter;
            this.writeRequest = writeRequest;
        }

        public WriteFuture flush() {
            Queue<IoBuffer> bufferQueue = getBufferQueue();
            WriteFuture future = null;
            for (;;) {
                IoBuffer buf = bufferQueue.poll();
                if (buf == null) {
                    break;
                }

                // Flush only when the buffer has remaining.
                if (buf.hasRemaining()) {
                    future = new DefaultWriteFuture(session);
                    nextFilter.filterWrite(session, new EncodedWriteRequest(buf,
                            future, writeRequest.getDestination()));
                }
            }

            if (future == null) {
                future = DefaultWriteFuture.newNotWrittenFuture(session);
            }

            return future;
        }
    }
}
