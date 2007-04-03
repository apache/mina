package org.apache.mina.transport.socket.nio;

import java.net.InetSocketAddress;

import org.apache.mina.common.IoSession;

public interface SocketSession extends IoSession {
    SocketSessionConfig getConfig();
    InetSocketAddress getRemoteAddress();
    InetSocketAddress getLocalAddress();
    InetSocketAddress getServiceAddress();
}
