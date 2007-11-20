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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.common.AbstractIoConnector;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.DefaultConnectFuture;
import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoProcessor;
import org.apache.mina.common.IoServiceListenerSupport;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.SimpleIoProcessorPool;
import org.apache.mina.common.TransportMetadata;
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

    private static final AtomicInteger id = new AtomicInteger();

    private final String threadName;
    private final IoProcessor<AprSession> processor;
    private final AprProtocol protocol;
    private final boolean createdProcessor;

    /**
     * Create a connector with a single processing thread using a
     * NewThreadExecutor
     * @param protocol 
     *            The needed socket protocol (TCP,UDP,...)
     */
    public AprConnector(AprProtocol protocol) {
        this(protocol, new SimpleIoProcessorPool<AprSession>(AprIoProcessor.class), true);
    }
    
    public AprConnector(AprProtocol protocol, int processorCount) {
        this(protocol, new SimpleIoProcessorPool<AprSession>(AprIoProcessor.class, processorCount), true);
    }
    
    public AprConnector(AprProtocol protocol, IoProcessor<AprSession> processor) {
        this(protocol, processor, false);
    }

    /**
     * Create a connector with the desired number of processing threads
     * 
     * @param protocol 
     * 			  The needed socket protocol (TCP,UDP,...)
     */
    private AprConnector(AprProtocol protocol, IoProcessor<AprSession> processor, boolean createdProcessor) {
        super(new DefaultAprSessionConfig());

        if (protocol == null) {
            throw new NullPointerException("protocol");
        }
        if (processor == null) {
            throw new NullPointerException("processor");
        }
        
        this.threadName = getClass().getSimpleName() + '-' + id.incrementAndGet();
        this.protocol = protocol;
        this.processor = processor;
        this.createdProcessor = createdProcessor;

        // load the APR library
        AprLibrary.initialize();
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

            long clientSock = Socket.create(Socket.APR_INET,
                    protocol.socketType, protocol.codeProto, AprLibrary
                            .getInstance().getRootPool());

            
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
            AprSession session = new AprSession(
                    this, processor, clientSock, sockAddr, (InetSocketAddress) localAddress);
            
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
            session.getProcessor().add(session);
            return future;
        } catch (Exception e) {
            return DefaultConnectFuture.newFailedFuture(e);
        }
    }

    @Override
    protected void doDispose() throws Exception {
        if (createdProcessor) {
            processor.dispose();
        }
    }

    @Override
    protected IoServiceListenerSupport getListeners() {
        return super.getListeners();
    }

    public TransportMetadata getTransportMetadata() {
        return AprSession.METADATA;
    }
    
    public static void main(String[] args) throws Exception {
        IoConnector c = new AprConnector(AprProtocol.TCP);
//        IoConnector c = new NioSocketConnector();
        c.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                session.write(((IoBuffer) message).duplicate());
            }

            @Override
            public void exceptionCaught(IoSession session, Throwable cause)
                    throws Exception {
                cause.printStackTrace();
            }
        });
        
        for (int i = 0; i < 4; i ++) {
            ConnectFuture f = c.connect(new InetSocketAddress("127.0.0.1", 8080));
            f.awaitUninterruptibly();
            IoSession session = f.getSession();
            session.write(IoBuffer.allocate(1048576));
        }
    }
}
