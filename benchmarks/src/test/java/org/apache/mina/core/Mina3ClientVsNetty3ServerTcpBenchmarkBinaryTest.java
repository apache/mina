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

import org.apache.mina.core.nio.tcp.Mina3TcpBenchmarkClient;
import org.apache.mina.core.nio.tcp.Netty3TcpBenchmarkServer;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Mina3ClientVsNetty3ServerTcpBenchmarkBinaryTest extends BenchmarkBinaryTest {

    /**
     * @param numberOfMessages
     * @param messageSize
     */
    public Mina3ClientVsNetty3ServerTcpBenchmarkBinaryTest(int numberOfMessages, int messageSize, int timeout) {
        super(numberOfMessages, messageSize, timeout);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BenchmarkClient getClient() {
        return new Mina3TcpBenchmarkClient();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BenchmarkServer getServer() {
        return new Netty3TcpBenchmarkServer();
    }

    @Parameters()
    public static Collection<Object[]> getParameters() {
        Object[][] parameters = new Object[][] { { 1000000, 10, 2 * 60 }, { 1000000, 1 * 1024, 2 * 60 },
                { 1000000, 10 * 1024, 2 * 60 }, { 1000000, 20 * 1024, 2 * 60 }, { 500000, 50 * 1024, 2 * 60 },
                { 200000, 100 * 1024, 2 * 60 }, { 100000, 200 * 1024, 2 * 60 }, { 50000, 500 * 1024, 2 * 60 },
                { 20000, 1024 * 1024, 2 * 60 }, { 2000, 10 * 1024 * 1024, 2 * 60 }, { 500, 64 * 1024 * 1024, 2 * 60 } };
        return Arrays.asList(parameters);
    }
}
