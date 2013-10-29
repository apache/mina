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
package org.apache.mina.examples.coap;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.apache.mina.api.AbstractIoFutureListener;
import org.apache.mina.api.AbstractIoHandler;
import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoSession;
import org.apache.mina.coap.CoapCode;
import org.apache.mina.coap.CoapMessage;
import org.apache.mina.coap.codec.CoapDecoder;
import org.apache.mina.coap.codec.CoapEncoder;
import org.apache.mina.coap.resource.AbstractResourceHandler;
import org.apache.mina.coap.resource.CoapResponse;
import org.apache.mina.coap.resource.ResourceRegistry;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.query.RequestFilter;
import org.apache.mina.transport.nio.NioUdpClient;

/**
 * 
 * A CoAP client, accepting "piggy-backed" request and behaving like a server too.
 */
public class CoapClient {

    static final ResourceRegistry reg = new ResourceRegistry();

    public static void main(String[] args) {

        final String status = "status message";
        System.err.println(status.length());

        final RequestFilter<CoapMessage, CoapMessage> rq = new RequestFilter<>();

        NioUdpClient client = new NioUdpClient();
        client.setFilters( //
                new ProtocolCodecFilter<CoapMessage, ByteBuffer, Void, Void>( //
                        new CoapEncoder(), new CoapDecoder() //
                ), rq);

        client.setIoHandler(new Handler());

        reg.register(new AbstractResourceHandler() {

            @Override
            public CoapResponse handle(CoapMessage request, IoSession session) {
                if (request.getCode() == CoapCode.GET.getCode()) {
                    return new CoapResponse(CoapCode.CONTENT.getCode(), status.getBytes());
                } else {
                    return new CoapResponse(CoapCode.METHOD_NOT_ALLOWED.getCode(), null);
                }
            }

            @Override
            public String getTitle() {
                return "Status report";
            }

            @Override
            public String getPath() {
                return "st";
            }

        });

        IoFuture<IoSession> cf = client.connect(new InetSocketAddress(args[0], Integer.parseInt(args[1])));

        // register on connection
        cf.register(new AbstractIoFutureListener<IoSession>() {

            @Override
            public void completed(IoSession session) {
                IoFuture<CoapMessage> response = rq.request(session,
                        CoapMessage.post("register?id=1234567890ABCDEF", true, "true".getBytes()), 10000);
                response.register(new AbstractIoFutureListener<CoapMessage>() {
                    @Override
                    public void completed(CoapMessage result) {
                        if (result.getCode() == CoapCode.CREATED.getCode()) {
                            System.err.println("registered !" + result);
                        } else {
                            System.err.println("registration error : " + result.getCode());
                        }
                    }
                });
            }
        });

        try {
            Thread.sleep(50000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static class Handler extends AbstractIoHandler {

        @Override
        public void messageReceived(IoSession session, Object message) {

            System.err.println("rcvd : " + message);
            CoapMessage msg = (CoapMessage) message;
            if (msg.getCode() == CoapCode.GET.getCode() || msg.getCode() == CoapCode.POST.getCode()
                    || msg.getCode() == CoapCode.PUT.getCode() || msg.getCode() == CoapCode.DELETE.getCode()) {
                CoapMessage resp = reg.respond(msg, session);
                session.write(resp);
            }
        }

        @Override
        public void messageSent(IoSession session, Object message) {
            System.err.println("sent : " + message);
        }

        @Override
        public void exceptionCaught(IoSession session, Exception cause) {
            System.err.println("exception !");
            cause.printStackTrace();

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            session.close(true);
        }
    }
}
