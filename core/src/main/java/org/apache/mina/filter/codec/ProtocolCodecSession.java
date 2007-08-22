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

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Queue;

import org.apache.mina.common.AbstractIoAcceptor;
import org.apache.mina.common.AbstractIoFilterChain;
import org.apache.mina.common.AbstractIoSession;
import org.apache.mina.common.AbstractIoSessionConfig;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.DefaultTransportMetadata;
import org.apache.mina.common.DefaultWriteFuture;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.TransportMetadata;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.common.WriteRequest;

/**
 * A virtual {@link IoSession} that provides {@link ProtocolEncoderOutput}
 * and {@link ProtocolDecoderOutput}.  It is useful for unit testing
 * codec and reusing codec for non-network use (e.g. serialization).
 * 
 * <h2>Decoding</h2>
 * <pre>
 * ProtocolCodecSession session = new ProtocolCodecSession();
 * ProtocolDecoder decoder = ...;
 * ByteBuffer in = ...;
 * 
 * decoder.decode(session, in, session.getProtocolDecoderOutput());
 * 
 * Object message = session.getProtocolDecoderOutputQueue().poll();
 * </pre>
 * 
 * <h2>Encoding</h2>
 * <pre>
 * ProtocolCodecSession session = new ProtocolCodecSession();
 * ProtocolEncoder encoder = ...;
 * MessageX in = ...;
 * 
 * encoder.encode(session, in, session.getProtocolEncoderOutput());
 * 
 * ByteBuffer buffer = session.getProtocolDecoderOutputQueue().poll();
 * </pre>
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class ProtocolCodecSession extends AbstractIoSession {
    
    private static final TransportMetadata METADATA =
        new DefaultTransportMetadata(
                "codec", false, false,
                SocketAddress.class, IoSessionConfig.class, Object.class);
    
    private static final IoSessionConfig SERVICE_CONFIG = new AbstractIoSessionConfig() {
        @Override
        protected void doSetAll(IoSessionConfig config) {
        }
    };
    
    private static final SocketAddress ANONYMOUS_ADDRESS = new SocketAddress() {
        private static final long serialVersionUID = -496112902353454179L;

        @Override
        public String toString() {
            return "?";
        }
    };
    
    private static final IoAcceptor SERVICE = new AbstractIoAcceptor(SERVICE_CONFIG) {
        @Override
        protected void doBind() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void doUnbind() {
            throw new UnsupportedOperationException();
        }

        public IoSession newSession(SocketAddress remoteAddress) {
            throw new UnsupportedOperationException();
        }

        public TransportMetadata getTransportMetadata() {
            return METADATA;
        }
    };
    
    static {
        // Set meaningless default values.
        SERVICE.setHandler(new IoHandlerAdapter());
        SERVICE.setLocalAddress(ANONYMOUS_ADDRESS);
    }

    private final IoSessionConfig config = new AbstractIoSessionConfig() {
        @Override
        protected void doSetAll(IoSessionConfig config) {
        }
    };
    
    private final IoFilterChain filterChain = new AbstractIoFilterChain(this) {
        @Override
        protected void doClose(IoSession session) throws Exception {
        }

        @Override
        protected void doWrite(IoSession session, WriteRequest writeRequest)
                throws Exception {
        }
    };
    
    private final WriteFuture notWrittenFuture =
        DefaultWriteFuture.newNotWrittenFuture(this);
    
    private final AbstractProtocolEncoderOutput encoderOutput =
        new AbstractProtocolEncoderOutput() {
            public WriteFuture flush() {
                return notWrittenFuture;
            }
    };
    
    private final AbstractProtocolDecoderOutput decoderOutput =
        new AbstractProtocolDecoderOutput() {
            public void flush() {
            }
    };
    
    private volatile IoHandler handler = new IoHandlerAdapter();
    private volatile SocketAddress localAddress = ANONYMOUS_ADDRESS;
    private volatile SocketAddress remoteAddress = ANONYMOUS_ADDRESS;

    /**
     * Creates a new instance.
     */
    public ProtocolCodecSession() {
    }
    
    /**
     * Returns the {@link ProtocolEncoderOutput} that buffers
     * {@link ByteBuffer}s generated by {@link ProtocolEncoder}.
     */
    public ProtocolEncoderOutput getEncoderOutput() {
        return encoderOutput;
    }
    
    /**
     * Returns the {@link Queue} of the buffered encoder output.
     */
    public Queue<ByteBuffer> getEncoderOutputQueue() {
        return encoderOutput.getBufferQueue();
    }
    
    /**
     * Returns the {@link ProtocolEncoderOutput} that buffers
     * messages generated by {@link ProtocolDecoder}.
     */
    public ProtocolDecoderOutput getDecoderOutput() {
        return decoderOutput;
    }
    
    /**
     * Returns the {@link Queue} of the buffered decoder output.
     */
    public Queue<Object> getDecoderOutputQueue() {
        return decoderOutput.getMessageQueue();
    }
    
    @Override
    protected void updateTrafficMask() {
    }

    public IoSessionConfig getConfig() {
        return config;
    }

    public IoFilterChain getFilterChain() {
        return filterChain;
    }

    public IoHandler getHandler() {
        return handler;
    }
    
    /**
     * Sets the {@link IoHandler} which handles this session.
     */
    public void setHandler(IoHandler handler) {
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        
        this.handler = handler;
    }

    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }
    
    /**
     * Sets the socket address of local machine which is associated with
     * this session.
     */
    public void setLocalAddress(SocketAddress localAddress) {
        if (localAddress == null) {
            throw new NullPointerException("localAddress");
        }
        
        this.localAddress = localAddress;
    }
    
    /**
     * Sets the socket address of remote peer. 
     */
    public void setRemoteAddress(SocketAddress remoteAddress) {
        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }
        
        this.remoteAddress = remoteAddress;
    }

    public int getScheduledWriteBytes() {
        return 0;
    }

    public int getScheduledWriteMessages() {
        return 0;
    }

    public IoService getService() {
        return SERVICE;
    }

    public TransportMetadata getTransportMetadata() {
        return METADATA;
    }
}
