package org.apache.mina.util;

import org.apache.mina.common.*;

import java.net.SocketAddress;

/**
 * dummy {@link IoSession} implementation for use in test cases.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 567228 $, $Date: 2007-08-18 06:39:46 +0200 (za, 18 aug 2007) $
 */
public class DummySession  extends AbstractIoSession {

    @Override
    protected void updateTrafficMask() {
    }

    public IoSessionConfig getConfig() {
        return null;
    }

    public IoFilterChain getFilterChain() {
        return null;
    }

    public IoHandler getHandler() {
        return null;
    }

    public SocketAddress getLocalAddress() {
        return null;
    }

    public SocketAddress getRemoteAddress() {
        return null;
    }

    public int getScheduledWriteBytes() {
        return 0;
    }

    public int getScheduledWriteMessages() {
        return 0;
    }

    public IoService getService() {
        return null;
    }

    public IoServiceMetadata getTransportType() {
        return null;
    }
}


