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
package org.apache.mina.filter.codec.netty;

import net.gleamynode.netty2.Message;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderException;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * A MINA <tt>ProtocolEncoder</tt> that encodes Netty2 {@link Message}s
 * into byte buffers. 
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$,
 */
public class NettyEncoder implements ProtocolEncoder {
    /**
     * Creates a new instance.
     */
    public NettyEncoder() {
    }

    public void encode(IoSession session, Object message,
            ProtocolEncoderOutput out) throws Exception {
        if (!(message instanceof Message)) {
            throw new ProtocolEncoderException(
                    "This encoder can decode only Netty Messages.");
        }

        for (;;) {
            ByteBuffer buf = ByteBuffer.allocate(8192);
            Message m = (Message) message;
            try {
                if (m.write(buf.buf())) {
                    break;
                }
            } finally {
                buf.flip();
                if (buf.hasRemaining()) {
                    out.write(buf);
                } else {
                    buf.release();
                }
            }
        }
    }

    public void dispose(IoSession session) throws Exception {
    }
}
