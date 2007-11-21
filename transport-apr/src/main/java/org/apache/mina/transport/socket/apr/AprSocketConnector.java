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
package org.apache.mina.transport.socket.apr;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.apache.mina.common.AbstractPollingIoConnector;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.DefaultConnectFuture;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoProcessor;
import org.apache.mina.common.RuntimeIoException;
import org.apache.mina.common.TransportMetadata;
import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.util.CircularQueue;
import org.apache.tomcat.jni.Address;
import org.apache.tomcat.jni.Poll;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.Socket;
import org.apache.tomcat.jni.Status;

/**
 * An {@link IoConnector} implementation using the Apache Portable Runtime 
 * (APR : http://apr.apache.org) for providing socket.
 * It's supporting different transport protocol, so you need to give the 
 * wanted {@link AprProtocol} as parameter to the constructor.
 * @see AprSession
 * @see AprProtocol
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class AprSocketConnector extends AbstractPollingIoConnector<AprSession, Long> implements SocketConnector {

    private static final int INITIAL_CAPACITY = 32;

    private final Map<Long, AprSession> allSessions =
        new HashMap<Long, AprSession>(INITIAL_CAPACITY);
    
    private final Object wakeupLock = new Object();
    private long wakeupSocket;
    private volatile boolean toBeWakenUp;

    private volatile long bufferPool; // memory pool
    private volatile long pollset; // socket poller
    private long[] polledSockets = new long[INITIAL_CAPACITY << 1];
    private final List<AprSession> polledSessions =
        new CircularQueue<AprSession>(INITIAL_CAPACITY);

    public AprSocketConnector() {
        super(new DefaultSocketSessionConfig(), AprIoProcessor.class);
    }
    
    public AprSocketConnector(int processorCount) {
        super(new DefaultSocketSessionConfig(), AprIoProcessor.class, processorCount);
    }
    
    public AprSocketConnector(IoProcessor<AprSession> processor) {
        super(new DefaultSocketSessionConfig(), processor);
    }

    public AprSocketConnector(Executor executor, IoProcessor<AprSession> processor) {
        super(new DefaultSocketSessionConfig(), executor, processor);
    }
    
    @Override
    protected Iterator<Long> allHandles() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected boolean connect(Long handle, SocketAddress remoteAddress)
            throws Exception {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected ConnectionRequest connectionRequest(Long handle) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void destroy(Long handle) throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void doDispose0() {
        Poll.destroy(pollset);
        Pool.destroy(bufferPool);
        Socket.close(wakeupSocket);
    }

    @Override
    protected void doInit() {
        // load the APR library
        AprLibrary.initialize();
        
        try {
            wakeupSocket = Socket.create(
                    Socket.APR_INET, Socket.SOCK_DGRAM, Socket.APR_PROTO_UDP, AprLibrary
                    .getInstance().getRootPool());
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeIoException("Failed to create a wakeup socket.", e);
        }

        // initialize a memory pool for APR functions
        bufferPool = Pool.create(AprLibrary.getInstance().getRootPool());
        
        boolean success = false;
        try {
            // TODO : optimize/parameterize those values
            pollset = Poll
                    .create(
                            INITIAL_CAPACITY,
                            AprLibrary.getInstance().getRootPool(),
                            Poll.APR_POLLSET_THREADSAFE,
                            10000000);
            if (pollset < 0) {
                if (Status.APR_STATUS_IS_ENOTIMPL(- (int) pollset)) {
                    throw new RuntimeIoException(
                            "Thread-safe pollset is not supported in this platform.");
                }
            }
            success = true;
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeIoException("Failed to create a pollset.", e);
        } finally {
            if (!success) {
                dispose();
            }
        }
    }

    @Override
    protected void finishConnect(Long handle) throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    protected Long newHandle(SocketAddress localAddress) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected AprSession newSession(IoProcessor<AprSession> processor,
            Long handle) {
        try {
            return new AprSocketSession(this, processor, handle);
        } catch (Exception e) {
            ExceptionMonitor.getInstance().exceptionCaught(e);
        }
        return null;
    }

    @Override
    protected void register(Long handle, ConnectionRequest request)
            throws Exception {
        Poll.add(pollset, handle, Poll.APR_POLLIN);
    }

    @Override
    protected boolean select(int timeout) throws Exception {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected boolean selectable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected Iterator<Long> selectedHandles() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void wakeup() {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected ConnectFuture doConnect(SocketAddress remoteAddress,
            SocketAddress localAddress) {
        // FIXME: this operation should be non-blocking and asynchronous like NioSocketConnector.
        try {
            InetSocketAddress sockAddr = (InetSocketAddress) remoteAddress;
            //pool = Pool.create(pool);
            long inetAddr = 0;
            inetAddr = Address.info(sockAddr.getHostName(), Socket.APR_INET,
                    sockAddr.getPort(), 0, AprLibrary.getInstance().getRootPool());

            long clientSock = 0;
            
            // FIXME : error checking
            int ret = Socket.connect(clientSock, inetAddr);
            if(ret!=Status.APR_SUCCESS)
                System.err.println("Error Socket.connect : " + ret);
            
            ret=Socket.optSet(clientSock, Socket.APR_SO_NONBLOCK, 1);

            if(ret!=Status.APR_SUCCESS)
                System.err.println("Error Socket.optSet : " + ret);

            if (localAddress != null) {
                // TODO, check if it's possible to bind to a local address
            }

            ConnectFuture future = new DefaultConnectFuture();
//            AprSession session = new AprSocketSession(
//                    this, getProcessor(), clientSock, sockAddr, (InetSocketAddress) localAddress);
//            
//            finishSessionInitialization(session, future);
//
//            try {
//                getFilterChainBuilder().buildFilterChain(
//                        session.getFilterChain());
//            } catch (Throwable e) {
//                throw (IOException) new IOException(
//                        "Failed to create a session.").initCause(e);
//            }
//
//            // Forward the remaining process to the APRIoProcessor.
//            // it's will validate the ConnectFuture when the session is in the poll set
//            session.getProcessor().add(session);
//            return future;
        } catch (Exception e) {
            return DefaultConnectFuture.newFailedFuture(e);
        }
        
        return null;
    }

    public TransportMetadata getTransportMetadata() {
        return AprSocketSession.METADATA;
    }
    
    @Override
    public SocketSessionConfig getSessionConfig() {
        return (SocketSessionConfig) super.getSessionConfig();
    }
}
