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

import junit.framework.Assert;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * test the AbstractIoService
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class AbstractIoServiceTest {

  private static final int PORT = 9123;

  @Test
  public void testDispose() throws IOException, InterruptedException {

    List threadsBefore = getThreadNames();

    final IoAcceptor acceptor = new NioSocketAcceptor();

    acceptor.getFilterChain().addLast( "logger", new LoggingFilter() );
    acceptor.getFilterChain().addLast( "codec", new ProtocolCodecFilter( new TextLineCodecFactory( Charset.forName( "UTF-8" ))));

    acceptor.setHandler(  new ServerHandler() );

    acceptor.getSessionConfig().setReadBufferSize( 2048 );
    acceptor.getSessionConfig().setIdleTime( IdleStatus.BOTH_IDLE, 10 );
    acceptor.bind( new InetSocketAddress(PORT) );
    System.out.println("Server running ...");

    final NioSocketConnector connector = new NioSocketConnector();

    // Set connect timeout.
    connector.setConnectTimeoutMillis(30 * 1000L);

    connector.setHandler(new ClientHandler());
    connector.getFilterChain().addLast( "logger", new LoggingFilter() );
    connector.getFilterChain().addLast( "codec", new ProtocolCodecFilter( new TextLineCodecFactory( Charset.forName( "UTF-8" ))));

    // Start communication.
    ConnectFuture cf = connector.connect(new InetSocketAddress("localhost", 9123));
    cf.awaitUninterruptibly();

    IoSession session = cf.getSession();

    // send a message
    session.write("Hello World!\r");

    // wait until response is received
    CountDownLatch latch = (CountDownLatch) session.getAttribute("latch");
    latch.await();

    // close the session
    CloseFuture closeFuture = session.close(false);

    System.out.println("session.close called");
    //Thread.sleep(5);

    // wait for session close and then dispose the connector
    closeFuture.addListener(new IoFutureListener<IoFuture>() {

      public void operationComplete(IoFuture future) {
        System.out.println("managed session count=" + connector.getManagedSessionCount());
        System.out.println("Disposing connector ...");
        connector.dispose(true);
        System.out.println("Disposing connector ... *finished*");

      }
    });

    closeFuture.awaitUninterruptibly();    
    acceptor.dispose(true);

    List threadsAfter = getThreadNames();

    System.out.println("threadsBefore = " + threadsBefore);
    System.out.println("threadsAfter  = " + threadsAfter);

    // Assert.assertEquals(threadsBefore, threadsAfter);

  }


  public static class ClientHandler extends IoHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger("CLIENT");

    @Override
    public void sessionCreated(IoSession session) throws Exception {
      session.setAttribute("latch", new CountDownLatch(1));
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
      LOGGER.info("client: messageReceived("+session+", "+message+")");
      CountDownLatch latch = (CountDownLatch) session.getAttribute("latch");
      latch.countDown();
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
      LOGGER.warn("exceptionCaught:", cause);
    }
  }

  public static class ServerHandler extends IoHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger("SERVER");

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
      LOGGER.info("server: messageReceived("+session+", "+message+")");
      session.write(message.toString());
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
      LOGGER.warn("exceptionCaught:", cause);
    }

  }

  public static void main(String[] args) throws IOException, InterruptedException {
    new AbstractIoServiceTest().testDispose();
  }

  private List<String> getThreadNames() {
      List<String> list = new ArrayList<String>();
      int active = Thread.activeCount();
      Thread[] threads = new Thread[active];
      Thread.enumerate(threads);
      for (Thread thread : threads) {
          try {
              String name = thread.getName();
              list.add(name);
          } catch (NullPointerException ignore) {
          }
      }
      return list;
  }

}
