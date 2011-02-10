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

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.FileRegion;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.polling.AbstractPollingIoProcessor;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.SessionState;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests non regression on issue DIRMINA-632.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class PollingIoProcessorTest {
    @Ignore
    @Test
    public void testExceptionOnWrite() throws Exception {
        final Executor ex = Executors.newFixedThreadPool(1);

        IoConnector connector = new NioSocketConnector(new AbstractPollingIoProcessor<NioSession>(ex) {

            private NioProcessor proc = new NioProcessor(ex);

            @Override
            protected Iterator<NioSession> allSessions() {
                return proc.allSessions();
            }

            @Override
            protected void destroy(NioSession session) throws Exception {
                proc.destroy(session);
            }

            @Override
            protected void doDispose() throws Exception {
                proc.doDispose();
            }

            @Override
            protected void init(NioSession session) throws Exception {
                proc.init(session);
            }

            @Override
            protected boolean isInterestedInRead(NioSession session) {
                return proc.isInterestedInRead(session);
            }

            @Override
            protected boolean isInterestedInWrite(NioSession session) {
                return proc.isInterestedInWrite(session);
            }

            @Override
            protected boolean isReadable(NioSession session) {
                return proc.isReadable(session);
            }

            @Override
            protected boolean isSelectorEmpty() {
                return proc.isSelectorEmpty();
            }

            @Override
            protected boolean isWritable(NioSession session) {
                return proc.isWritable(session);
            }

            @Override
            protected int read(NioSession session, IoBuffer buf) throws Exception {
                return proc.read(session, buf);
            }

            @Override
            protected int select(long timeout) throws Exception {
                return proc.select(timeout);
            }

            @Override
            protected int select() throws Exception {
                return proc.select();
            }

            @Override
            protected Iterator<NioSession> selectedSessions() {
                return proc.selectedSessions();
            }

            @Override
            protected void setInterestedInRead(NioSession session, boolean interested) throws Exception {
                proc.setInterestedInRead(session, interested);
            }

            @Override
            protected void setInterestedInWrite(NioSession session, boolean interested) throws Exception {
                proc.setInterestedInWrite(session, interested);
            }

            @Override
            protected SessionState getState(NioSession session) {
                return proc.getState(session);
            }

            @Override
            protected int transferFile(NioSession session, FileRegion region, int length) throws Exception {
                return proc.transferFile(session, region, length);
            }

            @Override
            protected void wakeup() {
                proc.wakeup();
            }

            @Override
            protected int write(NioSession session, IoBuffer buf, int length) throws Exception {
                throw new NoRouteToHostException("No Route To Host Test");
            }

            @Override
            protected boolean isBrokenConnection() throws IOException {
                return proc.isBrokenConnection();
            }

            @Override
            protected void registerNewSelector() throws IOException {
                proc.registerNewSelector();
            }
        });
        connector.setHandler(new IoHandlerAdapter());

        IoAcceptor acceptor = new NioSocketAcceptor();
        acceptor.setHandler(new IoHandlerAdapter());

        InetSocketAddress addr = new InetSocketAddress("localhost", AvailablePortFinder.getNextAvailable(20000));

        acceptor.bind(addr);
        ConnectFuture future = connector.connect(addr);
        future.awaitUninterruptibly();
        IoSession session = future.getSession();
        WriteFuture wf = session.write(IoBuffer.allocate(1)).awaitUninterruptibly();
        assertNotNull(wf.getException());

        connector.dispose();
        acceptor.dispose();
    }
}
