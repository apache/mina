/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.mina.example.imagine.step2.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.example.imagine.step1.codec.ImageCodecFactory;
import org.apache.mina.example.imagine.step1.server.ImageServerIoHandler;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 * entry point for the server used in the tutorial on protocol codecs
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */

public class ImageServer {
    public static final int PORT = 33789;

    public static void main(String[] args) throws IOException {
        // Create a class that handles sessions, incoming and outgoing data
        ImageServerIoHandler handler = new ImageServerIoHandler();
        
        // This socket acceptor will handle incoming connections
        NioSocketAcceptor acceptor = new NioSocketAcceptor();
        
        // add an IoFilter .  This class is responsible for converting the incoming and 
        // outgoing raw data to ImageRequest and ImageResponse objects
        acceptor.getFilterChain().addLast("protocol", new ProtocolCodecFilter(new ImageCodecFactory(false)));
        
        // get a reference to the filter chain from the acceptor
        DefaultIoFilterChainBuilder filterChainBuilder = acceptor.getFilterChain();
        
        // add an ExecutorFilter to the filter chain.  The preferred order is to put the executor filter
        // after any protocol filters due to the fact that protocol codecs are generally CPU-bound
        // which is the same as I/O filters.
        filterChainBuilder.addLast("threadPool", new ExecutorFilter(Executors.newCachedThreadPool()));
        
        // set this NioSocketAcceptor's handler to the ImageServerHandler
        acceptor.setHandler(handler);
        
        // Bind to the specified address.  This kicks off the listening for 
        // incoming connections
        acceptor.bind(new InetSocketAddress(PORT));
        System.out.println("Step 2 server is listenig at port " + PORT);
    }
}
