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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

import org.apache.mina.common.AbstractIoAcceptor;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoServiceListenerSupport;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.TransportMetadata;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.DefaultDatagramSessionConfig;
import org.apache.mina.util.NamePreservingRunnable;
import org.apache.mina.util.NewThreadExecutor;

/**
 * {@link IoAcceptor} for datagram transport (UDP/IP).
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DatagramAcceptor extends AbstractIoAcceptor implements
        org.apache.mina.transport.socket.DatagramAcceptor {

    private static volatile int nextId = 0;

    private final Executor executor;

    private final int id = nextId++;

    private final Selector selector;
    
    private final DatagramConnector connector;

    private DatagramChannel channel;

    private final Queue<ServiceOperationFuture> registerQueue =
        new ConcurrentLinkedQueue<ServiceOperationFuture>();

    private final Queue<ServiceOperationFuture> cancelQueue =
        new ConcurrentLinkedQueue<ServiceOperationFuture>();
    
    private final ConcurrentMap<SocketAddress, Object> cache =
        new ConcurrentHashMap<SocketAddress, Object>();

    private Worker worker;

    /**
     * Creates a new instance.
     */
    public DatagramAcceptor() {
        this(new NewThreadExecutor());
    }

    /**
     * Creates a new instance.
     */
    public DatagramAcceptor(Executor executor) {
        this(Runtime.getRuntime().availableProcessors() + 1, executor);
    }

    /**
     * Creates a new instance.
     */
    public DatagramAcceptor(int processorCount, Executor executor) {
        super(new DefaultDatagramSessionConfig());

        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeIOException("Failed to open a selector.", e);
        }

        this.executor = executor;
        this.connector = new DatagramConnector(
                this, "DatagramAcceptor-" + id, processorCount, executor);

        // The default reuseAddress should be 'true' for an accepted socket.
        getSessionConfig().setReuseAddress(true);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        try {
            selector.close();
        } catch (IOException e) {
            ExceptionMonitor.getInstance().exceptionCaught(e);
        }
    }

    public TransportMetadata getTransportMetadata() {
        return DatagramSessionImpl.METADATA;
    }

    @Override
    public DatagramSessionConfig getSessionConfig() {
        return (DatagramSessionConfig) super.getSessionConfig();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) super.getLocalAddress();
    }

    // This method is added to work around a problem with
    // bean property access mechanism.

    /**
     * @see org.apache.mina.common.AbstractIoAcceptor#setLocalAddress(java.net.SocketAddress)
     * @param localAddress the local address
     */
    public void setLocalAddress(InetSocketAddress localAddress) {
        super.setLocalAddress(localAddress);
    }
    
    @Override
    protected IoServiceListenerSupport getListeners() {
        return super.getListeners();
    }

    @Override
    protected void doBind() throws Exception {
        ServiceOperationFuture future = new ServiceOperationFuture();

        registerQueue.add(future);
        startupWorker();
        selector.wakeup();

        future.awaitUninterruptibly();

        if (future.getException() != null) {
            throw future.getException();
        }

        setLocalAddress(channel.socket().getLocalSocketAddress());
    }

    @Override
    protected void doUnbind() throws Exception {
        ServiceOperationFuture future = new ServiceOperationFuture();

        cancelQueue.add(future);
        startupWorker();
        selector.wakeup();

        future.awaitUninterruptibly();
        if (future.getException() != null) {
            throw future.getException();
        }
    }

    public IoSession newSession(SocketAddress remoteAddress) {
        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }

        synchronized (bindLock) {
            if (!isBound()) {
                throw new IllegalStateException(
                        "Can't create a session from a unbound service.");
            }
            
            Object data;
            synchronized (cache) { 
                data = cache.get(remoteAddress);
                if (data == null) {
                    ConnectFuture future = connector.connect(remoteAddress, getLocalAddress());
                    cache.put(remoteAddress, future);
                    future.awaitUninterruptibly();
                    return future.getSession();
                }
            }
            
            if (data instanceof ConnectFuture) {
                ConnectFuture future = (ConnectFuture) data;
                future.awaitUninterruptibly();
                return future.getSession();
            } else if (data instanceof IoSession) {
                return ((IoSession) data);
            } else {
                throw new IllegalStateException();
            }
        }
    }

    private synchronized void startupWorker() {
        if (worker == null) {
            worker = new Worker();
            executor.execute(new NamePreservingRunnable(worker));
        }
    }

    private class Worker implements Runnable {
        public void run() {
            Thread.currentThread().setName("DatagramAcceptor-" + id);

            for (; ;) {
                try {
                    int nKeys = selector.select();

                    registerNew();

                    if (nKeys > 0) {
                        processReadySessions(selector.selectedKeys());
                    }

                    cancelKeys();

                    if (selector.keys().isEmpty()) {
                        synchronized (DatagramAcceptor.this) {
                            if (selector.keys().isEmpty()
                                    && registerQueue.isEmpty()
                                    && cancelQueue.isEmpty()) {
                                worker = null;
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                    }
                }
            }
        }
    }

    private void processReadySessions(Set<SelectionKey> keys) {
        Iterator<SelectionKey> it = keys.iterator();
        while (it.hasNext()) {
            SelectionKey key = it.next();
            it.remove();

            DatagramChannel ch = (DatagramChannel) key.channel();

            try {
                if (key.isReadable()) {
                    readSession(ch);
                }
            } catch (Throwable t) {
                ExceptionMonitor.getInstance().exceptionCaught(t);
            }
        }
    }

    private void readSession(DatagramChannel channel) throws Exception {
        final ByteBuffer readBuf = ByteBuffer.allocate(getSessionConfig()
                .getReadBufferSize());

        final SocketAddress remoteAddress = channel.receive(readBuf.buf());
        if (remoteAddress != null) {
            readBuf.flip();
            Object data;
            ConnectFuture future = null;
            synchronized (cache) {
                data = cache.get(remoteAddress);
                if (data == null) {
                    future = connector.connect(remoteAddress, getLocalAddress());
                    cache.put(remoteAddress, future);
                }
            }
            
            if (data == null) {
                future.addListener(new IoFutureListener() {
                    public void operationComplete(IoFuture future) {
                        ConnectFuture f = (ConnectFuture) future;
                        if (f.getException() == null) {
                            IoSession s = f.getSession();
                            cache.put(remoteAddress, s);
                            s.getCloseFuture().addListener(new IoFutureListener() {
                                public void operationComplete(IoFuture future) {
                                    cache.remove(remoteAddress);
                                }
                            });
                            s.getFilterChain().fireMessageReceived(readBuf);
                        } else {
                            ExceptionMonitor.getInstance().exceptionCaught(f.getException());
                        }
                    }
                });
            } else if (data instanceof ConnectFuture) {
                future = (ConnectFuture) data;
                future.addListener(new IoFutureListener() {
                    public void operationComplete(IoFuture future) {
                        ConnectFuture f = (ConnectFuture) future;
                        if (f.getException() == null) {
                            IoSession s = f.getSession();
                            s.getFilterChain().fireMessageReceived(readBuf);
                        }
                    }
                });
            } else if (data instanceof IoSession) {
                ((IoSession) data).getFilterChain().fireMessageReceived(readBuf);
            }
        }
    }

    private void registerNew() {
        if (registerQueue.isEmpty()) {
            return;
        }

        for (; ;) {
            ServiceOperationFuture future = registerQueue.poll();
            if (future == null) {
                break;
            }

            DatagramChannel ch = null;
            try {
                ch = DatagramChannel.open();
                DatagramSessionConfig cfg = getSessionConfig();
                ch.socket().setReuseAddress(cfg.isReuseAddress());
                ch.socket().setBroadcast(cfg.isBroadcast());
                ch.socket().setReceiveBufferSize(cfg.getReceiveBufferSize());
                ch.socket().setSendBufferSize(cfg.getSendBufferSize());

                if (ch.socket().getTrafficClass() != cfg.getTrafficClass()) {
                    ch.socket().setTrafficClass(cfg.getTrafficClass());
                }

                ch.configureBlocking(false);
                ch.socket().bind(getLocalAddress());
                ch.register(selector, SelectionKey.OP_READ, future);
                this.channel = ch;

                future.setDone();
            } catch (Exception e) {
                future.setException(e);
            } finally {
                if (ch != null && future.getException() != null) {
                    try {
                        ch.disconnect();
                        ch.close();
                    } catch (Throwable e) {
                        ExceptionMonitor.getInstance().exceptionCaught(e);
                    }
                }
            }
        }
    }

    private void cancelKeys() {
        for (; ;) {
            ServiceOperationFuture future = cancelQueue.poll();
            if (future == null) {
                break;
            }

            DatagramChannel ch = this.channel;
            this.channel = null;

            // close the channel
            try {
                SelectionKey key = ch.keyFor(selector);
                key.cancel();
                selector.wakeup(); // wake up again to trigger thread death
                ch.disconnect();
                ch.close();
            } catch (Throwable t) {
                ExceptionMonitor.getInstance().exceptionCaught(t);
            } finally {
                future.setDone();
                getListeners().fireServiceDeactivated();
            }
        }
    }
}
