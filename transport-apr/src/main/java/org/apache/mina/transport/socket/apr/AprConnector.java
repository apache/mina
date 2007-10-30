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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;

import org.apache.mina.common.AbstractIoConnector;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.DefaultConnectFuture;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoServiceListenerSupport;
import org.apache.mina.common.TransportMetadata;
import org.apache.mina.util.NewThreadExecutor;
import org.apache.tomcat.jni.Address;
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
public class AprConnector extends AbstractIoConnector {

    /**
     * @noinspection StaticNonFinalField
     */
    private static volatile int nextId = 0;

    private final int id = nextId++;

    private final String threadName = "APRConnector-" + id;

    private final int processorCount;

    private final Executor executor;

    private final AprIoProcessor[] ioProcessors;

    private int processorDistributor = 0;

    private AprProtocol protocol;

    /**
     * Create a connector with a single processing thread using a
     * NewThreadExecutor
     * @param protocol 
     *            The needed socket protocol (TCP,UDP,...)
     */
    public AprConnector(AprProtocol protocol) {
        this(protocol, 1, new NewThreadExecutor());
    }

    /**
     * Create a connector with the desired number of processing threads
     * 
     * @param protocol 
     * 			  The needed socket protocol (TCP,UDP,...)
     * @param processorCount
     *            Number of processing threads
     * @param executor
     *            Executor to use for launching threads
     */
    public AprConnector(AprProtocol protocol, int processorCount,
            Executor executor) {
        super(new DefaultAprSessionConfig());

        this.protocol = protocol;

        // load  the APR library

        AprLibrary.initialize();

        if (processorCount < 1) {
            throw new IllegalArgumentException(
                    "Must have at least one processor");
        }

        this.executor = executor;
        this.processorCount = processorCount;
        ioProcessors = new AprIoProcessor[processorCount];

        for (int i = 0; i < processorCount; i++) {
            ioProcessors[i] = new AprIoProcessor(
                    threadName + "." + i, executor);
        }
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
                    sockAddr.getPort(), 0, AprLibrary.getLibrary().getPool());

            long clientSock = Socket.create(Socket.APR_INET,
                    protocol.socketType, protocol.codeProto, AprLibrary
                            .getLibrary().getPool());

            
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
            AprIoProcessor proc=nextProcessor();
            System.err.println("proc : "+proc);
            AprSessionImpl session = new AprSessionImpl(this,proc ,
                    clientSock, sockAddr, (InetSocketAddress) localAddress);
            
            finishSessionInitialization(session, future);

            try {
                getFilterChainBuilder().buildFilterChain(
                        session.getFilterChain());
            } catch (Throwable e) {
                throw (IOException) new IOException(
                        "Failed to create a session.").initCause(e);
            }

            // Forward the remaining process to the APRIoProcessor.
            // it's will validate the ConnectFuture when the session is in the poll set
            session.getIoProcessor().add(session);
            return future;
        } catch (Exception e) {
            return DefaultConnectFuture.newFailedFuture(e);
        }
    }

    @Override
    protected IoServiceListenerSupport getListeners() {
        return super.getListeners();
    }

    private AprIoProcessor nextProcessor() {
        if (this.processorDistributor == Integer.MAX_VALUE) {
            this.processorDistributor = Integer.MAX_VALUE % this.processorCount;
        }
        return ioProcessors[processorDistributor++ % processorCount];
    }

    public TransportMetadata getTransportMetadata() {
        return AprSessionImpl.METADATA;
    }
}
