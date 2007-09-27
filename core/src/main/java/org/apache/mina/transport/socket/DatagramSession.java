package org.apache.mina.transport.socket;

import java.net.InetSocketAddress;

import org.apache.mina.common.IoSession;

public interface DatagramSession extends IoSession {
    DatagramSessionConfig getConfig();

    InetSocketAddress getRemoteAddress();

    InetSocketAddress getLocalAddress();

    InetSocketAddress getServiceAddress();
}
