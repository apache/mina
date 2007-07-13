package org.apache.mina.transport.vmpipe;

import org.apache.mina.common.IoSession;

public interface VmPipeSession extends IoSession {
    VmPipeSessionConfig getConfig();

    VmPipeAddress getRemoteAddress();

    VmPipeAddress getLocalAddress();

    VmPipeAddress getServiceAddress();
}
