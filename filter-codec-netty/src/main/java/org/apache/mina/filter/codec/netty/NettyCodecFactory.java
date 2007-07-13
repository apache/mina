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
import net.gleamynode.netty2.MessageRecognizer;

import org.apache.mina.filter.codec.ProtocolCodecFactory;

/**
 * A MINA <tt>ProtocolCodecFactory</tt> that provides encoder and decoder
 * for Netty2 {@link Message}s and {@link MessageRecognizer}s.
 * <p>
 * Please note that this codec factory assumes one {@link MessageRecognizer}
 * can be used for multiple sessions.  If not, you'll have to create your
 * own factory after this factory.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$,
 */
public class NettyCodecFactory implements ProtocolCodecFactory {
    private static final NettyEncoder ENCODER = new NettyEncoder();

    private final MessageRecognizer recognizer;

    public NettyCodecFactory(MessageRecognizer recognizer) {
        this.recognizer = recognizer;
    }

    public org.apache.mina.filter.codec.ProtocolEncoder getEncoder() {
        return ENCODER;
    }

    public org.apache.mina.filter.codec.ProtocolDecoder getDecoder() {
        return new NettyDecoder(recognizer);
    }
}
