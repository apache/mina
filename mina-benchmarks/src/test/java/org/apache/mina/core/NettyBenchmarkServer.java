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
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.ServerBootstrap;
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
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NettyBenchmarkServer implements BenchmarkServer {

    private enum State {
        WAIT_FOR_FIRST_BYTE_LENGTH, WAIT_FOR_SECOND_BYTE_LENGTH, WAIT_FOR_THIRD_BYTE_LENGTH, WAIT_FOR_FOURTH_BYTE_LENGTH, READING
    }

    private static final ByteBuf ACK = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(new byte[] { 0 }));

    private EventLoopGroup acceptorGroup;
    private EventLoopGroup workerGroup;

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(int port) throws IOException {
        acceptorGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        try {
            workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
            ChannelFuture bound = new ServerBootstrap()
                .group(acceptorGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_RCVBUF, 128 * 1024)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        channel.pipeline().addLast(new BenchmarkServerHandler());
                    }
                })
                .bind(new InetSocketAddress(port)).awaitUninterruptibly();
            if (!bound.isSuccess()) {
                throw new IOException("Could not bind Netty benchmark server to port " + port, bound.cause());
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
        if (acceptorGroup != null) {
            acceptorGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).awaitUninterruptibly();
            acceptorGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).awaitUninterruptibly();
            workerGroup = null;
        }
    }

    private static final class BenchmarkServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

        private State state = State.WAIT_FOR_FIRST_BYTE_LENGTH;

        private int length;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buffer) {
            while (buffer.isReadable()) {
                switch (state) {
                case WAIT_FOR_FIRST_BYTE_LENGTH:
                    length = buffer.readUnsignedByte() << 24;
                    state = State.WAIT_FOR_SECOND_BYTE_LENGTH;
                    break;
                case WAIT_FOR_SECOND_BYTE_LENGTH:
                    length += buffer.readUnsignedByte() << 16;
                    state = State.WAIT_FOR_THIRD_BYTE_LENGTH;
                    break;
                case WAIT_FOR_THIRD_BYTE_LENGTH:
                    length += buffer.readUnsignedByte() << 8;
                    state = State.WAIT_FOR_FOURTH_BYTE_LENGTH;
                    break;
                case WAIT_FOR_FOURTH_BYTE_LENGTH:
                    length += buffer.readUnsignedByte();
                    if (length == 0) {
                        ctx.writeAndFlush(ACK.duplicate());
                        state = State.WAIT_FOR_FIRST_BYTE_LENGTH;
                    } else {
                        state = State.READING;
                    }
                    break;
                case READING:
                    int remaining = buffer.readableBytes();
                    if (length > remaining) {
                        length -= remaining;
                        buffer.skipBytes(remaining);
                    } else {
                        buffer.skipBytes(length);
                        ctx.writeAndFlush(ACK.duplicate());
                        state = State.WAIT_FOR_FIRST_BYTE_LENGTH;
                    }
                    break;
                }
            }
        }
    }
}
