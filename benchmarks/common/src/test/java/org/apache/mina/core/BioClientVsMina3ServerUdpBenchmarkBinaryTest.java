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

import org.apache.mina.core.bio.udp.BioUdpBenchmarkClient;
import org.apache.mina.core.nio.udp.Mina3UdpBenchmarkServer;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class BioClientVsMina3ServerUdpBenchmarkBinaryTest extends BenchmarkBinaryTest {

    /**
     * @param numberOfMessages
     * @param messageSize
     */
    public BioClientVsMina3ServerUdpBenchmarkBinaryTest(int numberOfMessages, int messageSize, int timeout) {
        super(numberOfMessages, messageSize, timeout);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BenchmarkClient getClient() {
        return new BioUdpBenchmarkClient();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BenchmarkServer getServer() {
        return new Mina3UdpBenchmarkServer();
    }

    @Parameters(name = "{0} messages of size {1}")
    public static Collection<Object[]> getParameters() {
        // Note : depending on your OS, the maximum PDU you can send can vary. See sysctl net.inet.udp.maxdgram
        Object[][] parameters = new Object[][] { 
                { 1000000, 10, 2 * 60 }, 
                { 1000000, 1 * 1024, 2 * 60 },
                { 1000000, 2 * 1024, 2 * 60 }, 
                { 1000000, 4 * 1024, 2 * 60 }, 
                { 500000, 8 * 1024, 2 * 60 } }; // No need to test any further, the maximum size for an UDP message is 64Kb
        return Arrays.asList(parameters);
    }
}
