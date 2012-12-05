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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.BenchmarkFactory.Type;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
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

    public abstract Type getClientType();

    public abstract Type getServerType();

    @Parameters
    public static Collection<Object[]> getParameters() {
        Object[][] parameters = new Object[][] { 
                { 1000000, 10, 2 * 60 }, 
                { 1000000, 1 * 1024, 2 * 60 }, 
                { 1000000, 10 * 1024, 2 * 60 },
                { 1000, 64 * 1024 * 1024, 10 * 60 }
        };
        return Arrays.asList(parameters);
    }

    @Before
    public void init() throws IOException {
        port = AvailablePortFinder.getNextAvailable();
        server = BenchmarkServerFactory.INSTANCE.get(getServerType());
        server.start(port);
        client = BenchmarkClientFactory.INSTANCE.get(getClientType());
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

        client.start(port, counter, data);
        counter.await(timeout, TimeUnit.SECONDS);
    }
}
