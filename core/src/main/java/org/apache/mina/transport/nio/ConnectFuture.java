package org.apache.mina.transport.nio;

import org.apache.mina.api.IoSession;
import org.apache.mina.util.AbstractIoFuture;

public class ConnectFuture extends AbstractIoFuture<IoSession> {

    @Override
    protected boolean cancelOwner(boolean mayInterruptIfRunning) {
        return false;
    }

    /**
     * session connected
     */
    public void complete(IoSession session) {
        setResult(session);
    }

    /**
     * connection error
     */
    public void error(Exception e) {
        setException(e);
    }
}