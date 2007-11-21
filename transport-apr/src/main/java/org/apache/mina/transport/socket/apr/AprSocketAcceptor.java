package org.apache.mina.transport.socket.apr;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;

import org.apache.mina.common.AbstractPollingIoAcceptor;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoProcessor;
import org.apache.mina.common.RuntimeIoException;
import org.apache.mina.common.TransportMetadata;
import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.util.CircularQueue;
import org.apache.tomcat.jni.Address;
import org.apache.tomcat.jni.Poll;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.Socket;
import org.apache.tomcat.jni.Status;

public class AprSocketAcceptor extends AbstractPollingIoAcceptor<AprSession, Long> implements SocketAcceptor {

    private static final int POLLSET_SIZE = 1024;

    private final Object wakeupLock = new Object();
    private long wakeupSocket;
    private volatile boolean toBeWakenUp;

    private int backlog = 50;
    private boolean reuseAddress = true;

    private volatile long pool;
    private volatile long pollset; // socket poller
    private volatile boolean selectable = true;
    private final long[] polledSockets = new long[POLLSET_SIZE << 1];
    private final List<Long> polledHandles =
        new CircularQueue<Long>(POLLSET_SIZE);
    
    public AprSocketAcceptor(int processorCount) {
        super(new DefaultSocketSessionConfig(), AprIoProcessor.class, processorCount);
    }

    public AprSocketAcceptor() {
        super(new DefaultSocketSessionConfig(), AprIoProcessor.class);
    }

    public AprSocketAcceptor(Executor executor,
            IoProcessor<AprSession> processor) {
        super(new DefaultSocketSessionConfig(), executor, processor);
    }

    public AprSocketAcceptor(IoProcessor<AprSession> processor) {
        super(new DefaultSocketSessionConfig(), processor);
    }

    @Override
    protected AprSession accept(IoProcessor<AprSession> processor, Long handle) {
        try {
            long s = Socket.accept(handle);
            try {
                return new AprSocketSession(this, processor, s);
            } catch (Exception e) {
                Socket.close(s);
                ExceptionMonitor.getInstance().exceptionCaught(e);
            }
        } catch (Exception e) {
            ExceptionMonitor.getInstance().exceptionCaught(e);
        }
        
        return null;
    }

    @Override
    protected Long bind(SocketAddress localAddress) throws Exception {
        InetSocketAddress la = (InetSocketAddress) localAddress;
        long handle = Socket.create(
                Socket.APR_INET, Socket.SOCK_STREAM, Socket.APR_PROTO_TCP, pool);

        boolean success = false;
        try {
            Socket.optSet(handle, Socket.APR_SO_NONBLOCK, 1);
            Socket.timeoutSet(handle, 0);
            
            // Configure the server socket,
            Socket.optSet(handle, Socket.APR_SO_REUSEADDR, isReuseAddress()? 1 : 0);
            Socket.optSet(handle, Socket.APR_SO_RCVBUF, getSessionConfig().getReceiveBufferSize());

            // and bind.
            long sa;
            if (la != null) {
                if (la.getAddress() == null) {
                    sa = Address.info(Address.APR_ANYADDR, Socket.APR_INET, la.getPort(), 0, pool);
                } else {
                    sa = Address.info(la.getAddress().getHostAddress(), Socket.APR_UNSPEC, la.getPort(), Socket.APR_IPV4_ADDR_OK, pool);
                }
            } else {
                sa = Address.info(Address.APR_ANYADDR, Socket.APR_INET, 0, 0, pool);
            }
            
            int result = Socket.bind(handle, sa);
            if (result != Status.APR_SUCCESS) {
                throwException(result);
            }
            result = Socket.listen(handle, getBacklog());
            if (result != Status.APR_SUCCESS) {
                throwException(result);
            }
            
            result = Poll.add(pollset, handle, Poll.APR_POLLIN);
            if (result != Status.APR_SUCCESS) {
                throwException(result);
            }
            success = true;
        } finally {
            if (!success) {
                unbind(handle);
            }
        }
        return handle;
    }

    @Override
    protected void doDispose0() {
        selectable = false;
        Poll.destroy(pollset);
        Pool.destroy(pool);
        Socket.close(wakeupSocket);
    }

    @Override
    protected void doInit() {
        try {
            wakeupSocket = Socket.create(
                    Socket.APR_INET, Socket.SOCK_DGRAM, Socket.APR_PROTO_UDP, AprLibrary
                    .getInstance().getRootPool());
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeIoException("Failed to create a wakeup socket.", e);
        }

        // initialize a memory pool for APR functions
        pool = Pool.create(AprLibrary.getInstance().getRootPool());
        
        boolean success = false;
        try {
            pollset = Poll.create(
                            POLLSET_SIZE,
                            pool,
                            Poll.APR_POLLSET_THREADSAFE,
                            Long.MAX_VALUE);
            
            if (pollset == 0) {
                pollset = Poll.create(
                        62,
                        pool,
                        Poll.APR_POLLSET_THREADSAFE,
                        Long.MAX_VALUE);
            }

            if (pollset < 0) {
                if (Status.APR_STATUS_IS_ENOTIMPL(- (int) pollset)) {
                    throw new RuntimeIoException(
                            "Thread-safe pollset is not supported in this platform.");
                }
            }
            success = true;
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeIoException("Failed to create a pollset.", e);
        } finally {
            if (!success) {
                dispose();
            }
        }
    }

    @Override
    protected SocketAddress localAddress(Long handle) throws Exception {
        long la = Address.get(Socket.APR_LOCAL, handle);
        return new InetSocketAddress(Address.getip(la), Address.getInfo(la).port);
    }

    @Override
    protected boolean select() throws Exception {
        if (!selectable()) {
            throw new ClosedSelectorException();
        }

        int rv = Poll.poll(pollset, Integer.MAX_VALUE, polledSockets, false);
        if (rv <= 0) {
            if (rv != -120001) {
                throwException(rv);
            }
            
            rv = Poll.maintain(pollset, polledSockets, true);
            if (rv > 0) {
                for (int i = 0; i < rv; i ++) {
                    Poll.add(pollset, polledSockets[i], Poll.APR_POLLIN);
                }
            } else if (rv < 0) {
                throwException(rv);
            }
            
            return false;
        } else {
            rv <<= 1;
            if (!polledHandles.isEmpty()) {
                polledHandles.clear();
            }

            for (int i = 0; i < rv; i ++) {
                long flag = polledSockets[i];
                long socket = polledSockets[++i];
                if (socket == wakeupSocket) {
                    synchronized (wakeupLock) {
                        Poll.remove(pollset, wakeupSocket);
                        toBeWakenUp = false;
                    }
                    continue;
                }
                
                if ((flag & Poll.APR_POLLIN) != 0) {
                    Poll.add(pollset, socket, Poll.APR_POLLIN);
                    polledHandles.add(socket);
                }
            }
            return !polledHandles.isEmpty();
        }
    }

    @Override
    protected boolean selectable() {
        return selectable;
    }

    @Override
    protected Iterator<Long> selectedHandles() {
        return polledHandles.iterator();
    }

    @Override
    protected void unbind(Long handle) throws Exception {
        Poll.remove(pollset, handle);
        int result = Socket.close(handle);
        if (result != Status.APR_SUCCESS) {
            throwException(result);
        }
    }

    @Override
    protected void wakeup() {
        if (toBeWakenUp) {
            return;
        }
        
        // Add a dummy socket to the pollset.
        synchronized (wakeupLock) {
            toBeWakenUp = true;
            Poll.add(pollset, wakeupSocket, Poll.APR_POLLOUT);
        }
    }

    public int getBacklog() {
        return backlog;
    }

    public boolean isReuseAddress() {
        return reuseAddress;
    }

    public void setBacklog(int backlog) {
        synchronized (bindLock) {
            if (isActive()) {
                throw new IllegalStateException(
                        "backlog can't be set while the acceptor is bound.");
            }

            this.backlog = backlog;
        }
    }

    public void setLocalAddress(InetSocketAddress localAddress) {
        super.setLocalAddress(localAddress);
    }

    public void setReuseAddress(boolean reuseAddress) {
        synchronized (bindLock) {
            if (isActive()) {
                throw new IllegalStateException(
                        "backlog can't be set while the acceptor is bound.");
            }

            this.reuseAddress = reuseAddress;
        }
    }

    public TransportMetadata getTransportMetadata() {
        return AprSocketSession.METADATA;
    }

    @Override
    public SocketSessionConfig getSessionConfig() {
        return (SocketSessionConfig) super.getSessionConfig();
    }
    
    @Override
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) super.getLocalAddress();
    }

    private void throwException(int code) throws IOException {
        throw new IOException(
                org.apache.tomcat.jni.Error.strerror(-code) +
                " (code: " + code + ")");
    }
}
