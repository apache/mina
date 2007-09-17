package org.apache.mina.transport.socket;

import java.net.InetSocketAddress;

import org.apache.mina.common.BroadcastIoSession;

public interface DatagramSession extends BroadcastIoSession {
    DatagramSessionConfig getConfig();

    InetSocketAddress getRemoteAddress();

    InetSocketAddress getLocalAddress();

    InetSocketAddress getServiceAddress();
}
