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
package org.apache.mina.example.netcat;

import java.net.InetSocketAddress;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

/**
 * (<b>Entry point</b>) NetCat client.  NetCat client connects to the specified
 * endpoint and prints out received data.  NetCat client disconnects
 * automatically when no data is read for 10 seconds.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println(Main.class.getName() + " <hostname> <port>");
            return;
        }

        // Create TCP/IP connector.
        NioSocketConnector connector = new NioSocketConnector();

        // Set connect timeout.
        connector.setConnectTimeoutMillis(30*1000L);

        // Start communication.
        connector.setHandler(new NetCatProtocolHandler());
        ConnectFuture cf = connector.connect(
                new InetSocketAddress(args[0], Integer.parseInt(args[1])));

        // Wait for the connection attempt to be finished.
        cf.awaitUninterruptibly();
        cf.getSession().getCloseFuture().awaitUninterruptibly();
        
        connector.dispose();
    }
}
