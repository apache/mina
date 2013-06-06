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
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.BenchmarkServer;
import org.apache.mina.core.CounterFilter;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ChildChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.WriteCompletionEvent;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

/**
 * A Netty 3 TCP Server.
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Netty3TcpBenchmarkServer implements BenchmarkServer {

    private static enum State {
        WAIT_FOR_FIRST_BYTE_LENGTH, WAIT_FOR_SECOND_BYTE_LENGTH, WAIT_FOR_THIRD_BYTE_LENGTH, WAIT_FOR_FOURTH_BYTE_LENGTH, READING
    }

    private static final ChannelBuffer ACK = ChannelBuffers.buffer(1);

    static {
        ACK.writeByte(0);
    }

    private static final String STATE_ATTRIBUTE = Netty3TcpBenchmarkServer.class.getName() + ".state";

    private static final String LENGTH_ATTRIBUTE = Netty3TcpBenchmarkServer.class.getName() + ".length";

    private ChannelFactory factory;

    private ChannelGroup allChannels = new DefaultChannelGroup();

    /**
     * Allocate a map as attachment for storing attributes.
     * 
     * @param ctx the channel context
     * @return the map from the attachment
     */
    protected static Map<String, Object> getAttributesMap(ChannelHandlerContext ctx) {
        Map<String, Object> map = (Map<String, Object>) ctx.getAttachment();
        if (map == null) {
            map = new HashMap<String, Object>();
            ctx.setAttachment(map);
        }
        return map;
    }

    private static void setAttribute(ChannelHandlerContext ctx, String name, Object value) {
        getAttributesMap(ctx).put(name, value);
    }

    private static Object getAttribute(ChannelHandlerContext ctx, String name) {
        return getAttributesMap(ctx).get(name);
    }

    /**
     * {@inheritDoc}
     */
    public void start(int port) throws IOException {
        factory = new NioServerSocketChannelFactory();
        ServerBootstrap bootstrap = new ServerBootstrap(factory);
        bootstrap.setOption("receiveBufferSize", 128 * 1024);
        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(new SimpleChannelUpstreamHandler() {
                    @Override
                    public void childChannelOpen(ChannelHandlerContext ctx, ChildChannelStateEvent e) throws Exception {
                        System.out.println("childChannelOpen");
                        setAttribute(ctx, STATE_ATTRIBUTE, State.WAIT_FOR_FIRST_BYTE_LENGTH);
                    }

                    @Override
                    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
                        System.out.println("channelOpen");
                        setAttribute(ctx, STATE_ATTRIBUTE, State.WAIT_FOR_FIRST_BYTE_LENGTH);
                        allChannels.add(ctx.getChannel());
                    }

                    public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e) throws Exception {
                        CounterFilter.messageSent.getAndIncrement();
                    }

                    @Override
                    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
                        if (e.getMessage() instanceof ChannelBuffer) {
                            ChannelBuffer buffer = (ChannelBuffer) e.getMessage();

                            State state = (State) getAttribute(ctx, STATE_ATTRIBUTE);
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
    }

    /**
     * {@inheritedDoc}
     */
    public void stop() throws IOException {
        allChannels.disconnect().awaitUninterruptibly();
        factory.releaseExternalResources();
    }
}
