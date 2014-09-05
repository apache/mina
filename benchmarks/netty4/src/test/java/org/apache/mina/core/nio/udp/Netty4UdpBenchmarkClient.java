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
package org.apache.mina.core.nio.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.BenchmarkClient;

/**
 * A Netty 4 based UDP client
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Netty4UdpBenchmarkClient implements BenchmarkClient {

    private Bootstrap bootstrap;

    /**
     * 
     */
    public Netty4UdpBenchmarkClient() {
    }

    /**
     * {@inheritedDoc}
     */
    public void start(final int port, final CountDownLatch counter, final byte[] data) throws IOException {
        bootstrap = new Bootstrap();
        bootstrap.option(ChannelOption.SO_SNDBUF, 65536);
        bootstrap.group(new NioEventLoopGroup());
        bootstrap.channel(NioDatagramChannel.class);
        bootstrap.handler(new ChannelInitializer<DatagramChannel>() {

            @Override
            protected void initChannel(DatagramChannel ch) throws Exception {
                ch.pipeline().addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
                    private void sendMessage(ChannelHandlerContext ctx, byte[] data, InetSocketAddress address) {
                        ByteBuf buf = ctx.alloc().buffer(data.length);
                        buf.writeBytes(data);
                        ctx.writeAndFlush(new DatagramPacket(buf, address));
                    }
                  
                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        sendMessage(ctx, data, new InetSocketAddress("localhost", port));
                    }

                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket message) throws Exception {
                        for(int i=0; i < message.content().readableBytes();i++) {
                            counter.countDown();
                            if (counter.getCount() > 0) {
                                sendMessage(ctx, data, message.sender());
                            } else {
                                ctx.channel().close();
                            }
                        }
                    }
                });
            }
        });
        bootstrap.bind(port+1);
    }

    /**
     * {@inheritedDoc}
     */
    public void stop() throws IOException {
        bootstrap.group().shutdownGracefully();
    }
}
