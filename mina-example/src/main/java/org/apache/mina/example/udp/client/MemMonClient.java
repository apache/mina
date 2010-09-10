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
package org.apache.mina.example.udp.client;

import java.net.InetSocketAddress;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.example.udp.MemoryMonitor;
import org.apache.mina.transport.socket.nio.NioDatagramConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends its memory usage to the MemoryMonitor server.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class MemMonClient extends IoHandlerAdapter {

    private final static Logger LOGGER = LoggerFactory.getLogger(MemMonClient.class);

    private IoSession session;

    private IoConnector connector;

    /**
     * Default constructor.
     */
    public MemMonClient() {

        LOGGER.debug("UDPClient::UDPClient");
        LOGGER.debug("Created a datagram connector");
        connector = new NioDatagramConnector();

        LOGGER.debug("Setting the handler");
        connector.setHandler(this);

        LOGGER.debug("About to connect to the server...");
        ConnectFuture connFuture = connector.connect(new InetSocketAddress(
                "localhost", MemoryMonitor.PORT));

        LOGGER.debug("About to wait.");
        connFuture.awaitUninterruptibly();

        LOGGER.debug("Adding a future listener.");
        connFuture.addListener(new IoFutureListener<ConnectFuture>() {
            public void operationComplete(ConnectFuture future) {
                if (future.isConnected()) {
                    LOGGER.debug("...connected");
                    session = future.getSession();
                    try {
                        sendData();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    LOGGER.error("Not connected...exiting");
                }
            }
        });
    }

    private void sendData() throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            long free = Runtime.getRuntime().freeMemory();
            IoBuffer buffer = IoBuffer.allocate(8);
            buffer.putLong(free);
            buffer.flip();
            session.write(buffer);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new InterruptedException(e.getMessage());
            }
        }
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause)
            throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void messageReceived(IoSession session, Object message)
            throws Exception {
        LOGGER.debug("Session recv...");
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        LOGGER.debug("Message sent...");
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        LOGGER.debug("Session closed...");
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        LOGGER.debug("Session created...");
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status)
            throws Exception {
        LOGGER.debug("Session idle...");
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        LOGGER.debug("Session opened...");
    }

    public static void main(String[] args) {
        new MemMonClient();
    }
}
