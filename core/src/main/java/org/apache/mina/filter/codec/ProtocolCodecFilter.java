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

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ByteBufferProxy;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.common.support.DefaultWriteFuture;
import org.apache.mina.filter.codec.support.SimpleProtocolDecoderOutput;
import org.apache.mina.filter.codec.support.SimpleProtocolEncoderOutput;
import org.apache.mina.util.SessionLog;

/**
 * An {@link IoFilter} which translates binary or protocol specific data into
 * message object and vice versa using {@link ProtocolCodecFactory},
 * {@link ProtocolEncoder}, or {@link ProtocolDecoder}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ProtocolCodecFilter extends IoFilterAdapter {
    public static final String ENCODER = ProtocolCodecFilter.class.getName()
            + ".encoder";

    public static final String DECODER = ProtocolCodecFilter.class.getName()
            + ".decoder";

    private static final String DECODER_OUT = ProtocolCodecFilter.class.getName()
            + ".decoderOut";

    private static final Class<?>[] EMPTY_PARAMS = new Class[0];

    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.wrap(new byte[0]);

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

        factory = new ProtocolCodecFactory() {
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

        factory = new ProtocolCodecFactory() {
            public ProtocolEncoder getEncoder() throws Exception {
                return encoderClass.newInstance();
            }

            public ProtocolDecoder getDecoder() throws Exception {
                return decoderClass.newInstance();
            }
        };
    }

    @Override
    public void onPreAdd(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
        if (parent.contains(ProtocolCodecFilter.class)) {
            throw new IllegalStateException(
                    "A filter chain cannot contain more than one ProtocolCodecFilter.");
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
        if (!(message instanceof ByteBuffer)) {
            nextFilter.messageReceived(session, message);
            return;
        }

        ByteBuffer in = (ByteBuffer) message;
        if (!in.hasRemaining()) {
            in.release();
            return;
        }

        ProtocolDecoder decoder = getDecoder(session);
        ProtocolDecoderOutput decoderOut = getDecoderOut(session, nextFilter);

        int oldPos = in.position();
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

            if (pde.getHexdump() == null) {
                int curPos = in.position();
                in.position(oldPos);
                pde.setHexdump(in.getHexDump());
                in.position(curPos);
            }
            throw pde;
        } finally {
            try {
                // Release the read buffer.
                in.release();
            } finally {
                decoderOut.flush();
            }
        }
    }

    @Override
    public void messageSent(NextFilter nextFilter, IoSession session,
            Object message) throws Exception {
        if (message instanceof HiddenByteBuffer) {
            return;
        }

        if (!(message instanceof MessageByteBuffer)) {
            nextFilter.messageSent(session, message);
            return;
        }

        nextFilter.messageSent(session, ((MessageByteBuffer) message).message);
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        Object message = writeRequest.getMessage();
        if (message instanceof ByteBuffer) {
            nextFilter.filterWrite(session, writeRequest);
            return;
        }

        ProtocolEncoder encoder = getEncoder(session);
        ProtocolEncoderOutputImpl encoderOut = getEncoderOut(session,
                nextFilter, writeRequest);

        try {
            encoder.encode(session, message, encoderOut);
            encoderOut.flush();
            nextFilter.filterWrite(session, new WriteRequest(
                    new MessageByteBuffer(writeRequest.getMessage()),
                    writeRequest.getFuture(), writeRequest.getDestination()));
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
        ProtocolDecoder decoder = getDecoder(session);
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

    private ProtocolEncoder getEncoder(IoSession session) throws Exception {
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

    private ProtocolDecoder getDecoder(IoSession session) throws Exception {
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
            out = new SimpleProtocolDecoderOutput(session, nextFilter);
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
            SessionLog.warn(session, "Failed to dispose: "
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
            SessionLog.warn(session, "Falied to dispose: "
                    + decoder.getClass().getName() + " (" + decoder + ')');
        }
    }

    private void disposeDecoderOut(IoSession session) {
        session.removeAttribute(DECODER_OUT);
    }

    private static class HiddenByteBuffer extends ByteBufferProxy {
        private HiddenByteBuffer(ByteBuffer buf) {
            super(buf);
        }
    }

    private static class MessageByteBuffer extends ByteBufferProxy {
        private final Object message;

        private MessageByteBuffer(Object message) {
            super(EMPTY_BUFFER);
            this.message = message;
        }

        @Override
        public void acquire() {
            // no-op since we are wraping a zero-byte buffer, this instance is to just curry the message
        }

        @Override
        public void release() {
            // no-op since we are wraping a zero-byte buffer, this instance is to just curry the message
        }
    }

    private static class ProtocolEncoderOutputImpl extends
            SimpleProtocolEncoderOutput {
        private final IoSession session;

        private final NextFilter nextFilter;

        private final WriteRequest writeRequest;

        ProtocolEncoderOutputImpl(IoSession session, NextFilter nextFilter,
                WriteRequest writeRequest) {
            this.session = session;
            this.nextFilter = nextFilter;
            this.writeRequest = writeRequest;
        }

        @Override
        protected WriteFuture doFlush(ByteBuffer buf) {
            WriteFuture future = new DefaultWriteFuture(session);
            nextFilter.filterWrite(session, new WriteRequest(
                    new HiddenByteBuffer(buf), future, writeRequest
                            .getDestination()));
            return future;
        }
    }
}
