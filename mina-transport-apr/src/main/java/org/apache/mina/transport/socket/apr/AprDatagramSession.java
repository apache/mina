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
package org.apache.mina.transport.socket.apr;

import java.net.InetSocketAddress;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.AbstractDatagramSessionConfig;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.tomcat.jni.Socket;

/**
 * An {@link IoSession} for APR UDP datagram based session.
 * It's implementing the usual common features for {@link DatagramSessionConfig}. 
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
class AprDatagramSession extends AprSession {

    static final TransportMetadata METADATA =
        new DefaultTransportMetadata(
                "apr", "datagram", true, false,
                InetSocketAddress.class,
                DatagramSessionConfig.class, IoBuffer.class);

    /**
     * Create an instance of {@link AprDatagramSession}. 
     * 
     * {@inheritDoc} 
     */    
    AprDatagramSession(
            IoService service, IoProcessor<AprSession> processor,
            long descriptor, InetSocketAddress remoteAddress) throws Exception {
        super(service, processor, descriptor, remoteAddress);
        config = new SessionConfigImpl();
        this.config.setAll(service.getSessionConfig());
    }

    /**
     * {@inheritDoc}
     */
    public DatagramSessionConfig getConfig() {
        return ( DatagramSessionConfig ) config;
    }

    /**
     * {@inheritDoc}
     */
    public TransportMetadata getTransportMetadata() {
        return METADATA;
    }

    /**
     * The implementation for the {@link org.apache.mina.core.session.IoSessionConfig} related to APR UDP socket.
     * @author <a href="http://mina.apache.org">Apache MINA Project</a>
     */
    private class SessionConfigImpl extends AbstractDatagramSessionConfig {
        /**
         * {@inheritDoc}
         */
        public boolean isReuseAddress() {
            try {
                return Socket.optGet(getDescriptor(), Socket.APR_SO_REUSEADDR) == 1;
            } catch (Exception e) {
                throw new RuntimeIoException("Failed to get SO_REUSEADDR.", e);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void setReuseAddress(boolean on) {
            Socket.optSet(getDescriptor(), Socket.APR_SO_REUSEADDR, on ? 1 : 0);
        }

        /**
         * {@inheritDoc}
         */
        public int getTrafficClass() {
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        public void setTrafficClass(int tc) {
        }

        /**
         * {@inheritDoc}
         */
        public int getSendBufferSize() {
            try {
                return Socket.optGet(getDescriptor(), Socket.APR_SO_SNDBUF);
            } catch (Exception e) {
                throw new RuntimeException("APR Exception", e);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void setSendBufferSize(int size) {
            Socket.optSet(getDescriptor(), Socket.APR_SO_SNDBUF, size);
        }

        /**
         * {@inheritDoc}
         */
        public int getReceiveBufferSize() {
            try {
                return Socket.optGet(getDescriptor(), Socket.APR_SO_RCVBUF);
            } catch (Exception e) {
                throw new RuntimeException("APR Exception", e);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void setReceiveBufferSize(int size) {
            Socket.optSet(getDescriptor(), Socket.APR_SO_RCVBUF, size);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isBroadcast() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public void setBroadcast(boolean broadcast) {
        }
    }
}
