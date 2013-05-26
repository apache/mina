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
package org.apache.mina.transport.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ExecutionException;

import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoSession;
import org.apache.mina.transport.nio.NioTcpClient;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test if the {@link NioTcpClient} handle correctly session connection timeout.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NioTcpClientTimeoutTest {

    @Test
    @Ignore
    public void timeout() throws IOException, InterruptedException {
        NioTcpClient client = new NioTcpClient();
        client.setConnectTimeoutMillis(1000);

        ServerSocket server = new ServerSocket();
        try {
            server.bind(null);
            IoFuture<IoSession> cf = client.connect(new InetSocketAddress("localhost", server.getLocalPort()));
            Thread.sleep(5000);
            IoSession session = cf.get();
            System.err.println(session);
            Assert.fail();
        } catch (ExecutionException ex) {
            // happy
            ex.printStackTrace();
        } finally {
            server.close();
        }
    }
}
