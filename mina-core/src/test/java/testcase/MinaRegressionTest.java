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
package testcase;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.executor.OrderedThreadPoolExecutor;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO : Add documentation
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 */
public class MinaRegressionTest extends IoHandlerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(MinaRegressionTest.class);

  public static final int MSG_SIZE = 5000;
  public static final int MSG_COUNT = 10;
  private static final int PORT = 23234;
  private static final int BUFFER_SIZE = 8192;
  private static final int TIMEOUT = 10000;

  public static final String OPEN = "open";

  public SocketAcceptor acceptor;
  public SocketConnector connector;

  private final Object LOCK = new Object();

  private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
    public Thread newThread(final Runnable r) {
      return new Thread(null, r, "MinaThread", 64 * 1024);
    }
  };

  private OrderedThreadPoolExecutor executor;

  public static AtomicInteger sent = new AtomicInteger(0);


  public MinaRegressionTest() throws IOException {
    executor = new OrderedThreadPoolExecutor(
      0,
      1000,
      60,
      TimeUnit.SECONDS,
      THREAD_FACTORY);

    acceptor = new NioSocketAcceptor(Runtime.getRuntime().availableProcessors() + 1);
    acceptor.setReuseAddress( true );
    acceptor.getSessionConfig().setReceiveBufferSize(BUFFER_SIZE);

    acceptor.getFilterChain().addLast("threadPool", new ExecutorFilter(executor));
    acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MyProtocolCodecFactory()));

    connector = new NioSocketConnector(Runtime.getRuntime().availableProcessors() + 1);

    connector.setConnectTimeoutMillis(TIMEOUT);
    connector.getSessionConfig().setSendBufferSize(BUFFER_SIZE);
    connector.getSessionConfig().setReuseAddress( true );
  }

  public void connect() throws Exception {
    final InetSocketAddress socketAddress = new InetSocketAddress("0.0.0.0", PORT);

    acceptor.setHandler(new MyIoHandler(LOCK));

    acceptor.bind(socketAddress);
    connector.setHandler(this);

    final IoFutureListener<ConnectFuture> listener = new IoFutureListener<ConnectFuture>() {
      public void operationComplete(ConnectFuture future) {
        try {logger.info( "Write message to session " + future.getSession().getId() );
          final IoSession s = future.getSession();
          IoBuffer wb = IoBuffer.allocate(MSG_SIZE);
          wb.put(new byte[MSG_SIZE]);
          wb.flip();
          s.write(wb);
        } catch (Exception e) {
          logger.error("Can't send message: {}", e.getMessage());
        }
      }
    };

    for (int i = 0; i < MSG_COUNT; i++) {
      ConnectFuture future = connector.connect(socketAddress);
      future.addListener(listener);
    }

    synchronized (LOCK) {
      LOCK.wait(50000);
    }

    connector.dispose();
    acceptor.unbind();
    acceptor.dispose();
    executor.shutdownNow();

    logger.info("Received: " + MyIoHandler.received.intValue());
    logger.info("Sent: " + sent.intValue());
    logger.info("FINISH");
  }

  @Override
  public void exceptionCaught(IoSession session, Throwable cause) {
    if (!(cause instanceof IOException)) {
      logger.error("Exception: ", cause);
    } else {
      logger.info("I/O error: " + cause.getMessage());
    }
    session.close(true);
  }

  @Override
  public void messageSent(IoSession session, Object message) throws Exception {
    sent.incrementAndGet();
  }

  public static void main(String[] args) throws Exception {
    logger.info("START");
    new MinaRegressionTest().connect();
  }
}