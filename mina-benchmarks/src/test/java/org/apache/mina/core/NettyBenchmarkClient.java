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
package org.apache.mina.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NettyBenchmarkClient implements BenchmarkClient {

    private EventLoopGroup group;

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final int port, final CountDownLatch counter, final byte[] data) throws IOException {
        group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            ChannelFuture connected = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_SNDBUF, 64 * 1024)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        channel.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) {
                                sendMessage(ctx);
                            }

                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buffer) {
                                for (int i = 0; i < buffer.readableBytes(); ++i) {
                                    counter.countDown();
                                    if (counter.getCount() == 0) {
                                        ctx.close();
                                        return;
                                    }
                                    sendMessage(ctx);
                                }
                            }

                            private void sendMessage(ChannelHandlerContext ctx) {
                                ctx.writeAndFlush(Unpooled.wrappedBuffer(data));
                            }
                        });
                    }
                })
                .connect(new InetSocketAddress(port)).awaitUninterruptibly();
            if (!connected.isSuccess()) {
                throw new IOException("Could not connect Netty benchmark client to port " + port, connected.cause());
            }
        } catch (IOException | RuntimeException | Error e) {
            stop();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (group != null) {
            group.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).awaitUninterruptibly();
            group = null;
        }
    }
}
