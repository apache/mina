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
package org.apache.mina.transport;

import java.net.InetSocketAddress;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.util.AvailablePortFinder;

/**
 * Tests a generic {@link IoConnector}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$ 
 */
public abstract class AbstractConnectorTest extends TestCase {

    protected abstract IoAcceptor createAcceptor();

    protected abstract IoConnector createConnector();

    public void testConnectFutureSuccessTiming() throws Exception {
        int port = AvailablePortFinder.getNextAvailable(1025);
        IoAcceptor acceptor = createAcceptor();
        acceptor.bind(new InetSocketAddress(port), new IoHandlerAdapter());

        try {
            final StringBuffer buf = new StringBuffer();
            IoConnector connector = createConnector();
            ConnectFuture future = connector.connect(new InetSocketAddress(
                    "localhost", port), new IoHandlerAdapter() {
                public void sessionCreated(IoSession session) {
                    buf.append("1");
                }

                public void sessionOpened(IoSession session) {
                    buf.append("2");
                }

                public void exceptionCaught(IoSession session, Throwable cause) {
                    buf.append("X");
                }
            });

            future.join();
            buf.append("3");
            future.getSession().close();
            Assert.assertEquals("123", buf.toString());
        } finally {
            acceptor.unbind(new InetSocketAddress(port));
        }
    }

    public void testConnectFutureFailureTiming() throws Exception {
        int port = AvailablePortFinder.getNextAvailable(1025);
        final StringBuffer buf = new StringBuffer();

        IoConnector connector = createConnector();
        ConnectFuture future = connector.connect(new InetSocketAddress(
                "localhost", port), new IoHandlerAdapter() {
            public void sessionCreated(IoSession session) {
                buf.append("X");
            }

            public void sessionOpened(IoSession session) {
                buf.append("Y");
            }

            public void exceptionCaught(IoSession session, Throwable cause) {
                buf.append("Z");
            }
        });

        future.join();
        buf.append("1");
        try {
            future.getSession().close();
            fail();
        } catch (RuntimeIOException e) {
            // OK.
        }
        Assert.assertEquals("1", buf.toString());
    }
}
