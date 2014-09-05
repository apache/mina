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
package org.apache.mina.core;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Binary benchmark
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
@RunWith(Parameterized.class)
public abstract class BenchmarkBinaryTest {
    private int numberOfMessages;

    private int port;

    private BenchmarkServer server;

    private BenchmarkClient client;

    private int messageSize;

    private int timeout;

    private byte[] data;

    public BenchmarkBinaryTest(int numberOfMessages, int messageSize, int timeout) {
        this.numberOfMessages = numberOfMessages;
        this.messageSize = messageSize;
        this.timeout = timeout;
    }

    public abstract BenchmarkClient getClient();

    public abstract BenchmarkServer getServer();

    @Parameters(name = "{0} messages of size {1}")
    public static Collection<Object[]> getParameters() {
        Object[][] parameters = new Object[][] { { 100000, 10, 2 * 60 }, { 100000, 1 * 1024, 2 * 60 },
                { 100000, 10 * 1024, 2 * 60 }, { 100, 64 * 1024 * 1024, 10 * 60 } };
        return Arrays.asList(parameters);
    }

    public static int getNextAvailable() {
        ServerSocket serverSocket = null;

        try {
            // Here, we simply return an available port found by the system
            serverSocket = new ServerSocket(0);
            int port = serverSocket.getLocalPort();

            // Don't forget to close the socket...
            serverSocket.close();

            return port;
        } catch (IOException ioe) {
            throw new NoSuchElementException(ioe.getMessage());
        }
    }

    @Before
    public void init() throws IOException {
        port = getNextAvailable();
        server = getServer();
        server.start(port);
        client = getClient();
        data = new byte[messageSize + 4];
        data[0] = (byte) (messageSize >>> 24 & 255);
        data[1] = (byte) (messageSize >>> 16 & 255);
        data[2] = (byte) (messageSize >>> 8 & 255);
        data[3] = (byte) (messageSize & 255);
    }

    @After
    public void shutdown() throws IOException {
        client.stop();
        server.stop();
    }

    /**
     * Send "numberOfMessages" messages to a server. Currently, 1 million, with two different
     * size, 10Ko and 64Ko.
     */
    @Test
    public void benchmark() throws IOException, InterruptedException {
        CountDownLatch counter = new CountDownLatch(numberOfMessages);
        CounterFilter.messageSent.set(0);

        boolean result = true;

        System.out.println("-------------- Sending " + data.length + " bytes");
        client.start(port, counter, data);
        long globalSent = 0;
        long warmedUpSent = 0;
        int nbSeconds = 0;

        while ((counter.getCount() > 0) && (nbSeconds < 120)) {
            result = counter.await(1, TimeUnit.SECONDS);

            long nbSent = CounterFilter.messageSent.getAndSet(0);
            nbSeconds++;

            globalSent += nbSent;

            if (nbSeconds > 5) {
                warmedUpSent += nbSent;
            }

            System.out.print("Nb messages sent per second : " + nbSent + "\r");
        }

        System.out.println();
        if (nbSeconds < 120) {
            System.out.println("Average : " + (warmedUpSent / (nbSeconds - 5)) + ", for " + globalSent
                    + " messages sent in " + nbSeconds + "s");
        } else {
            System.out.println("Wasn't able to send all the messages : sent " + globalSent);
        }

        assertTrue("Still " + counter.getCount() + " messages to send of a total of " + numberOfMessages, result);
    }
}
