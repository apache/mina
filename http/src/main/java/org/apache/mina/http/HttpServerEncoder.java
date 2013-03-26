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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.mina.codec.StatelessProtocolEncoder;
import org.apache.mina.http.api.HttpContentChunk;
import org.apache.mina.http.api.HttpEndOfContent;
import org.apache.mina.http.api.HttpPdu;
import org.apache.mina.http.api.HttpPduEncodingVisitor;
import org.apache.mina.http.api.HttpRequest;
import org.apache.mina.http.api.HttpResponse;

/**
 * In charge of encoding HTTP message into bytes.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class HttpServerEncoder implements StatelessProtocolEncoder<HttpPdu, ByteBuffer> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Void createEncoderState() {
        // steless, so it's not used
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer encode(HttpPdu message, Void context) {
        return message.encode(visitor);
    }

    private HttpPduEncodingVisitor visitor = new HttpPduEncodingVisitor() {

        /**
         * {@inheritDoc}
         */
        @Override
        public ByteBuffer visit(HttpResponse msg) {
            StringBuilder sb = new StringBuilder(msg.getStatus().line());

            for (Map.Entry<String, String> header : msg.getHeaders().entrySet()) {
                sb.append(header.getKey());
                sb.append(": ");
                sb.append(header.getValue());
                sb.append("\r\n");
            }
            sb.append("\r\n");
            byte[] bytes = sb.toString().getBytes(Charset.forName("UTF-8"));
            return ByteBuffer.wrap(bytes);
        }

        @Override
        public ByteBuffer visit(HttpContentChunk msg) {
            return msg.getContent();
        }

        @Override
        public ByteBuffer visit(HttpEndOfContent msg) {
            return null;
        }

        @Override
        public ByteBuffer visit(HttpRequest msg) {
            throw new IllegalStateException("cannot encode that on server side");
        }
    };
}
