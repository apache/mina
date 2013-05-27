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
package org.apache.mina.core.nio.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.BenchmarkClient;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

/**
 * A Netty 3 TCP CLient.
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Netty3TcpBenchmarkClient implements BenchmarkClient {

    private ChannelFactory factory;

    /**
     * 
     */
    public Netty3TcpBenchmarkClient() {
    }

    /**
     * {@inheritedDoc}
     */
    public void start(final int port, final CountDownLatch counter, final byte[] data) throws IOException {
        factory = new NioClientSocketChannelFactory();
        ClientBootstrap bootstrap = new ClientBootstrap(factory);
        bootstrap.setOption("sendBufferSize", 64 * 1024);
        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(new SimpleChannelUpstreamHandler() {
                    private void sendMessage(ChannelHandlerContext ctx, byte[] data) {
                        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(data);
                        ctx.getChannel().write(buffer);
                    }

                    @Override
                    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
                        if (e.getMessage() instanceof ChannelBuffer) {
                            ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
                            for (int i = 0; i < buffer.readableBytes(); ++i) {
                                counter.countDown();
                                if (counter.getCount() > 0) {
                                    sendMessage(ctx, data);
                                } else {
                                    ctx.getChannel().close();
                                }
                            }
                        } else {
                            throw new IllegalArgumentException(e.getMessage().getClass().getName());
                        }
                    }

                    @Override
                    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
                        sendMessage(ctx, data);
                    }

                });
            }
        });
        bootstrap.connect(new InetSocketAddress(port));
    }

    /**
     * {@inheritedDoc}
     */
    public void stop() throws IOException {
        factory.releaseExternalResources();
    }
}
