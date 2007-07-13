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

import org.apache.mina.common.IoSession;

/**
 * A {@link ProtocolEncoder} implementation which decorates an existing encoder
 * to be thread-safe.  Please be careful if you're going to use this decorator
 * because it can be a root of performance degradation in a multi-thread
 * environment.  Please use this decorator only when you need to synchronize
 * on a per-encoder basis instead of on a per-session basis, which is not
 * common.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SynchronizedProtocolEncoder implements ProtocolEncoder {
    private final ProtocolEncoder encoder;

    /**
     * Creates a new instance which decorates the specified <tt>encoder</tt>.
     */
    public SynchronizedProtocolEncoder(ProtocolEncoder encoder) {
        if (encoder == null) {
            throw new NullPointerException("encoder");
        }
        this.encoder = encoder;
    }

    /**
     * Returns the encoder this encoder is decorating.
     */
    public ProtocolEncoder getEncoder() {
        return encoder;
    }

    public void encode(IoSession session, Object message,
            ProtocolEncoderOutput out) throws Exception {
        synchronized (encoder) {
            encoder.encode(session, message, out);
        }
    }

    public void dispose(IoSession session) throws Exception {
        synchronized (encoder) {
            encoder.dispose(session);
        }
    }
}
