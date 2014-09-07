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
package org.apache.mina.transport.socket.nio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.regex.Pattern;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.Test;

/**
 * Tests a generic {@link IoConnector}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DIRMINA777Test {

    @Test
    public void checkReadFuture() throws Throwable {
        int port = AvailablePortFinder.getNextAvailable(1025);
        NioSocketAcceptor acceptor = new NioSocketAcceptor();
        acceptor.setReuseAddress(true);
        acceptor.setHandler(new IoHandlerAdapter() {

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                IoBuffer buffer = IoBuffer.allocate(1);
                buffer.put((byte) 125);
                buffer.rewind();
                session.write(buffer);
            }
            
        });
        acceptor.bind(new InetSocketAddress(port));

        try {
            IoConnector connector = new NioSocketConnector();
            connector.setHandler(new IoHandlerAdapter());
            ConnectFuture connectFuture = connector.connect(new InetSocketAddress("localhost", port));
            connectFuture.awaitUninterruptibly();
            if (connectFuture.getException() != null) {
                throw connectFuture.getException();
            }
            connectFuture.getSession().getConfig().setUseReadOperation(true);
            ReadFuture readFuture = connectFuture.getSession().read();
            readFuture.awaitUninterruptibly();
            if (readFuture.getException() != null) {
                throw readFuture.getException();
            }
            IoBuffer message = (IoBuffer)readFuture.getMessage();
            assertEquals(1, message.remaining());
            assertEquals(125,message.get());
            connectFuture.getSession().close(true);
        } finally {
            acceptor.dispose();
        }
    }

}
