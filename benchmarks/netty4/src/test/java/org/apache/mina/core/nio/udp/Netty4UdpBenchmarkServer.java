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
import io.netty.util.Attribute;

import java.io.IOException;

import org.apache.mina.core.Netty4BenchmarkServer;

/**
 * A Netty 4 based UDP server
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Netty4UdpBenchmarkServer extends Netty4BenchmarkServer {

    private Bootstrap bootstrap;

    /**
     * {@inheritDoc}
     */
    public void start(int port) throws IOException {
        bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup());
        bootstrap.channel(NioDatagramChannel.class);
        bootstrap.option(ChannelOption.SO_RCVBUF, 65536);
        bootstrap.handler(new ChannelInitializer<DatagramChannel>() {

            @Override
            protected void initChannel(DatagramChannel ch) throws Exception {
                ch.pipeline().addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        ctx.attr(STATE_ATTRIBUTE).set(State.WAIT_FOR_FIRST_BYTE_LENGTH);
                    }

                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket message) throws Exception {
                        ByteBuf buffer = message.content();
                        State state = ctx.attr(STATE_ATTRIBUTE).get();
                        int length = 0;
                        Attribute<Integer> lengthAttribute = ctx.attr(LENGTH_ATTRIBUTE);

                        if (lengthAttribute.get() != null) {
                            length = lengthAttribute.get();
                        }

                        while (buffer.readableBytes() > 0) {
                            switch (state) {
                                case WAIT_FOR_FIRST_BYTE_LENGTH:
                                    length = (buffer.readByte() & 255) << 24;
                                    state = State.WAIT_FOR_SECOND_BYTE_LENGTH;
                                    break;

                                case WAIT_FOR_SECOND_BYTE_LENGTH:
                                    length += (buffer.readByte() & 255) << 16;
                                    state = State.WAIT_FOR_THIRD_BYTE_LENGTH;
                                    break;

                                case WAIT_FOR_THIRD_BYTE_LENGTH:
                                    length += (buffer.readByte() & 255) << 8;
                                    state = State.WAIT_FOR_FOURTH_BYTE_LENGTH;
                                    break;

                                case WAIT_FOR_FOURTH_BYTE_LENGTH:
                                    length += (buffer.readByte() & 255);
                                    state = State.READING;

                                    if ((length == 0) && (buffer.readableBytes() == 0)) {
                                        ctx.writeAndFlush(new DatagramPacket(ACK.retain(1).resetReaderIndex(), message.sender()));
                                        state = State.WAIT_FOR_FIRST_BYTE_LENGTH;
                                    }
                                    break;

                                case READING:
                                    int remaining = buffer.readableBytes();

                                    if (length > remaining) {
                                        length -= remaining;
                                        buffer.skipBytes(remaining);
                                    } else {
                                        buffer.skipBytes(length);
                                        ctx.writeAndFlush(new DatagramPacket(ACK.retain(1).resetReaderIndex(), message.sender()));
                                        state = State.WAIT_FOR_FIRST_BYTE_LENGTH;
                                        length = 0;
                                    }
                            }
                        }

                        ctx.attr(STATE_ATTRIBUTE).set(state);
                        ctx.attr(LENGTH_ATTRIBUTE).set(length);
                   }
                });
            }
        });
        bootstrap.bind(port);
    }

    /**
     * {@inheritedDoc}
     */
    public void stop() throws IOException {
        bootstrap.group().shutdownGracefully();
    }
}
