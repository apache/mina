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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.BenchmarkClient;

/**
 * A Netty 3 TCP CLient.
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Netty4TcpBenchmarkClient implements BenchmarkClient {

    private EventLoopGroup group = new NioEventLoopGroup();
  
    /**
     * 
     */
    public Netty4TcpBenchmarkClient() {
    }

    /**
     * {@inheritedDoc}
     */
    public void start(final int port, final CountDownLatch counter, final byte[] data) throws IOException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group);
        bootstrap.option(ChannelOption.SO_SNDBUF, 64 * 1024);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                    private void sendMessage(ChannelHandlerContext ctx, byte[] data) {
                        ByteBuf buf = ctx.alloc().buffer(data.length);
                        buf.writeBytes(data);
                        ctx.writeAndFlush(buf);
                    }

                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
                        ByteBuf buf = (ByteBuf)message;
                        for(int i=0; i < buf.readableBytes();i++) {
                            counter.countDown();
                            if (counter.getCount() > 0) {
                                sendMessage(ctx, data);
                            } else {
                                ctx.channel().close();
                            }
                        }
                        buf.release();
                    }

                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                      sendMessage(ctx, data);
                    }
                });
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                cause.printStackTrace();
                ctx.close();
            }
        });
        bootstrap.connect(new InetSocketAddress(port));
    }

    /**
     * {@inheritedDoc}
     */
    public void stop() throws IOException {
        group.shutdownGracefully();
    }
}
