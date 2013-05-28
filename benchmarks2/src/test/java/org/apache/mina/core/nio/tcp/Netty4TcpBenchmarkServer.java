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

import io.netty.bootstrap.ChannelFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.AttributeKey;

import java.io.IOException;

import org.apache.mina.core.BenchmarkServer;

/**
 * A Netty 3 TCP Server.
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Netty4TcpBenchmarkServer implements BenchmarkServer {

    private static enum State {
        WAIT_FOR_FIRST_BYTE_LENGTH, WAIT_FOR_SECOND_BYTE_LENGTH, WAIT_FOR_THIRD_BYTE_LENGTH, WAIT_FOR_FOURTH_BYTE_LENGTH, READING
    }

    private static final ByteBuf ACK = UnpooledByteBufAllocator.DEFAULT.buffer(1);

    static {
        ACK.writeByte(0);
    }

    private static final String STATE_ATTRIBUTE = Netty4TcpBenchmarkServer.class.getName() + ".state";

    private static final String LENGTH_ATTRIBUTE = Netty4TcpBenchmarkServer.class.getName() + ".length";

    private ChannelFactory factory;

    private ChannelGroup allChannels = new DefaultChannelGroup();

    private static void setAttribute(ChannelHandlerContext ctx, String name, State value) {
        AttributeKey<State> attribute = new AttributeKey<State>(name);
        ctx.attr(attribute).set(value);
    }

    private static State getAttribute(ChannelHandlerContext ctx, String name) {
        AttributeKey<State> attribute = new AttributeKey<State>(name);
        return ctx.attr(attribute).get();
    }

    /**
     * {@inheritDoc}
     */
    public void start(int port) throws IOException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.option(ChannelOption.SO_RCVBUF, 128 * 1024);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.group(new NioEventLoopGroup(2));
        bootstrap.channel(NioServerSocketChannel.class);
        /*
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(new SimpleChannelUpstreamHandler() {
                    @Override
                    public void connect(ChannelHandlerContext ctx, ChildChannelStateEvent e) throws Exception {
                        System.out.println("childChannelOpen");
                        setAttribute(ctx, STATE_ATTRIBUTE, State.WAIT_FOR_FIRST_BYTE_LENGTH);
                    }

                    @Override
                    public void channelOpen(ChannelHandlerContext ctx, ) throws Exception {
                        System.out.println("channelOpen");
                        setAttribute(ctx, STATE_ATTRIBUTE, State.WAIT_FOR_FIRST_BYTE_LENGTH);
                        allChannels.add(ctx.getChannel());
                    }

                    @Override
                    public void inboundBufferUpdated(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
                        if (e.getMessage() instanceof ByteBuf) {
                            ByteBuf buffer = (ByteBuf) e.getMessage();

                            State state = getAttribute(ctx, STATE_ATTRIBUTE);
                            int length = 0;
                            if (getAttributesMap(ctx).containsKey(LENGTH_ATTRIBUTE)) {
                                length = (Integer) getAttribute(ctx, LENGTH_ATTRIBUTE);
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
                                        ctx.getChannel().write(ACK.slice());
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
                                        ctx.getChannel().write(ACK.slice());
                                        state = State.WAIT_FOR_FIRST_BYTE_LENGTH;
                                        length = 0;
                                    }
                                }
                            }
                            setAttribute(ctx, STATE_ATTRIBUTE, state);
                            setAttribute(ctx, LENGTH_ATTRIBUTE, length);
                        }
                    }

                    @Override
                    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
                        allChannels.remove(ctx.getChannel());
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
                        e.getCause().printStackTrace();
                    }
                });
            }
        });
        allChannels.add(bootstrap.bind(new InetSocketAddress(port)));
        */
    }

    /**
     * {@inheritedDoc}
     */
    public void stop() throws IOException {
        allChannels.disconnect().awaitUninterruptibly();
        factory.releaseExternalResources();
    }
}
