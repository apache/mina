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

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

/**
 * A {@link ProtocolDecoder} implementation which decorates an existing decoder
 * to be thread-safe.  Please be careful if you're going to use this decorator
 * because it can be a root of performance degradation in a multi-thread
 * environment.  Also, by default, appropriate synchronization is done
 * on a per-session basis by {@link ProtocolCodecFilter}.  Please use this
 * decorator only when you need to synchronize on a per-decoder basis, which
 * is not common.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SynchronizedProtocolDecoder implements ProtocolDecoder {
    private final ProtocolDecoder decoder;

    /**
     * Creates a new instance which decorates the specified <tt>decoder</tt>.
     * 
     * @param decoder The decorated decoder
     */
    public SynchronizedProtocolDecoder(ProtocolDecoder decoder) {
        if (decoder == null) {
            throw new IllegalArgumentException("decoder");
        }
        
        this.decoder = decoder;
    }

    /**
     * @return the decoder this decoder is decorating.
     */
    public ProtocolDecoder getDecoder() {
        return decoder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        synchronized (decoder) {
            decoder.decode(session, in, out);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finishDecode(IoSession session, ProtocolDecoderOutput out) throws Exception {
        synchronized (decoder) {
            decoder.finishDecode(session, out);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose(IoSession session) throws Exception {
        synchronized (decoder) {
            decoder.dispose(session);
        }
    }
}
