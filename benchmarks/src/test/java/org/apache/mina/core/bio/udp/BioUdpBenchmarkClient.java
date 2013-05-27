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
package org.apache.mina.core.bio.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.BenchmarkClient;

/**
 * A client that uses a BIO datagram to communicate with the server
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class BioUdpBenchmarkClient implements BenchmarkClient {
    // The UDP client
    private DatagramSocket sender;

    /**
     * {@inheritDoc}
     */
    public void start(int port, final CountDownLatch counter, final byte[] data) throws IOException {
        InetAddress serverAddress = InetAddress.getLocalHost();
        byte[] buffer = new byte[65507];
        sender = new DatagramSocket(port + 1);

        DatagramPacket pduSent = new DatagramPacket(data, data.length, serverAddress, port);
        DatagramPacket pduReceived = new DatagramPacket(buffer, data.length);
        sender.send(pduSent);

        boolean done = false;

        while (!done) {
            try {
                sender.receive(pduReceived);

                for (int i = 0; i < pduReceived.getLength(); ++i) {
                    counter.countDown();

                    if (counter.getCount() > 0) {
                        sender.send(pduSent);
                        break;
                    } else {
                        done = true;
                    }
                }
            } catch (IOException ioe) {
                // Nothing to do
            }
        }

        sender.close();
    }

    /**
     * {@inheritedDoc}
     */
    public void stop() throws IOException {
    }
}
