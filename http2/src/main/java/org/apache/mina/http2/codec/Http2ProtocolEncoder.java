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
package org.apache.mina.http2.codec;

import java.nio.ByteBuffer;

import org.apache.mina.codec.StatelessProtocolEncoder;
import org.apache.mina.http2.api.Http2Frame;

/**
 *      
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Http2ProtocolEncoder implements StatelessProtocolEncoder<Http2Frame, ByteBuffer> {

    @Override
    public Void createEncoderState() {
        return null;
    }

    @Override
    public ByteBuffer encode(Http2Frame message, Void context) {
        return message.toBuffer();
    }
}
