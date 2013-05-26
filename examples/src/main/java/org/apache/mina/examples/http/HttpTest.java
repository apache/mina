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
package org.apache.mina.examples.http;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.api.AbstractIoFilter;
import org.apache.mina.api.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.filterchain.ReadFilterChainController;
import org.apache.mina.http.DateUtil;
import org.apache.mina.http.HttpDecoderState;
import org.apache.mina.http.HttpServerDecoder;
import org.apache.mina.http.HttpServerEncoder;
import org.apache.mina.http.api.DefaultHttpResponse;
import org.apache.mina.http.api.HttpContentChunk;
import org.apache.mina.http.api.HttpEndOfContent;
import org.apache.mina.http.api.HttpMethod;
import org.apache.mina.http.api.HttpPdu;
import org.apache.mina.http.api.HttpRequest;
import org.apache.mina.http.api.HttpStatus;
import org.apache.mina.http.api.HttpVersion;
import org.apache.mina.transport.nio.NioTcpServer;

public class HttpTest {

    public static void main(String[] args) throws Exception {

        NioTcpServer httpServer = new NioTcpServer();
        httpServer.setReuseAddress(true);
        httpServer.setFilters(new LoggingFilter("INCOMING"),
                new ProtocolCodecFilter<HttpPdu, ByteBuffer, Void, HttpDecoderState>(new HttpServerEncoder(),
                        new HttpServerDecoder()), new LoggingFilter("DECODED"), new DummyHttpSever());

        httpServer.getSessionConfig().setTcpNoDelay(true);

        httpServer.bind(new InetSocketAddress(8080));

        // run for 20 seconds
        Thread.sleep(20000);
        httpServer.unbind();

    }

    private static class DummyHttpSever extends AbstractIoFilter {

        private HttpRequest incomingRequest;

        private List<ByteBuffer> body;

        @Override
        public void messageReceived(IoSession session, Object message, ReadFilterChainController controller) {
            if (message instanceof HttpRequest) {
                incomingRequest = (HttpRequest) message;
                body = new ArrayList<ByteBuffer>();

                // check if this request is going to be followed by and HTTP body or not
                if (incomingRequest.getMethod() != HttpMethod.POST && incomingRequest.getMethod() != HttpMethod.PUT) {
                    sendResponse(session, incomingRequest);
                } else {

                }
            } else if (message instanceof ByteBuffer) {
                body.add((ByteBuffer) message);
            } else if (message instanceof HttpEndOfContent) {
                // we received all the post content, send the crap back
                sendResponse(session, incomingRequest);
            }

        }

        public void sendResponse(IoSession session, HttpRequest request) {
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Server", "Apache MINA Dummy test server/0.0.");
            headers.put("Date", DateUtil.getCurrentAsString());
            headers.put("Connection", "Close");
            String strContent = "Hello ! we reply to request !";
            ByteBuffer content = ByteBuffer.wrap(strContent.getBytes());

            // compute content len
            headers.put("Content-Length", String.valueOf(content.remaining()));
            session.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SUCCESS_OK, headers));
            session.write(new HttpContentChunk(content));
            session.write(new HttpEndOfContent());
            session.close(false);

        }
    }
}
