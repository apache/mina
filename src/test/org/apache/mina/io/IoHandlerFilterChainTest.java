package org.apache.mina.io;

import java.net.SocketAddress;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.SessionConfig;
import org.apache.mina.common.TransportType;

import junit.framework.TestCase;

// I'll use easymock to test AbstractIoHandlerFilterChain
public class IoHandlerFilterChainTest extends TestCase {

    private static class TestSession implements IoSession
    {
        private final IoHandlerFilterChain chain;
        private final IoHandler handler;
        
        private TestSession( IoHandlerFilterChain chain, IoHandler handler )
        {
            this.chain = chain;
            this.handler = handler;
        }
        
        public IoHandler getHandler()
        {
            return handler;
        }

        public void close() {
        }

        public void write(ByteBuffer buf, Object marker) {
            chain.filterWrite( null, this, buf, marker );
        }

        public Object getAttachment() {
            return null;
        }

        public void setAttachment(Object attachment) {
        }

        public TransportType getTransportType() {
            return null;
        }

        public boolean isConnected() {
            return false;
        }

        public SessionConfig getConfig() {
            return null;
        }

        public SocketAddress getRemoteAddress() {
            return null;
        }

        public SocketAddress getLocalAddress() {
            return null;
        }

        public long getReadBytes() {
            return 0;
        }

        public long getWrittenBytes() {
            return 0;
        }

        public long getLastIoTime() {
            return 0;
        }

        public long getLastReadTime() {
            return 0;
        }

        public long getLastWriteTime() {
            return 0;
        }

        public boolean isIdle(IdleStatus status) {
            return false;
        }
    }

    private static class IoHandlerFilterChainImpl extends AbstractIoHandlerFilterChain
    {
        protected void doWrite(IoSession session, ByteBuffer buffer, Object marker)
        {
            session.getHandler().dataWritten( session, marker );
        }
    }
}
