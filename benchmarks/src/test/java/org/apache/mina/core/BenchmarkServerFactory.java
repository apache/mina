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

import org.apache.mina.core.bio.udp.BioUdpBenchmarkServer;
import org.apache.mina.core.nio.tcp.Mina3TcpBenchmarkServer;
import org.apache.mina.core.nio.tcp.Netty3TcpBenchmarkServer;
import org.apache.mina.core.nio.udp.Mina3UdpBenchmarkServer;
import org.apache.mina.core.nio.udp.Netty3UdpBenchmarkServer;

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class BenchmarkServerFactory implements BenchmarkFactory<BenchmarkServer> {

    public static final BenchmarkServerFactory INSTANCE = new BenchmarkServerFactory();

    /**
     * {@inheritedDoc}
     */
    public BenchmarkServer get(org.apache.mina.core.BenchmarkFactory.Type type) {
        switch (type) {
        case Mina3_tcp:
            return new Mina3TcpBenchmarkServer();
        case Mina3_udp:
            return new Mina3UdpBenchmarkServer();
        case Netty3_tcp:
            return new Netty3TcpBenchmarkServer();
        case Netty3_udp:
            return new Netty3UdpBenchmarkServer();
        case Bio_udp:
            return new BioUdpBenchmarkServer();
        case Bio_tcp:
            //return new BioUdpBenchmarkClient();
        default:
            throw new IllegalArgumentException("Invalid type " + type);
        }
    }
}
