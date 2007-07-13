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
package org.apache.mina.example.echoserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import org.apache.commons.net.EchoTCPClient;
import org.apache.commons.net.EchoUDPClient;
import org.apache.mina.example.echoserver.ssl.SSLServerSocketFactory;
import org.apache.mina.example.echoserver.ssl.SSLSocketFactory;

/**
 * Tests echo server example.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev:448075 $, $Date:2006-09-20 05:26:53Z $
 */
public class AcceptorTest extends AbstractTest {
    public AcceptorTest() {
    }

    public void testTCP() throws Exception {
        EchoTCPClient client = new EchoTCPClient();
        testTCP0(client);
    }

    public void testTCPWithSSL() throws Exception {
        // Add an SSL filter
        useSSL = true;

        // Create a commons-net socket factory
        SSLSocketFactory.setSslEnabled(true);
        SSLServerSocketFactory.setSslEnabled(true);
        org.apache.commons.net.SocketFactory factory = new org.apache.commons.net.SocketFactory() {

            private SocketFactory f = SSLSocketFactory.getSocketFactory();

            private ServerSocketFactory ssf = SSLServerSocketFactory
                    .getServerSocketFactory();

            public Socket createSocket(String arg0, int arg1)
                    throws UnknownHostException, IOException {
                return f.createSocket(arg0, arg1);
            }

            public Socket createSocket(InetAddress arg0, int arg1)
                    throws IOException {
                return f.createSocket(arg0, arg1);
            }

            public Socket createSocket(String arg0, int arg1, InetAddress arg2,
                    int arg3) throws UnknownHostException, IOException {
                return f.createSocket(arg0, arg1, arg2, arg3);
            }

            public Socket createSocket(InetAddress arg0, int arg1,
                    InetAddress arg2, int arg3) throws IOException {
                return f.createSocket(arg0, arg1, arg2, arg3);
            }

            public ServerSocket createServerSocket(int arg0) throws IOException {
                return ssf.createServerSocket(arg0);
            }

            public ServerSocket createServerSocket(int arg0, int arg1)
                    throws IOException {
                return ssf.createServerSocket(arg0, arg1);
            }

            public ServerSocket createServerSocket(int arg0, int arg1,
                    InetAddress arg2) throws IOException {
                return ssf.createServerSocket(arg0, arg1, arg2);
            }

        };

        // Create a echo client with SSL factory and test it.
        EchoTCPClient client = new EchoTCPClient();
        client.setSocketFactory(factory);
        testTCP0(client);
    }

    private void testTCP0(EchoTCPClient client) throws Exception {
        client.connect("localhost", port);
        byte[] writeBuf = new byte[16];

        for (int i = 0; i < 10; i++) {
            fillWriteBuffer(writeBuf, i);
            client.getOutputStream().write(writeBuf);
        }

        client.setSoTimeout(30000);

        byte[] readBuf = new byte[writeBuf.length];

        for (int i = 0; i < 10; i++) {
            fillWriteBuffer(writeBuf, i);

            int readBytes = 0;
            while (readBytes < readBuf.length) {
                int nBytes = client.getInputStream().read(readBuf, readBytes,
                        readBuf.length - readBytes);

                if (nBytes < 0)
                    fail("Unexpected disconnection.");

                readBytes += nBytes;
            }

            assertEquals(writeBuf, readBuf);
        }

        client.setSoTimeout(500);

        try {
            client.getInputStream().read();
            fail("Unexpected incoming data.");
        } catch (SocketTimeoutException e) {
        }

        client.disconnect();
    }

    public void testUDP() throws Exception {
        EchoUDPClient client = new EchoUDPClient();
        client.open();
        client.setSoTimeout(3000);

        byte[] writeBuf = new byte[16];
        byte[] readBuf = new byte[writeBuf.length];

        client.setSoTimeout(500);

        for (int i = 0; i < 10; i++) {
            fillWriteBuffer(writeBuf, i);
            client.send(writeBuf, writeBuf.length, InetAddress.getByName(null),
                    port);

            assertEquals(readBuf.length, client
                    .receive(readBuf, readBuf.length));
            assertEquals(writeBuf, readBuf);
        }

        try {
            client.receive(readBuf);
            fail("Unexpected incoming data.");
        } catch (SocketTimeoutException e) {
        }

        client.close();
    }

    private void fillWriteBuffer(byte[] writeBuf, int i) {
        for (int j = writeBuf.length - 1; j >= 0; j--) {
            writeBuf[j] = (byte) (j + i);
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AcceptorTest.class);
    }
}
