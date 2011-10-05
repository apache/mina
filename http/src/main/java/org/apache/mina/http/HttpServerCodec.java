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
package org.apache.mina.http;

import org.apache.mina.api.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerCodec extends ProtocolCodecFilter {

    private static final Logger LOG = LoggerFactory.getLogger(HttpServerCodec.class);

    /** Key for decoder current state */
    private static final String DECODER_STATE_ATT = "http.ds";

    /** Key for the partial HTTP requests head */
    private static final String PARTIAL_HEAD_ATT = "http.ph";

    private static ProtocolEncoder encoder = new HttpServerEncoder();
    private static ProtocolDecoder decoder = new HttpServerDecoder();

    public HttpServerCodec() {
        super(encoder, decoder);
    }
    
    @Override
    public void sessionCreated(IoSession session) {
        super.sessionCreated(session);
        session.setAttribute(DECODER_STATE_ATT, DecoderState.NEW);
    }

    @Override
    public void sessionClosed(IoSession session) {
        super.sessionClosed(session);
        session.removeAttribute(DECODER_STATE_ATT);
        session.removeAttribute(PARTIAL_HEAD_ATT);
    }
}