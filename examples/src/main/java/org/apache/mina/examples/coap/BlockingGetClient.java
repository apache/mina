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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Random;

import org.apache.mina.coap.CoapCode;
import org.apache.mina.coap.CoapMessage;
import org.apache.mina.coap.CoapOption;
import org.apache.mina.coap.CoapOptionType;
import org.apache.mina.coap.MessageType;
import org.apache.mina.coap.codec.CoapDecoder;
import org.apache.mina.coap.codec.CoapEncoder;

public class BlockingGetClient {

    public static void main(String[] args) {
        try {
            DatagramChannel channel = DatagramChannel.open();

            InetSocketAddress target = new InetSocketAddress("127.0.0.1", 5683);

            channel.connect(target);

            System.err.println(channel);

            CoapEncoder encoder = new CoapEncoder();
            CoapDecoder decoder = new CoapDecoder();
            ByteBuffer buff = ByteBuffer.allocateDirect(2048);

            Random r = new Random();
            byte[] url = "nlp".getBytes();
            CoapMessage msg = new CoapMessage(1, MessageType.CONFIRMABLE, CoapCode.GET.getCode(), 1234, null,
                    new CoapOption[] { new CoapOption(CoapOptionType.URI_PATH, url) }, null);

            for (int j = 0; j < 8; j++) {
                long start = System.currentTimeMillis();
                final int count = 100000;
                for (int i = 0; i < count; i++) {
                    buff.position(0);
                    buff.limit(buff.capacity());
                    int id = r.nextInt(1024);
                    msg.setId(id);
                    int bytes = channel.send(encoder.encode(msg, null), target);

                    if (bytes < 1) {
                        System.err.println("write fail :/ " + bytes);
                    } else {
                        buff.position(0);
                        buff.limit(buff.capacity());
                        SocketAddress addy = channel.receive(buff);
                        buff.flip();
                        CoapMessage response = decoder.decode(buff, null);
                        if (response.getId() != id) {
                            System.err.println("gni?");
                        }
                        // System.err.println("response : " + response);
                    }
                }
                System.err.println("time : " + (System.currentTimeMillis() - start) + "ms");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
