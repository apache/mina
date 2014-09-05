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

import java.util.Arrays;
import java.util.Collection;

import org.apache.mina.core.nio.tcp.Mina3TcpBenchmarkServer;
import org.apache.mina.core.nio.tcp.Netty4TcpBenchmarkClient;
import org.junit.runners.Parameterized.Parameters;

/**
 * A TCP  Netty4 client vs Mina 3 server benchmark
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Netty4ClientVsMina3ServerTcpBenchmarkBinaryTest extends BenchmarkBinaryTest {

    /**
     * @param numberOfMessages
     * @param messageSize
     */
    public Netty4ClientVsMina3ServerTcpBenchmarkBinaryTest(int numberOfMessages, int messageSize, int timeout) {
        super(numberOfMessages, messageSize, timeout);
    }

    /** {@inheritDoc}
     */
    @Override
    public BenchmarkClient getClient() {
        return new Netty4TcpBenchmarkClient();
    }

    /** {@inheritDoc}
     */
    @Override
    public BenchmarkServer getServer() {
        return new Mina3TcpBenchmarkServer();
    }

    //TODO: analyze with Netty is so slow on large message: last test lower to 100 messages
    @Parameters(name = "{0} messages of size {1}")
    public static Collection<Object[]> getParameters() {
        Object[][] parameters = new Object[][] { 
                { 1000000, 10, 2 * 60 }, 
                { 1000000, 1 * 1024, 2 * 60 },
                { 1000000, 10 * 1024, 2 * 60 }, 
                { 1000000, 20 * 1024, 2 * 60 }, 
                { 500000, 50 * 1024, 2 * 60 },
                { 200000, 100 * 1024, 2 * 60 }, 
                { 100000, 200 * 1024, 2 * 60 }, 
                { 50000, 500 * 1024, 2 * 60 },
                { 20000, 1024 * 1024, 2 * 60 }, 
                { 2000, 10 * 1024 * 1024, 2 * 60 }, 
                { 500, 64 * 1024 * 1024, 2 * 60 } };
        return Arrays.asList(parameters);
    }
}
