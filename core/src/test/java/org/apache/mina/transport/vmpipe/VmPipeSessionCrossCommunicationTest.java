/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mina.transport.vmpipe;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoAcceptorConfig;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;

import junit.framework.TestCase;

/**
 * @author Apache Mina Project (dev@mina.apache.org)
 * @version $Rev: $, $Date:  $
 */
public class VmPipeSessionCrossCommunicationTest extends TestCase {
    public void testOneSessionTalkingBackAndForthDoesNotDeadlock() throws Exception {
        final VmPipeAddress address = new VmPipeAddress( 1 );
        final IoConnector connector = new VmPipeConnector();
        final AtomicReference<IoSession> c1 = new AtomicReference<IoSession>();
        final CountDownLatch latch = new CountDownLatch( 1 );
        final CountDownLatch messageCount = new CountDownLatch( 2 );
        IoAcceptor acceptor = new VmPipeAcceptor();

        acceptor.bind( address, new IoHandlerAdapter() {
            @Override
            public void messageReceived( IoSession session, Object message ) throws Exception {
                System.out.println( Thread.currentThread().getName() + ": " + message );

                if ( "start".equals( message ) ) {
                    session.write( "open new" );
                } else if ( "re-use c1".equals( message ) ) {
                    session.write( "tell me something on c1 now" );
                } else if ( ( (String) message ).startsWith( "please don't deadlock" ) ) {
                    messageCount.countDown();
                } else {
                    fail( "unexpected message received " + message );
                }
            }
        } );

        connector.getDefaultConfig().setThreadModel( ThreadModel.MANUAL );

        ConnectFuture future = connector.connect( address, new IoHandlerAdapter() {
            @Override
            public void messageReceived( IoSession session, Object message ) throws Exception {
                System.out.println( Thread.currentThread().getName() + ": " + message );
                
                if ( "open new".equals( message ) ) {
                    System.out.println( "opening c2 from " + Thread.currentThread().getName() );

                    ConnectFuture c2Future = connector.connect( address, new IoHandlerAdapter() {
                        @Override
                        public void sessionOpened( IoSession session ) throws Exception {
                            session.write( "re-use c1" );
                        }

                        @Override
                        public void messageReceived( IoSession session, Object message ) throws Exception {
                            System.out.println( Thread.currentThread().getName() + ": " + message );

                            if ( "tell me something on c1 now".equals( message ) ) {
                                latch.countDown();
                                c1.get().write( "please don't deadlock via c1" );
                            } else {
                                fail( "unexpected message received " + message );
                            }
                        }
                    } );

                    c2Future.join();

                    latch.await();

                    c2Future.getSession().write( "please don't deadlock via c2" );
                } else {
                    fail( "unexpeced message received " + message );
                }
            }
        } );

        future.join();

        c1.set( future.getSession() );
        c1.get().write( "start" );

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        while ( !messageCount.await( 100, TimeUnit.MILLISECONDS ) ) {
            long[] threads = threadMXBean.findMonitorDeadlockedThreads();

            if ( null != threads ) {
                StringBuffer sb = new StringBuffer( 256 );
                ThreadInfo[] infos = threadMXBean.getThreadInfo( threads, Integer.MAX_VALUE );

                for ( ThreadInfo info : infos ) {
                    sb.append( info.getThreadName() )
                        .append( " blocked on " )
                        .append( info.getLockName() )
                        .append( " owned by " )
                        .append( info.getLockOwnerName() )
                        .append( "\n" );
                }

                for ( ThreadInfo info : infos ) {
                    sb.append( "\nStack for " ).append( info.getThreadName() ).append( "\n" );
                    for ( StackTraceElement element : info.getStackTrace() ) {
                        sb.append( "\t" ).append( element ).append( "\n" );
                    }
                }

                fail( "deadlocked! \n" + sb );
            }
        }

        ( (IoAcceptorConfig) acceptor.getDefaultConfig() ).setDisconnectOnUnbind( false );
        acceptor.unbindAll();
    }
}
