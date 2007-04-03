package org.apache.mina.transport.vmpipe;

import org.apache.mina.common.IoSession;

public interface VmPipeSession extends IoSession {
    VmPipeSessionConfig getConfig();
    /* FIXME: These methods can return AnonymousSocketAddress.
     *        We need to create a port number manager like O/S do for port
     *        numbers.
    VmPipeAddress getRemoteAddress();
    VmPipeAddress getLocalAddress();
    VmPipeAddress getServiceAddress();
    */
}
