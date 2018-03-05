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

package org.apache.mina.core.service;

import static org.junit.Assert.fail;

import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

/**
 * Test disposal of AbstractIoService. This test should not hang or timeout when DIRMINA-1076 is fixed.
 * 
 * @author chrjohn
 */
public class AbstractIoServiceDIRMINA1076Test {

    @Test( timeout = 15000 )
    public void testDispose()
        throws Exception {

        long startTime = System.currentTimeMillis();
        // without DIRMINA-1076 fixed, the test will hang after short time
        while ( System.currentTimeMillis() < startTime + 10000 ) {
            final CountDownLatch disposalLatch = new CountDownLatch( 1 );
            Thread thread = new Thread() {

                public void run() {

                    final IoAcceptor acceptor = new NioSocketAcceptor();
                    acceptor.getFilterChain()
                            .addLast( "codec",
                                      new ProtocolCodecFilter( new TextLineCodecFactory( Charset.forName( "UTF-8" ) ) ) );

                    acceptor.setHandler( new ServerHandler() );

                    acceptor.getSessionConfig().setReadBufferSize( 2048 );
                    acceptor.getSessionConfig().setIdleTime( IdleStatus.BOTH_IDLE, 10 );
                    int nextAvailable = AvailablePortFinder.getNextAvailable();
                    try {
                        acceptor.bind( new InetSocketAddress( nextAvailable ) );
                    } catch ( IOException e1 ) {
                        throw new RuntimeException( e1 );
                    }

                    final NioSocketConnector connector = new NioSocketConnector();

                    // Set connect timeout.
                    connector.setConnectTimeoutMillis( 30 * 1000L );

                    connector.setHandler( new ClientHandler() );
                    connector.getFilterChain()
                             .addLast( "codec",
                                       new ProtocolCodecFilter( new TextLineCodecFactory( Charset.forName( "UTF-8" ) ) ) );

                    // Start communication.
                    ConnectFuture cf = connector.connect( new InetSocketAddress( "localhost", nextAvailable ) );
                    cf.awaitUninterruptibly();

                    IoSession session = cf.getSession();

                    // send a message
                    session.write( "Hello World!\r" );

                    // wait until response is received
                    CountDownLatch latch = (CountDownLatch)session.getAttribute( "latch" );
                    try {
                        latch.await();
                    } catch ( InterruptedException e1 ) {
                        Thread.currentThread().interrupt();
                    }

                    // close the session
                    CloseFuture closeFuture = session.closeOnFlush();

                    connector.dispose( true );

                    closeFuture.awaitUninterruptibly();
                    acceptor.unbind();
                    acceptor.dispose( true );
                    disposalLatch.countDown();
                }
            };
            thread.setDaemon( true );
            thread.start();
            disposalLatch.await();
            thread.join( 1000 );

            if ( thread.isAlive() ) {
                for ( StackTraceElement stackTraceElement : thread.getStackTrace() ) {
                    if ( "dispose".equals( stackTraceElement.getMethodName() )
                         && AbstractIoService.class.getCanonicalName().equals( stackTraceElement.getClassName() ) ) {
                        System.err.println( "Detected hang in AbstractIoService.dispose()!" );
                    }
                }
                fail( "Thread should have died by now, supposed hang in AbstractIoService.dispose()" );
            }
        }
        ;
    }

    public static class ClientHandler
        extends
        IoHandlerAdapter {

        @Override
        public void sessionCreated( IoSession session )
            throws Exception {
            session.setAttribute( "latch", new CountDownLatch( 1 ) );
        }



        @Override
        public void messageReceived( IoSession session, Object message )
            throws Exception {
            CountDownLatch latch = (CountDownLatch)session.getAttribute( "latch" );
            latch.countDown();
        }



        @Override
        public void exceptionCaught( IoSession session, Throwable cause )
            throws Exception {}
    }

    public static class ServerHandler
        extends
        IoHandlerAdapter {

        @Override
        public void messageReceived( IoSession session, Object message )
            throws Exception {
            session.write( message.toString() );
        }



        @Override
        public void exceptionCaught( IoSession session, Throwable cause )
            throws Exception {}

    }

}
